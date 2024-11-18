package access.api;

import access.config.Config;
import access.exception.NotFoundException;
import access.exception.UserRestrictionException;
import access.logging.AccessLogger;
import access.logging.Event;
import access.manage.Manage;
import access.model.*;
import access.provision.ProvisioningService;
import access.provision.scim.GroupURN;
import access.repository.ApplicationRepository;
import access.repository.ApplicationUsageRepository;
import access.repository.RoleRepository;
import access.security.UserPermissions;
import access.validation.URLFormatValidator;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.Getter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

import static access.SwaggerOpenIdConfig.API_TOKENS_SCHEME_NAME;
import static access.SwaggerOpenIdConfig.OPEN_ID_SCHEME_NAME;

@RestController
@RequestMapping(value = {
        "/api/v1/roles",
        "/api/external/v1/roles"},
        produces = MediaType.APPLICATION_JSON_VALUE)
@Transactional
@SecurityRequirement(name = OPEN_ID_SCHEME_NAME, scopes = {"openid"})
@SecurityRequirement(name = API_TOKENS_SCHEME_NAME)
@EnableConfigurationProperties(Config.class)
public class RoleController implements ApplicationResource {
    private static final Log LOG = LogFactory.getLog(RoleController.class);

    private final Config config;
    private final RoleRepository roleRepository;
    @Getter
    private final ApplicationRepository applicationRepository;
    @Getter
    private final ApplicationUsageRepository applicationUsageRepository;
    private final Manage manage;
    private final ProvisioningService provisioningService;
    private final URLFormatValidator urlFormatValidator = new URLFormatValidator();
    private final boolean limitInstitutionAdminRoleVisibility;
    private final RoleOperations roleOperations;

    public RoleController(Config config,
                          RoleRepository roleRepository,
                          ApplicationRepository applicationRepository,
                          ApplicationUsageRepository applicationUsageRepository,
                          Manage manage,
                          ProvisioningService provisioningService,
                          @Value("${feature.limit-institution-admin-role-visibility}") boolean limitInstitutionAdminRoleVisibility) {
        this.config = config;
        this.roleRepository = roleRepository;
        this.applicationRepository = applicationRepository;
        this.applicationUsageRepository = applicationUsageRepository;
        this.manage = manage;
        this.provisioningService = provisioningService;
        this.limitInstitutionAdminRoleVisibility = limitInstitutionAdminRoleVisibility;
        this.roleOperations = new RoleOperations(this);
    }

    @GetMapping("")
    public ResponseEntity<List<Role>> rolesByApplication(@Parameter(hidden = true) User user) {
        LOG.debug(String.format("/roles for user %s", user.getEduPersonPrincipalName()));

        if (user.isSuperUser()) {
            return ResponseEntity.ok(manage.addManageMetaData(roleRepository.findAll()));
        }
        UserPermissions.assertAuthority(user, Authority.INSTITUTION_ADMIN);

        if (limitInstitutionAdminRoleVisibility) {
            List<Role> roles = roleRepository.findByOrganizationGUID(user.getOrganizationGUID());
            return ResponseEntity.ok(manage.addManageMetaData(roles));
        }

        Set<String> manageIdentifiers = new HashSet<>();
        if (user.isInstitutionAdmin()) {
            Set<String> applicationManageIdentifiers = user.getApplications().stream().map(m -> (String) m.get("id")).collect(Collectors.toSet());
            manageIdentifiers.addAll(applicationManageIdentifiers);
        }

        Set<String> roleManageIdentifiers = user.getUserRoles().stream()
                //If the user has an userRole as Inviter, then we must exclude those
                .filter(userRole -> userRole.getAuthority().hasEqualOrHigherRights(Authority.MANAGER))
                .map(userRole -> userRole.getRole().applicationsUsed())
                .flatMap(Collection::stream)
                .map(Application::getManageId)
                .collect(Collectors.toSet());
        manageIdentifiers.addAll(roleManageIdentifiers);

        List<Role> roles = new ArrayList<>();
        manageIdentifiers.forEach(manageId -> roles.addAll(roleRepository.findByApplicationUsagesApplicationManageId(manageId)));
        return ResponseEntity.ok(manage.addManageMetaData(roles));
    }

    @GetMapping("{id}")
    public ResponseEntity<Role> role(@PathVariable("id") Long id,@Parameter(hidden = true) User user) {
        LOG.debug(String.format("/role/%s for user %s", id, user.getEduPersonPrincipalName()));
        Role role = roleRepository.findById(id).orElseThrow(() -> new NotFoundException("Role not found"));
        UserPermissions.assertRoleAccess(user, role, Authority.INVITER);
        manage.addManageMetaData(List.of(role));
        return ResponseEntity.ok(role);
    }

    @GetMapping("/application/{manageId}")
    public ResponseEntity<List<Role>> rolesPerApplicationId(@PathVariable("manageId") String manageId, @Parameter(hidden = true) User user) {
        LOG.debug(String.format("/rolesPerApplicationId for user %s", user.getEduPersonPrincipalName()));
        UserPermissions.assertAuthority(user, Authority.INSTITUTION_ADMIN);

        if (!user.isSuperUser()) {
            Set<String> applicationManageIdentifiers = user.getApplications().stream().map(m -> (String) m.get("id")).collect(Collectors.toSet());
            Set<String> roleManageIdentifiers = user.getUserRoles().stream()
                    //If the user has an userRole as Inviter, then we must exclude those
                    .filter(userRole -> userRole.getAuthority().hasEqualOrHigherRights(Authority.MANAGER))
                    .map(userRole -> userRole.getRole().applicationsUsed())
                    .flatMap(Collection::stream)
                    .map(Application::getManageId)
                    .collect(Collectors.toSet());
            applicationManageIdentifiers.addAll(roleManageIdentifiers);
            if (!applicationManageIdentifiers.contains(manageId)) {
                throw new UserRestrictionException();
            }
        }
        List<Role> roles = roleRepository.findByApplicationUsagesApplicationManageId(manageId);
        return ResponseEntity.ok(manage.addManageMetaData(roles));
    }


    @GetMapping("search")
    public ResponseEntity<List<Role>> search(@RequestParam(value = "query") String query,
                                             @Parameter(hidden = true) User user) {
        LOG.debug("/search");
        UserPermissions.assertSuperUser(user);
        List<Role> roles = roleRepository.search(query + "*", 15);
        return ResponseEntity.ok(manage.addManageMetaData(roles));
    }

    @PostMapping("")
    public ResponseEntity<Role> newRole(@Validated @RequestBody Role role,
                                        @Parameter(hidden = true) User user) {
        UserPermissions.assertAuthority(user, Authority.INSTITUTION_ADMIN);
        //For super_users this is NULL, which is ok (unless they are impersonating an institution_admin)
        role.setOrganizationGUID(user.getOrganizationGUID());

        role.setShortName(GroupURN.sanitizeRoleShortName(role.getShortName()));
        role.setIdentifier(UUID.randomUUID().toString());

        LOG.debug(String.format("New role '%s' by user %s", role.getName(), user.getName()));

        return saveOrUpdate(role, user);
    }

    @PutMapping("")
    public ResponseEntity<Role> updateRole(@Validated @RequestBody Role role,
                                           @Parameter(hidden = true) User user) {
        UserPermissions.assertAuthority(user, Authority.MANAGER);
        LOG.debug(String.format("Update role '%s' by user %s", role.getName(), user.getEduPersonPrincipalName()));
        return saveOrUpdate(role, user);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRole(@PathVariable("id") Long id,
                                           @Parameter(hidden = true) User user) {
        Role role = roleRepository.findById(id).orElseThrow(() -> new NotFoundException("Role not found"));

        LOG.debug(String.format("Delete role %s by user %s", role.getName(), user.getEduPersonPrincipalName()));

        manage.addManageMetaData(List.of(role));
        UserPermissions.assertAuthority(user, Authority.INSTITUTION_ADMIN);

        if (limitInstitutionAdminRoleVisibility && !user.isSuperUser() &&
                !Objects.equals(user.getOrganizationGUID(), role.getOrganizationGUID())) {
            throw new UserRestrictionException();
        }

        provisioningService.deleteGroupRequest(role);
        roleRepository.delete(role);
        AccessLogger.role(LOG, Event.Deleted, user, role);
        return Results.deleteResult();
    }

    private ResponseEntity<Role> saveOrUpdate(Role role, User user) {
        roleOperations.assertValidRole(role);

        manage.addManageMetaData(List.of(role));

        boolean isNew = role.getId() == null;
        List<String> previousApplicationIdentifiers = new ArrayList<>();
        Optional<UserRole> optionalUserRole = user.userRoleForRole(role);
        boolean immutableApplicationUsages = !user.isSuperUser() && 
                optionalUserRole.isPresent() && optionalUserRole.get().getAuthority().equals(Authority.MANAGER);
        boolean nameChanged = false;
        if (!isNew) {
            Role previousRole = roleRepository.findById(role.getId()).orElseThrow(() -> new NotFoundException("Role not found"));
            //We don't allow shortName, identifier or organizationGUID changes after creation
            role.setShortName(previousRole.getShortName());
            role.setIdentifier(previousRole.getIdentifier());
            if (user.isSuperUser()) {
                role.setOrganizationGUID(role.getOrganizationGUID());
            } else {
                role.setOrganizationGUID(previousRole.getOrganizationGUID());
            }
            previousApplicationIdentifiers.addAll(previousRole.applicationIdentifiers());
            if (immutableApplicationUsages) {
                role.setApplicationUsages(previousRole.getApplicationUsages());
            }
            nameChanged = !previousRole.getName().equals(role.getName());
        }
        if (!immutableApplicationUsages) {
            roleOperations.syncRoleApplicationUsages(role);
        }

        Role saved = roleRepository.save(role);
        if (isNew) {
            provisioningService.newGroupRequest(saved);
        } else {
            provisioningService.updateGroupRequest(previousApplicationIdentifiers, saved, nameChanged);
        }
        AccessLogger.role(LOG, isNew ? Event.Created : Event.Updated, user, role);

        return ResponseEntity.ok(saved);
    }


}
