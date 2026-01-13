package invite.internal;

import invite.api.*;
import invite.exception.NotFoundException;
import invite.logging.AccessLogger;
import invite.logging.Event;
import invite.mail.MailBox;
import invite.manage.Manage;
import invite.model.*;
import invite.provision.ProvisioningService;
import invite.provision.scim.GroupURN;
import invite.repository.*;
import invite.security.RemoteUser;
import invite.security.RemoteUserPermissions;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.Getter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.*;

import static invite.SwaggerOpenIdConfig.BASIC_AUTHENTICATION_SCHEME_NAME;

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
    private final String groupUrnPrefix;


    public InternalInviteController(RoleRepository roleRepository,
                                    UserRoleRepository userRoleRepository,
                                    ApplicationRepository applicationRepository,
                                    ApplicationUsageRepository applicationUsageRepository,
                                    MailBox mailBox,
                                    Manage manage,
                                    InvitationRepository invitationRepository,
                                    ProvisioningService provisioningService,
                                    @Value("${voot.group_urn_domain}") String groupUrnPrefix) {
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.applicationRepository = applicationRepository;
        this.applicationUsageRepository = applicationUsageRepository;
        this.mailBox = mailBox;
        this.manage = manage;
        this.invitationRepository = invitationRepository;
        this.provisioningService = provisioningService;
        this.groupUrnPrefix = groupUrnPrefix;
        this.userRoleOperations = new UserRoleOperations(this);
        this.roleOperations = new RoleOperations(this);
        this.invitationOperations = new InvitationOperations(this);
    }

    @GetMapping("/roles")
    @PreAuthorize("hasAnyRole('SP_DASHBOARD','ACCESS')")
    @Transactional(readOnly = true)
    @Hidden
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

    @GetMapping("/roles/{organizationGUID}/{manageId}")
    @PreAuthorize("hasRole('ACCESS')")
    @Transactional(readOnly = true)
    public ResponseEntity<List<Role>> rolesPerOrganizationApplicationId(@PathVariable("organizationGUID") String organizationGUID,
                                                                        @PathVariable("manageId") String manageId,
                                                                        @Parameter(hidden = true) @AuthenticationPrincipal RemoteUser remoteUser) {
        LOG.debug(String.format("/rolesPerApplicationId for remoteUser %s", remoteUser.getName()));

        List<Role> roles = roleRepository
                .findByOrganizationGUIDAndApplicationUsagesApplicationManageId(organizationGUID, manageId);
        return ResponseEntity.ok(roles);
    }

    @GetMapping("/roles/{id}")
    @PreAuthorize("hasAnyRole('SP_DASHBOARD','ACCESS')")
    @Hidden
    public ResponseEntity<Role> role(@PathVariable("id") Long id,
                                     @Parameter(hidden = true) @AuthenticationPrincipal RemoteUser remoteUser) {
        LOG.debug(String.format("/role/%s for user %s", id, remoteUser.getName()));

        Role role = roleRepository.findById(id).orElseThrow(() -> new NotFoundException("Role not found"));

        RemoteUserPermissions.assertApplicationAccess(remoteUser, role);

        manage.addManageMetaData(List.of(role));
        return ResponseEntity.ok(role);
    }

    @PostMapping("/roles")
    @PreAuthorize("hasAnyRole('SP_DASHBOARD','ACCESS')")
    @Operation(summary = "Create a Role",
            description = "Create a Role linked to a SP in Manage. Note that the required application object needs to be pre-configured during deployment.",
            security = { @SecurityRequirement(name = BASIC_AUTHENTICATION_SCHEME_NAME) },
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    useParameterTypeSchema = true,
                    content = {@Content(examples = {@ExampleObject(value = """
                            {
                              "name": "Required role name",
                              "description": "Required role description",
                              "defaultExpiryDays": 365,
                              "inviterDisplayName": "Free format field used in the invitation emails for this role (can be email address)"
                              "applicationUsages": [
                                {
                                  "landingPage": "https://landingpage.com",
                                  "application": {
                                    "manageId": "4",
                                    "manageType": "SAML20_SP"
                                  }
                                }
                              ]
                            }
                            """
                    )})}
            ),
            responses = {
                    @ApiResponse(responseCode = "201", description = "Created",
                            content = {@Content(schema = @Schema(implementation = Role.class),
                                    examples = {@ExampleObject(value = """
                                            {
                                              "id": 42114,
                                              "name": "Required role name",
                                              "shortName": "required_role_name",
                                              "description": "Required role description",
                                              "urn": "urn:mace:surf.nl:test.surfaccess.nl:74fd8059-7558-4454-8393-fd84f74c4907:required_role_name",
                                              "defaultExpiryDays": 365,
                                              "enforceEmailEquality": false,
                                              "eduIDOnly": false,
                                              "blockExpiryDate": false,
                                              "overrideSettingsAllowed": false,
                                              "teamsOrigin": false,
                                              "identifier": "74fd8059-7558-4454-8393-fd84f74c4907",
                                              "remoteApiUser": "SP Dashboard",
                                              "applicationUsages": [
                                                {
                                                  "id": 49203,
                                                  "landingPage": "http://landingpage.com",
                                                  "application": {
                                                    "id": 41904,
                                                    "manageId": "4",
                                                    "manageType": "SAML20_SP"
                                                  }
                                                }
                                              ],
                                              "auditable": {
                                                "createdAt": 1729254283,
                                                "createdBy": "sp_dashboard"
                                              },
                                              "applicationMaps": [
                                                {
                                                  "OrganizationName:en": "SURF bv",
                                                  "landingPage": "http://landingpage.com",
                                                  "logo": "https://static.surfconext.nl/media/idp/surfconext.png",
                                                  "entityid": "https://research",
                                                  "name:en": "Research EN",
                                                  "id": "4",
                                                  "_id": "4",
                                                  "type": "saml20_sp",
                                                  "url": "https://default-url-research.org",
                                                  "name:nl": "Research NL"
                                                }
                                              ]
                                            }                                            
                                            """
                                    )})}),
                    @ApiResponse(responseCode = "400", description = "BadRequest",
                            content = {@Content(schema = @Schema(implementation = StatusResponse.class),
                                    examples = {@ExampleObject(value = """
                                            {
                                              "timestamp": 1717672263253,
                                              "status": 400,
                                              "error": "BadRequest",
                                              "exception": "access.exception.UserRestrictionException",
                                              "message": "No access to application",
                                              "path": "/api/internal/invite/invitations"
                                            }
                                            """
                                    )})})})
    public ResponseEntity<Role> newRole(@Validated @RequestBody RoleRequest roleRequest,
                                        @Parameter(hidden = true) @AuthenticationPrincipal RemoteUser remoteUser) {
        Role role = new Role(roleRequest);
        role.setRemoteApiUser(remoteUser.getName());

        role.setShortName(GroupURN.sanitizeRoleShortName(role.getName()));
        role.setIdentifier(UUID.randomUUID().toString());

        LOG.debug(String.format("New role '%s' by user %s", role.getName(), remoteUser.getName()));

        role.setUrn(GroupURN.urnFromRole(groupUrnPrefix, role));

        Role savedRole = saveOrUpdate(role, remoteUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedRole);
    }

    @PutMapping("/roles")
    @PreAuthorize("hasAnyRole('SP_DASHBOARD','ACCESS')")
    @Hidden
    public ResponseEntity<Role> updateRole(@Validated @RequestBody Role role,
                                           @Parameter(hidden = true) @AuthenticationPrincipal RemoteUser remoteUser) {
        LOG.debug(String.format("Update role '%s' by user %s", role.getName(), remoteUser.getName()));

        return ResponseEntity.status(HttpStatus.CREATED).body(saveOrUpdate(role, remoteUser));
    }

    @DeleteMapping("/roles/{id}")
    @PreAuthorize("hasAnyRole('SP_DASHBOARD','ACCESS')")
    @Operation(summary = "Delete existing Role",
            description = "Delete an existing role. The path parameter id is the id returned when creating the role.",
            parameters = {@Parameter(name = "id", in = ParameterIn.PATH, description = "Unique database id of the role", required = true)},
            responses = {
                    @ApiResponse(responseCode = "204", description = "No content"),
                    @ApiResponse(responseCode = "400", description = "BadRequest",
                            content = {@Content(schema = @Schema(implementation = StatusResponse.class),
                                    examples = {@ExampleObject(value = """
                                            {
                                              "timestamp": 1717672263253,
                                              "status": 400,
                                              "error": "BadRequest",
                                              "exception": "access.exception.UserRestrictionException",
                                              "message": "No access to role",
                                              "path": "/api/internal/roles/999"
                                            }
                                            """
                                    )})}),
                    @ApiResponse(responseCode = "404", description = "Role not found",
                            content = {@Content(schema = @Schema(implementation = StatusResponse.class),
                                    examples = {@ExampleObject(value = """
                                            {
                                              "timestamp": 1717672263253,
                                              "status": 404,
                                              "error": "Not found",
                                              "exception": "access.exception.NotFoundException",
                                              "message": "Role not found",
                                              "path": "/api/internal/roles/999"
                                            }
                                            """
                                    )})})})

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
    @PreAuthorize("hasAnyRole('SP_DASHBOARD','ACCESS')")
    @Operation(summary = "Invite member for existing Role",
            description = "Invite a member for an existing role. An invitation email will be sent. Do not forget to set guestRoleIncluded to true." +
                    "At least one email must be either present in invites or invitations. When using the invitations you can also specify the " +
                    "internalPlaceholderIdentifier, which will be used as the id in the SCIM POST to /User.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = {@Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "Invitation example",
                                            summary = "Example invitation request",
                                            value = """
                                                    {
                                                      "intendedAuthority": "INVITER",
                                                      "message": "Personal message included in the email",
                                                      "language": "en",
                                                      "guestRoleIncluded": true,
                                                      "invites": [
                                                        "admin@service.org"
                                                      ],
                                                      "invitations": [{
                                                        "email": "admin2@service.org",
                                                        "internalPlaceholderIdentifier": "4EFF937F-EE78-4A54-9FD8-A214FD64D7E1",
                                                      }],
                                                      "roleIdentifiers": [
                                                        99
                                                      ],
                                                      "roleExpiryDate": 1760788376,
                                                      "expiryDate": 1730461976
                                                    }
                                                    """
                                    )})}
            ),
            responses = {
                    @ApiResponse(responseCode = "201", description = "Created",
                            content = {@Content(schema = @Schema(implementation = InvitationResponse.class),
                                    examples = {@ExampleObject(value = """
                                            {
                                              "status": 201,
                                              "recipientInvitationURLs": [
                                                {
                                                  "recipient": "admin@service.nl",
                                                  "invitationURL": "https://invite.test.surfconext.nl/invitation/accept?{hash}"
                                                }
                                              ]
                                            }
                                            """
                                    )})}),
                    @ApiResponse(responseCode = "400", description = "BadRequest",
                            content = {@Content(schema = @Schema(implementation = StatusResponse.class),
                                    examples = {@ExampleObject(value = """
                                            {
                                              "timestamp": 1717672263253,
                                              "status": 400,
                                              "error": "BadRequest",
                                              "exception": "access.exception.UserRestrictionException",
                                              "message": "No access to application",
                                              "path": "/api/internal/invite/invitations"
                                            }
                                            """
                                    )})}),
                    @ApiResponse(responseCode = "404", description = "Role not found",
                            content = {@Content(schema = @Schema(implementation = StatusResponse.class),
                                    examples = {@ExampleObject(value = """
                                            {
                                              "timestamp": 1717672263253,
                                              "status": 404,
                                              "error": "Not found",
                                              "exception": "access.exception.NotFoundException",
                                              "message": "Role not found",
                                              "path": "/api/internal/invite/invitations"
                                            }
                                            """
                                    )})})})
    public ResponseEntity<InvitationResponse> newInvitation(@Validated @RequestBody InvitationRequest invitationRequest,
                                                            @Parameter(hidden = true) @AuthenticationPrincipal RemoteUser remoteUser) {
        return this.invitationOperations.sendInvitation(invitationRequest, null, remoteUser);
    }

    @PutMapping("/invitations/{id}")
    @PreAuthorize("hasAnyRole('SP_DASHBOARD','ACCESS')")
    @Hidden
    public ResponseEntity<Map<String, Integer>> resendInvitation(@PathVariable("id") Long id,
                                                                 @Parameter(hidden = true) @AuthenticationPrincipal RemoteUser remoteUser) {
        return this.invitationOperations.resendInvitation(id, null, remoteUser);
    }

    @GetMapping("user_roles/{roleId}")
    @PreAuthorize("hasAnyRole('SP_DASHBOARD','ACCESS')")
    @Transactional
    @Hidden
    public ResponseEntity<List<UserRole>> byRole(@PathVariable("roleId") Long roleId,
                                                 @Parameter(hidden = true) @AuthenticationPrincipal RemoteUser remoteUser) {
        return this.userRoleOperations.userRolesByRole(roleId,
                role -> RemoteUserPermissions.assertApplicationAccess(remoteUser, role));
    }

    private Role saveOrUpdate(Role role, RemoteUser remoteUser) {
        roleOperations.assertValidRole(role);
        roleOperations.setDefaultsValidRole(role);
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

        return saved;
    }

}
