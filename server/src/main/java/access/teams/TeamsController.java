package access.teams;

import access.exception.NotFoundException;
import access.exception.RemoteException;
import access.manage.Manage;
import access.model.Role;
import access.model.*;
import access.provision.ProvisioningService;
import access.provision.scim.GroupURN;
import access.provision.scim.OperationType;
import access.repository.RoleRepository;
import access.repository.UserRepository;
import access.repository.UserRoleRepository;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.rmi.Remote;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static access.SwaggerOpenIdConfig.ATTRIBUTE_AGGREGATION_SCHEME_NAME;

@RestController
@RequestMapping(value = {"/api/teams", "/api/external/v1/teams"}, produces = MediaType.APPLICATION_JSON_VALUE)
@SecurityRequirement(name = ATTRIBUTE_AGGREGATION_SCHEME_NAME)
public class TeamsController {

    private static final int DEFAULT_EXPIRY_DAYS = 5 * 365;

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final Manage manage;
    private final ProvisioningService provisioningService;

    public TeamsController(RoleRepository roleRepository,
                           UserRepository userRepository,
                           UserRoleRepository userRoleRepository,
                           Manage manage,
                           ProvisioningService provisioningService) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.manage = manage;
        this.provisioningService = provisioningService;
    }

    @PostMapping("")
    @PreAuthorize("hasRole('TEAMS')")
    @Transactional
    public ResponseEntity<Void> migrateTeam(@RequestBody Team team) {
        Role role = new Role();
        role.setName(team.getName());
        role.setShortName(GroupURN.sanitizeRoleShortName(role.getName()));
        role.setDescription(team.getDescription());
        role.setUrn(team.getUrn());
        role.setLandingPage(team.getLandingPage());
        role.setDefaultExpiryDays(DEFAULT_EXPIRY_DAYS);
        role.setIdentifier(UUID.randomUUID().toString());
        role.setTeamsOrigin(true);
        //Check if the applications exist in Manage
        Set<Application> applications = team.getApplications().stream().filter(this::applicationExists).collect(Collectors.toSet());
        if (applications.isEmpty()) {
            throw new NotFoundException();
        }
        role.setApplications(applications);
        Role savedRole = roleRepository.save(role);

        provisioningService.newGroupRequest(savedRole);

        List<Membership> memberships = team.getMemberships();
        memberships.forEach(membership -> this.provision(savedRole, membership));

        return ResponseEntity.status(201).build();
    }

    private boolean applicationExists(Application application) {
        try {
            manage.providerById(application.getManageType(), application.getManageId());
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private void provision(Role role, Membership membership) {
        Person person = membership.getPerson();
        Optional<User> optionalUser = userRepository.findBySubIgnoreCase(person.getUrn());
        User user = optionalUser.orElseGet(() -> {
            User newUser = new User();
            newUser.setSub(person.getUrn());
            newUser.setName(person.getName());
            newUser.setEmail(person.getEmail());
            newUser.setSchacHomeOrganization(person.getSchacHomeOrganization());
            return userRepository.save(newUser);
        });
        UserRole userRole = new UserRole();
        userRole.setInviter("teams_migration");
        userRole.setUser(user);
        userRole.setRole(role);
        Instant now = Instant.now();
        userRole.setCreatedAt(now);
        userRole.setEndDate(now.plus(DEFAULT_EXPIRY_DAYS, ChronoUnit.DAYS));
        userRole.setAuthority(membership.getRole().equals(access.teams.Role.MEMBER) ? Authority.GUEST : Authority.INVITER);
        userRole = userRoleRepository.save(userRole);

        provisioningService.updateGroupRequest(userRole, OperationType.Add);
    }
}
