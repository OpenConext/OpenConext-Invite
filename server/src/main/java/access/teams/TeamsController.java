package access.teams;

import access.api.Results;
import access.exception.InvalidInputException;
import access.manage.Manage;
import access.model.Role;
import access.model.*;
import access.provision.ProvisioningService;
import access.provision.scim.GroupURN;
import access.provision.scim.OperationType;
import access.repository.ApplicationRepository;
import access.repository.RoleRepository;
import access.repository.UserRepository;
import access.repository.UserRoleRepository;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static access.SwaggerOpenIdConfig.BASIC_AUTHENTICATION_SCHEME_NAME;

@RestController
@RequestMapping(value = {"/api/teams", "/api/external/v1/teams"}, produces = MediaType.APPLICATION_JSON_VALUE)
@SecurityRequirement(name = BASIC_AUTHENTICATION_SCHEME_NAME)
public class TeamsController {

    private static final int DEFAULT_EXPIRY_DAYS = 5 * 365;

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final ApplicationRepository applicationRepository;
    private final Manage manage;
    private final ProvisioningService provisioningService;

    public TeamsController(RoleRepository roleRepository,
                           UserRepository userRepository,
                           UserRoleRepository userRoleRepository,
                           ApplicationRepository applicationRepository,
                           Manage manage,
                           ProvisioningService provisioningService) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.applicationRepository = applicationRepository;
        this.manage = manage;
        this.provisioningService = provisioningService;
    }

    @PutMapping("")
    @PreAuthorize("hasRole('TEAMS')")
    @Transactional
    public ResponseEntity<Map<String, Integer>> migrateTeam(@RequestBody Team team) {
        if (CollectionUtils.isEmpty(team.getApplications())) {
            throw new InvalidInputException("Applications are required");
        }
        List<Membership> memberships = team.getMemberships();
        if (!CollectionUtils.isEmpty(memberships)) {
            memberships.forEach(membership -> {
                if (membership.getPerson() == null) {
                    throw new InvalidInputException("Person of a membership is required");
                }
                if (membership.getPerson().getSchacHomeOrganization() == null) {
                    throw new InvalidInputException("SchacHomeOrganization of a person is required");
                }
            });
        }
        //Check if the applications exist in Manage
        Set<Application> applications = team.getApplications().stream()
                .filter(this::applicationExists)
                .collect(Collectors.toSet());
        if (applications.isEmpty()) {
            throw new InvalidInputException("None of the applications exists in Manage");
        }
        Role role = new Role();
        role.setName(team.getName());
        role.setShortName(GroupURN.sanitizeRoleShortName(role.getName()));
        role.setDescription(team.getDescription());
        role.setUrn(team.getUrn());
        role.setDefaultExpiryDays(DEFAULT_EXPIRY_DAYS);
        role.setIdentifier(UUID.randomUUID().toString());
        role.setTeamsOrigin(true);
        //This is the disadvantage of having to save references from Manage
        Set<ApplicationUsage> applicationUsages = team.getApplications().stream()
                .map(applicationFromTeams -> {
                    Application applicationFromDB = applicationRepository
                            .findByManageIdAndManageType(applicationFromTeams.getManageId(), applicationFromTeams.getManageType())
                            .orElseGet(() -> applicationRepository.save(applicationFromTeams));
                    return new ApplicationUsage(applicationFromDB, applicationFromTeams.getLandingPage());
                })
                .collect(Collectors.toSet());
        role.setApplicationUsages(applicationUsages);
        Role savedRole = roleRepository.save(role);

        provisioningService.newGroupRequest(savedRole);

        memberships.forEach(membership -> this.provision(savedRole, membership));

        return Results.createResult();
    }

    private boolean applicationExists(Application application) {
        try {
            Map<String, Object> provider = manage.providerById(application.getManageType(), application.getManageId());
            return provider != null;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private void provision(Role role, Membership membership) {
        Person person = membership.getPerson();
        Optional<User> optionalUser = userRepository.findBySubIgnoreCase(person.getUrn());
        Instant now = Instant.now();
        User user = optionalUser.orElseGet(() -> {
            User newUser = new User();
            newUser.setSub(person.getUrn());
            newUser.setName(person.getName());
            newUser.setEmail(person.getEmail());
            newUser.setSchacHomeOrganization(person.getSchacHomeOrganization());
            newUser.setCreatedAt(now);
            newUser.setLastActivity(now);
            newUser.nameInvariant();
            return userRepository.save(newUser);
        });
        if (user.getCreatedAt() == null) {
            user.setCreatedAt(now);
        }
        if (user.getLastActivity() == null) {
            user.setLastActivity(now);
        }
        UserRole userRole = new UserRole();
        userRole.setInviter("teams_migration");
        userRole.setUser(user);
        userRole.setRole(role);
        userRole.setCreatedAt(now);
        userRole.setEndDate(now.plus(DEFAULT_EXPIRY_DAYS, ChronoUnit.DAYS));
        access.teams.Role teamsRole = membership.getRole();
        userRole.setAuthority(mapAuthority(teamsRole));
        boolean guestRoleIncluded = teamsRole.equals(access.teams.Role.ADMIN) || teamsRole.equals(access.teams.Role.MANAGER);
        userRole.setGuestRoleIncluded(guestRoleIncluded);
        userRole = userRoleRepository.save(userRole);

        provisioningService.updateGroupRequest(userRole, OperationType.Add);
    }

    protected static Authority mapAuthority(access.teams.Role role) {
        switch (role) {
            case MEMBER -> {
                return Authority.GUEST;
            }
            case MANAGER -> {
                return Authority.INVITER;
            }
            case ADMIN, OWNER -> {
                return Authority.MANAGER;
            }
            default -> {
                throw new InvalidInputException("Unknown membership role: " + role);
            }
        }
    }
}
