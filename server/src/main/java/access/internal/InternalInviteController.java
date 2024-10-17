package access.internal;

import access.api.AppRepositoryResource;
import access.api.Results;
import access.api.RoleOperations;
import access.exception.NotFoundException;
import access.logging.AccessLogger;
import access.logging.Event;
import access.manage.Manage;
import access.model.Role;
import access.provision.ProvisioningService;
import access.provision.scim.GroupURN;
import access.repository.ApplicationRepository;
import access.repository.ApplicationUsageRepository;
import access.repository.RoleRepository;
import access.security.RemoteUser;
import access.security.RemoteUserPermissions;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.Getter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static access.SwaggerOpenIdConfig.BASIC_AUTHENTICATION_SCHEME_NAME;

@RestController
@RequestMapping(value = {"/api/internal/invite", "/api/external/v1/internal/invite"},
        produces = MediaType.APPLICATION_JSON_VALUE)
@SecurityRequirement(name = BASIC_AUTHENTICATION_SCHEME_NAME)
public class InternalInviteController implements AppRepositoryResource {
    private static final Log LOG = LogFactory.getLog(InternalInviteController.class);

    private final RoleRepository roleRepository;
    @Getter
    private final ApplicationRepository applicationRepository;
    @Getter
    private final ApplicationUsageRepository applicationUsageRepository;
    private final Manage manage;
    private final ProvisioningService provisioningService;
    private final RoleOperations roleOperations;

    public InternalInviteController(RoleRepository roleRepository,
                                    ApplicationRepository applicationRepository,
                                    ApplicationUsageRepository applicationUsageRepository,
                                    Manage manage,
                                    ProvisioningService provisioningService) {
        this.roleRepository = roleRepository;
        this.applicationRepository = applicationRepository;
        this.applicationUsageRepository = applicationUsageRepository;
        this.manage = manage;
        this.provisioningService = provisioningService;
        roleOperations = new RoleOperations(this);
    }

    @GetMapping("/role/{id}")
    @PreAuthorize("hasRole('SP_DASHBOARD')")
    public ResponseEntity<Role> role(@PathVariable("id") Long id,
                                     @Parameter(hidden = true) @AuthenticationPrincipal RemoteUser remoteUser) {
        LOG.debug(String.format("/role/%s for user %s", id, remoteUser.getName()));

        Role role = roleRepository.findById(id).orElseThrow(() -> new NotFoundException("Role not found"));

        RemoteUserPermissions.assertApplicationAccess(remoteUser, role);

        manage.addManageMetaData(List.of(role));
        return ResponseEntity.ok(role);
    }

    @PostMapping("/role")
    @PreAuthorize("hasRole('SP_DASHBOARD')")
    public ResponseEntity<Role> newRole(@Validated @RequestBody Role role,
                                        @Parameter(hidden = true) @AuthenticationPrincipal RemoteUser remoteUser) {
        role.setRemoteApiUser(remoteUser.getName());

        role.setShortName(GroupURN.sanitizeRoleShortName(role.getShortName()));
        role.setIdentifier(UUID.randomUUID().toString());

        LOG.debug(String.format("New role '%s' by user %s", role.getName(), remoteUser.getName()));

        return saveOrUpdate(role, remoteUser);
    }

    @PutMapping("/role")
    @PreAuthorize("hasRole('SP_DASHBOARD')")
    public ResponseEntity<Role> updateRole(@Validated @RequestBody Role role,
                                           @Parameter(hidden = true) @AuthenticationPrincipal RemoteUser remoteUser) {
        LOG.debug(String.format("Update role '%s' by user %s", role.getName(), remoteUser.getName()));

        return saveOrUpdate(role, remoteUser);
    }

    @DeleteMapping("/role/{id}")
    @PreAuthorize("hasRole('SP_DASHBOARD')")
    public ResponseEntity<Void> deleteRole(@PathVariable("id") Long id,
                                           @Parameter(hidden = true) @AuthenticationPrincipal RemoteUser remoteUser) {
        Role role = roleRepository.findById(id).orElseThrow(() -> new NotFoundException("Role not found"));

        LOG.debug(String.format("Delete role %s by user %s", role.getName(), remoteUser.getName()));

        manage.addManageMetaData(List.of(role));
        RemoteUserPermissions.assertApplicationAccess(remoteUser, role);

        provisioningService.deleteGroupRequest(role);
        roleRepository.delete(role);

        AccessLogger.role(LOG, Event.Deleted, remoteUser, role);

        return Results.deleteResult();
    }

    private ResponseEntity<Role> saveOrUpdate(Role role, RemoteUser remoteUser) {
        roleOperations.assertValidRole(role);
        RemoteUserPermissions.assertApplicationAccess(remoteUser, role);

        manage.addManageMetaData(List.of(role));
        boolean isNew = role.getId() == null;
        List<String> previousApplicationIdentifiers = new ArrayList<>();
        boolean nameChanged = false;
        if (!isNew) {
            Role previousRole = roleRepository.findById(role.getId()).orElseThrow(() -> new NotFoundException("Role not found"));
            //We don't allow shortName, identifier or organizationGUID changes after creation
            role.setShortName(previousRole.getShortName());
            role.setIdentifier(previousRole.getIdentifier());
            role.setOrganizationGUID(previousRole.getOrganizationGUID());
            previousApplicationIdentifiers.addAll(previousRole.applicationIdentifiers());
            nameChanged = !previousRole.getName().equals(role.getName());
        }
        roleOperations.syncRoleApplicationUsages(role);

        Role saved = roleRepository.save(role);
        if (isNew) {
            provisioningService.newGroupRequest(saved);
        } else {
            provisioningService.updateGroupRequest(previousApplicationIdentifiers, saved, nameChanged);
        }

        AccessLogger.role(LOG, isNew ? Event.Created : Event.Updated, remoteUser, role);

        return ResponseEntity.ok(saved);
    }

}
