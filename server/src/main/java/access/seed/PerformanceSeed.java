package access.seed;

import access.api.APITokenController;
import access.manage.EntityType;
import access.manage.Manage;
import access.manage.ManageIdentifier;
import access.model.*;
import access.repository.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
public class PerformanceSeed {

    private static final Log LOG = LogFactory.getLog(APITokenController.class);

    private static final Random random = new Random();

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ApplicationRepository applicationRepository;
    private final UserRoleRepository userRoleRepository;
    private final InvitationRepository invitationRepository;
    private final Manage manage;
    private final JdbcTemplate jdbcTemplate;

    public PerformanceSeed(UserRepository userRepository,
                           RoleRepository roleRepository,
                           ApplicationRepository applicationRepository,
                           UserRoleRepository userRoleRepository,
                           InvitationRepository invitationRepository,
                           Manage manage,
                           DataSource dataSource) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.applicationRepository = applicationRepository;
        this.userRoleRepository = userRoleRepository;
        this.invitationRepository = invitationRepository;
        this.manage = manage;
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public Map<String, Object> go(int numberOfRoles, int numberOfUsers) {
        List<Map<String, Object>> providers = manage.providers(EntityType.SAML20_SP, EntityType.OIDC10_RP);
        List<Map<String, Object>> identityProviders = manage.providers(EntityType.SAML20_IDP);
        List<String> institutionGuids = identityProviders.stream()
                .map(m -> (String) m.get("institutionGuid"))
                .filter(s -> s != null)
                .toList();
        IntStream.range(1, numberOfRoles + 1)
                .forEach(i -> {
                    String institutionGuid = institutionGuids.get(random.nextInt(institutionGuids.size()));
                    Role role = createRole(providers, institutionGuid);
                    roleRepository.save(role);
                    if (i % 100 == 0) {
                        LOG.debug(String.format("Created %s from %s roles", i, numberOfRoles));
                    }
                });

        long minRoleId = this.minId("roles");
        long maxRoleId = this.maxId("roles");

        IntStream.range(1, numberOfUsers + 1)
                .forEach(i -> {
                    String institutionGuid = institutionGuids.get(random.nextInt(institutionGuids.size()));
                    User user = userRepository.save(createUser(institutionGuid));
                    long roleId = random.nextLong(minRoleId, maxRoleId + 1L);
                    Role role = roleRepository.findById(roleId).get();
                    if (!user.isInstitutionAdmin()) {
                        userRoleRepository.save(this.createUserRole(user, role));
                    }
                    invitationRepository.save(this.createInvitation(user, role));
                    if (i % 1000 == 0) {
                        LOG.debug(String.format("Created %s from %s users", i, numberOfUsers));
                    }
                });

        return Map.of("users", numberOfUsers,
                "roles", numberOfRoles,
                "userRoles", numberOfUsers);
    }

    private User createUser(String institutionGuid) {
        String uuid = UUID.randomUUID().toString();
        String givenName = NameGenerator.generate();
        //For testing pagination, it's a Doe
        String familyName = "Doe"; //NameGenerator.generate();
        String schacHome = NameGenerator.generate().toLowerCase() + ".org";
        String eppn = String.format("%s.%s@%s", givenName.toLowerCase(), familyName.toLowerCase(), schacHome);
        User user = new User(false,
                eppn,
                uuid,
                schacHome,
                givenName,
                familyName,
                eppn);
        int i = random.nextInt(15);
        //We also need institution-admin
        if (i == 9) {
            user.setInstitutionAdmin(true);
            user.setOrganizationGUID(institutionGuid);
        }
        return user;
    }

    private UserRole createUserRole(User user, Role role) {
        Authority authority = getAuthority();
        return new UserRole("performance-seed", user, role, authority);
    }

    private static Authority getAuthority() {
        double randomDouble = Math.random();
        return randomDouble > 0.8 ? Authority.MANAGER : randomDouble < 0.2 ? Authority.INVITER : Authority.GUEST;
    }

    private Invitation createInvitation(User inviter, Role role) {
        Set<InvitationRole> roles = Set.of(new InvitationRole(role));
        return new Invitation(
                getAuthority(),
                UUID.randomUUID().toString(),
                String.format("%s@example.com", NameGenerator.generate()),
                false,
                false,
                false,
                "Auto generated",
                Language.en,
                inviter,
                Instant.now().plus(30, ChronoUnit.DAYS),
                Instant.now().plus(365 * 5, ChronoUnit.DAYS),
                roles);
    }

    private Role createRole(List<Map<String, Object>> providers, String institutionGuid) {
        String name = NameGenerator.generate();

        int i = random.nextInt(1,15);
        int numberOfApplications = i > 13 ? 1 : i;
        Set<ManageIdentifier> manageIdentifiers = IntStream.range(0, numberOfApplications)
                .mapToObj(j -> {
                    Map<String, Object> provider = providers.get(random.nextInt(providers.size()));
                    return new ManageIdentifier((String) provider.get("id"),
                            EntityType.valueOf(((String) provider.get("type")).toUpperCase()));
                })
                //This will remove duplicates
                .collect(Collectors.toSet());

        Set<ApplicationUsage> applicationUsages = manageIdentifiers.stream()
                .map(manageIdentifier ->
                        new ApplicationUsage(
                                this.application(manageIdentifier),
                                "http://landingpage.com"
                        )).collect(Collectors.toSet());
        Role role = new Role(name,
                name,
                applicationUsages,
                365 * 5,
                false,
                false);
        role.setOrganizationGUID(institutionGuid);
        return role;
    }

    private Application application(ManageIdentifier manageIdentifier) {
        String manageId = manageIdentifier.manageId();
        EntityType entityType = manageIdentifier.manageType();
        return applicationRepository.findByManageIdAndManageType(manageId, entityType).
                orElseGet(() -> applicationRepository.save(new Application(manageId, entityType)));
    }

    private Long minId(String tableName) {
        return jdbcTemplate.query(String.format("SELECT min(id) as minId FROM %s ", tableName), rs -> {
            rs.next();
            return rs.getLong("minId");
        });
    }

    private Long maxId(String tableName) {
        return jdbcTemplate.query(String.format("SELECT max(id) as maxId FROM %s ", tableName), rs -> {
            rs.next();
            return rs.getLong("maxId");
        });
    }
}