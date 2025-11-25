package invite.api;

import invite.config.Config;
import invite.exception.NotFoundException;
import invite.exception.UserRestrictionException;
import invite.logging.AccessLogger;
import invite.logging.Event;
import invite.manage.EntityType;
import invite.manage.Manage;
import invite.model.*;
import invite.provision.ProvisioningService;
import invite.provision.scim.GroupURN;
import invite.repository.ApplicationRepository;
import invite.repository.ApplicationUsageRepository;
import invite.repository.RoleRepository;
import invite.security.InstitutionAdmin;
import invite.security.UserPermissions;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.Getter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLTransactionRollbackException;
import java.util.*;
import java.util.stream.Collectors;

import static invite.SwaggerOpenIdConfig.API_TOKENS_SCHEME_NAME;
import static invite.SwaggerOpenIdConfig.OPEN_ID_SCHEME_NAME;

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

    private final RoleRepository roleRepository;
    @Getter
    private final ApplicationRepository applicationRepository;
    @Getter
    private final ApplicationUsageRepository applicationUsageRepository;
    private final Manage manage;
    private final ProvisioningService provisioningService;
    private final RoleOperations roleOperations;
    private final String groupUrnPrefix;

    public RoleController(RoleRepository roleRepository,
                          ApplicationRepository applicationRepository,
                          ApplicationUsageRepository applicationUsageRepository,
                          Manage manage,
                          ProvisioningService provisioningService,
                          @Value("${voot.group_urn_domain}") String groupUrnPrefix) {
        this.roleRepository = roleRepository;
        this.applicationRepository = applicationRepository;
        this.applicationUsageRepository = applicationUsageRepository;
        this.manage = manage;
        this.provisioningService = provisioningService;
        this.roleOperations = new RoleOperations(this);
        this.groupUrnPrefix = groupUrnPrefix;
    }

    @GetMapping("")
    @Transactional(readOnly = true)
    public ResponseEntity<Page<Role>> rolesByApplication(@Parameter(hidden = true) User user,
                                                         @RequestParam(value = "force", required = false, defaultValue = "true") boolean force,
                                                         @RequestParam(value = "query", required = false, defaultValue = "") String query,
                                                         @RequestParam(value = "pageNumber", required = false, defaultValue = "0") int pageNumber,
                                                         @RequestParam(value = "pageSize", required = false, defaultValue = "10") int pageSize,
                                                         @RequestParam(value = "sort", required = false, defaultValue = "name") String sort,
                                                         @RequestParam(value = "sortDirection", required = false, defaultValue = "ASC") String sortDirection) {
        LOG.debug(String.format("/roles for user %s", user.getEduPersonPrincipalName()));

        Page<Role> rolesPage;
        if (user.isSuperUser()) {
            if (force) {
                Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE);
                rolesPage = roleRepository.searchByPage(pageable);
            } else {
                Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.fromString(sortDirection), sort));
                rolesPage = StringUtils.hasText(query) ?
                        roleRepository.searchByPageWithKeyword(FullSearchQueryParser.parse(query), pageable) :
                        roleRepository.searchByPage(pageable);
            }
        } else {
            UserPermissions.assertAuthority(user, Authority.INSTITUTION_ADMIN);
            Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE);
            rolesPage = roleRepository.searchByPageAndOrganizationGUID(user.getOrganizationGUID(), pageable);

        }
        List<Long> roleIdentifiers = rolesPage.getContent().stream().map(role -> role.getId()).toList();
        List<Map<String, Object>> applications = roleRepository.findApplications(roleIdentifiers);
        List<Role> roles = manage.addManageMetaData(this.roleFromQuery(rolesPage, applications));
        return Pagination.of(rolesPage, roles);
    }

    @GetMapping("{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<Role> role(@PathVariable("id") Long id, @Parameter(hidden = true) User user) {
        LOG.debug(String.format("/role/%s for user %s", id, user.getEduPersonPrincipalName()));

        Role role = roleRepository.findById(id).orElseThrow(() -> new NotFoundException("Role not found"));
        UserPermissions.assertRoleAccess(user, role, Authority.INVITER);
        manage.addManageMetaData(List.of(role));
        return ResponseEntity.ok(role);
    }

    @GetMapping("/application/{manageId}")
    @Transactional(readOnly = true)
    public ResponseEntity<List<Role>> rolesPerApplicationId(@PathVariable("manageId") String manageId, @Parameter(hidden = true) User user) {
        LOG.debug(String.format("/rolesPerApplicationId for user %s", user.getEduPersonPrincipalName()));

        UserPermissions.assertAuthority(user, Authority.INSTITUTION_ADMIN);
        List<Role> roles;
        if (user.isSuperUser()) {
            roles = roleRepository.findByApplicationUsagesApplicationManageId(manageId);
        } else {
            Set<String> applicationManageIdentifiers = user.getApplications().stream().map(m -> (String) m.get("id")).collect(Collectors.toSet());
            Set<String> roleManageIdentifiers = user.getUserRoles().stream()
                    //If the user has a userRole as Inviter, then we must exclude those
                    .filter(userRole -> userRole.getAuthority().hasEqualOrHigherRights(Authority.MANAGER))
                    .map(userRole -> userRole.getRole().applicationsUsed())
                    .flatMap(Collection::stream)
                    .map(Application::getManageId)
                    .collect(Collectors.toSet());
            applicationManageIdentifiers.addAll(roleManageIdentifiers);
            if (!applicationManageIdentifiers.contains(manageId)) {
                throw new UserRestrictionException();
            }
            roles = roleRepository.findByOrganizationGUIDAndApplicationUsagesApplicationManageId(user.getOrganizationGUID(), manageId);
        }
        return ResponseEntity.ok(manage.addManageMetaData(roles));
    }


    @PostMapping("")
    public ResponseEntity<Role> newRole(@Validated @RequestBody RoleRequest roleRequest,
                                        @Parameter(hidden = true) User user) {
        LOG.debug(String.format("POST /roles/ for user %s", user.getEduPersonPrincipalName()));
        UserPermissions.assertAuthority(user, Authority.INSTITUTION_ADMIN);
        //For super_users we allow an organization GUID from the input form
        Role role = new Role(roleRequest);
        if (InstitutionAdmin.isInstitutionAdmin(user)) {
            role.setOrganizationGUID(user.getOrganizationGUID());
        } else if (user.isSuperUser()) {
            role.setOrganizationGUID(roleRequest.getOrganizationGUID());
        } else {
            role.setOrganizationGUID(null);
        }
        role.setShortName(GroupURN.sanitizeRoleShortName(roleRequest.getName()));
        role.setIdentifier(UUID.randomUUID().toString());
        role.setUrn(GroupURN.urnFromRole(this.groupUrnPrefix, role));

        LOG.debug(String.format("New role '%s' by user %s", role.getName(), user.getName()));

        return saveOrUpdate(role, user);
    }

    @PutMapping("")
    @Retryable(
            retryFor = {SQLTransactionRollbackException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000)
    )
    public ResponseEntity<Role> updateRole(@Validated @RequestBody Role role,
                                           @Parameter(hidden = true) User user) {
        LOG.debug(String.format("PUT /roles/ for user %s", user.getEduPersonPrincipalName()));
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

        if (!user.isSuperUser() &&
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

    //See RoleRepository#searchByPage
    private List<Role> roleFromQuery(Page<Role> rolesPage, List<Map<String, Object>> applications) {
        List<Role> roles = rolesPage.getContent();

        //Now add all applications, note that we need to preserve ordering of the roles
        Map<Long, List<Map<String, Object>>> applicationGroupedByRoleId =
                applications.stream().collect(Collectors.groupingBy(m -> (Long) m.get("role_id")));
        roles.forEach(role -> {
            //Find all applications with this role id
            List<Map<String, Object>> applicationsForRole = applicationGroupedByRoleId.get(role.getId());
            if (applicationsForRole != null) {
                Set<ApplicationUsage> applicationUsages = applicationsForRole.stream()
                        .map(m -> new ApplicationUsage(
                                new Application(
                                        (String) m.get("manage_id"),
                                        EntityType.valueOf((String) m.get("manage_type"))),
                                null))
                        .collect(Collectors.toSet());
                role.setApplicationUsages(applicationUsages);
            }
        });
        return roles;
    }

}
