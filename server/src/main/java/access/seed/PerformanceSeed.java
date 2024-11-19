package access.seed;

import access.manage.EntityType;
import access.manage.Manage;
import access.manage.ManageIdentifier;
import access.model.Application;
import access.model.ApplicationUsage;
import access.model.Role;
import access.model.User;
import access.repository.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
public class PerformanceSeed {

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

    public Map<String, Object> go() {
        List<Map<String, Object>> providers = manage.providers(EntityType.SAML20_SP, EntityType.OIDC10_RP);

//        int numberOfUsers = 500_000;
//        int numberOfRoles = 75_000;
        int numberOfUsers = 500;
        int numberOfRoles = 75;
        int[] numberOfUserRolesRange = new int[]{1, 5};
        int userRolesCreated = 0;

        IntStream.range(1, numberOfUsers)
                .forEach(i -> userRepository.save(createUser()));

        IntStream.range(1, numberOfRoles)
                .forEach(i -> {
                    Role role = createRole(providers);
                    try {
                        roleRepository.save(role);
                    } catch (Exception e) {
                        System.out.println(role);
                    }

                });

        return Map.of("users", numberOfUsers,
                "roles", numberOfRoles,
                "userRoles", userRolesCreated);
    }

    private User createUser() {
        String uuid = UUID.randomUUID().toString();
        String givenName = NameGenerator.generate();
        //For testing pagination, it's a Doe
        String familyName = "Doe"; //NameGenerator.generate();
        String schacHome = NameGenerator.generate() + ".org";
        String eppn = String.format("%s.%s@%s", givenName.toLowerCase(), familyName.toLowerCase(), schacHome.toLowerCase());
        return new User(false,
                eppn,
                uuid,
                schacHome,
                givenName,
                familyName,
                eppn);
    }

    private Role createRole(List<Map<String, Object>> providers) {
        String name = NameGenerator.generate();

        int i = random.nextInt(15);
        int numberOfApplications = i == 14 ? 1 : Math.min(i, 5);
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
        return new Role(name,
                name,
                applicationUsages,
                365 * 5,
                false,
                false);
    }

    public Application application(ManageIdentifier manageIdentifier) {
        String manageId = manageIdentifier.manageId();
        EntityType entityType = manageIdentifier.manageType();
        return applicationRepository.findByManageIdAndManageType(manageId, entityType).
                orElseGet(() -> applicationRepository.save(new Application(manageId, entityType)));
    }


    private Long randomId(String tableName) {
        return jdbcTemplate.query(String.format("SELECT id FROM %s ORDER BY RAND() LIMIT 1", tableName), rs -> {
            return rs.getLong("id");
        });
    }
}