package access.internal;

import access.api.*;
import access.exception.NotFoundException;
import access.logging.AccessLogger;
import access.logging.Event;
import access.mail.MailBox;
import access.manage.Manage;
import access.model.InvitationRequest;
import access.model.InvitationResponse;
import access.model.Role;
import access.model.UserRole;
import access.provision.ProvisioningService;
import access.provision.scim.GroupURN;
import access.repository.*;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static access.SwaggerOpenIdConfig.BASIC_AUTHENTICATION_SCHEME_NAME;

@RestController
@RequestMapping(value = {"/api/internal/invite", "/api/external/v1/internal/invite"},
        produces = MediaType.APPLICATION_JSON_VALUE)
@SecurityRequirement(name = BASIC_AUTHENTICATION_SCHEME_NAME)
public class InternalInviteController implements ApplicationResource, InvitationResource, UserRoleResource {
    private static final Log LOG = LogFactory.getLog(InternalInviteController.class);

    @Getter
    private final RoleRepository roleRepository;
    @Getter
    private final UserRoleRepository userRoleRepository;
    @Getter
    private final ApplicationRepository applicationRepository;
    @Getter
    private final ApplicationUsageRepository applicationUsageRepository;
    @Getter
    private final MailBox mailBox;
    @Getter
    private final Manage manage;
    @Getter
    private final InvitationRepository invitationRepository;

    private final ProvisioningService provisioningService;
    private final RoleOperations roleOperations;
    private final InvitationOperations invitationOperations;
    private final UserRoleOperations userRoleOperations;


    public InternalInviteController(RoleRepository roleRepository,
                                    UserRoleRepository userRoleRepository,
                                    ApplicationRepository applicationRepository,
                                    ApplicationUsageRepository applicationUsageRepository,
                                    MailBox mailBox,
                                    Manage manage,
                                    InvitationRepository invitationRepository,
                                    ProvisioningService provisioningService) {
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.applicationRepository = applicationRepository;
        this.applicationUsageRepository = applicationUsageRepository;
        this.mailBox = mailBox;
        this.manage = manage;
        this.invitationRepository = invitationRepository;
        this.provisioningService = provisioningService;
        this.userRoleOperations = new UserRoleOperations(this);
        this.roleOperations = new RoleOperations(this);
        this.invitationOperations = new InvitationOperations(this);
    }

    @GetMapping("/roles")
    @PreAuthorize("hasRole('SP_DASHBOARD')")
    public ResponseEntity<List<Role>> rolesByApplication(@Parameter(hidden = true) @AuthenticationPrincipal RemoteUser remoteUser) {
        LOG.debug(String.format("/roles for user %s", remoteUser.getName()));

        List<Role> roles = remoteUser.getApplications()
                .stream()
                .map(application -> roleRepository.findByApplicationUsagesApplicationManageId(application.getManageId()))
                .flatMap(Collection::stream)
                .toList();
        manage.addManageMetaData(roles);
        return ResponseEntity.ok(roles);
    }

    @GetMapping("/roles/{id}")
    @PreAuthorize("hasRole('SP_DASHBOARD')")
    public ResponseEntity<Role> role(@PathVariable("id") Long id,
                                     @Parameter(hidden = true) @AuthenticationPrincipal RemoteUser remoteUser) {
        LOG.debug(String.format("/role/%s for user %s", id, remoteUser.getName()));

        Role role = roleRepository.findById(id).orElseThrow(() -> new NotFoundException("Role not found"));

        RemoteUserPermissions.assertApplicationAccess(remoteUser, role);

        manage.addManageMetaData(List.of(role));
        return ResponseEntity.ok(role);
    }

    @PostMapping("/roles")
    @PreAuthorize("hasRole('SP_DASHBOARD')")
    public ResponseEntity<Role> newRole(@Validated @RequestBody Role role,
                                        @Parameter(hidden = true) @AuthenticationPrincipal RemoteUser remoteUser) {
        role.setRemoteApiUser(remoteUser.getName());

        role.setShortName(GroupURN.sanitizeRoleShortName(role.getShortName()));
        role.setIdentifier(UUID.randomUUID().toString());

        LOG.debug(String.format("New role '%s' by user %s", role.getName(), remoteUser.getName()));

        return saveOrUpdate(role, remoteUser);
    }

    @PutMapping("/roles")
    @PreAuthorize("hasRole('SP_DASHBOARD')")
    public ResponseEntity<Role> updateRole(@Validated @RequestBody Role role,
                                           @Parameter(hidden = true) @AuthenticationPrincipal RemoteUser remoteUser) {
        LOG.debug(String.format("Update role '%s' by user %s", role.getName(), remoteUser.getName()));

        return saveOrUpdate(role, remoteUser);
    }

    @DeleteMapping("/roles/{id}")
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

    @PostMapping("/invitations")
    @PreAuthorize("hasRole('SP_DASHBOARD')")
    public ResponseEntity<InvitationResponse> newInvitation(@Validated @RequestBody InvitationRequest invitationRequest,
                                                            @Parameter(hidden = true) @AuthenticationPrincipal RemoteUser remoteUser) {
        return this.invitationOperations.sendInvitation(invitationRequest, null, remoteUser);
    }

    @GetMapping("user_roles/{roleId}")
    @PreAuthorize("hasRole('SP_DASHBOARD')")
    @Transactional
    public ResponseEntity<List<UserRole>> byRole(@PathVariable("roleId") Long roleId,
                                                 @Parameter(hidden = true) @AuthenticationPrincipal RemoteUser remoteUser) {
        return this.userRoleOperations.userRolesByRole(roleId,
                role -> RemoteUserPermissions.assertApplicationAccess(remoteUser, role));
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
