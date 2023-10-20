package access.api;

import access.config.HashGenerator;
import access.exception.*;
import access.logging.AccessLogger;
import access.logging.Event;
import access.mail.MailBox;
import access.manage.Manage;
import access.model.*;
import access.provision.ProvisioningService;
import access.provision.graph.GraphResponse;
import access.provision.scim.OperationType;
import access.repository.InvitationRepository;
import access.repository.RemoteProvisionedUserRepository;
import access.repository.RoleRepository;
import access.repository.UserRepository;
import access.security.SuperAdmin;
import access.security.UserPermissions;
import access.validation.EmailFormatValidator;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.RedirectView;

import java.net.http.HttpClient;
import java.time.Instant;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static access.SwaggerOpenIdConfig.OPEN_ID_SCHEME_NAME;
import static java.util.stream.Collectors.toSet;

@RestController
@RequestMapping(value = {"/api/v1/invitations", "/api/external/v1/invitations"}, produces = MediaType.APPLICATION_JSON_VALUE)
@Transactional
@SecurityRequirement(name = OPEN_ID_SCHEME_NAME, scopes = {"openid"})
@EnableConfigurationProperties(SuperAdmin.class)
public class InvitationController implements HasManage {

    private static final Log LOG = LogFactory.getLog(InvitationController.class);

    private final MailBox mailBox;
    private final Manage manage;
    private final InvitationRepository invitationRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final EmailFormatValidator emailFormatValidator = new EmailFormatValidator();
    private final ProvisioningService provisioningService;
    private final SuperAdmin superAdmin;

    public InvitationController(MailBox mailBox,
                                Manage manage,
                                InvitationRepository invitationRepository,
                                UserRepository userRepository,
                                RoleRepository roleRepository,
                                ProvisioningService provisioningService,
                                SuperAdmin superAdmin) {
        this.mailBox = mailBox;
        this.manage = manage;
        this.invitationRepository = invitationRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.provisioningService = provisioningService;
        this.superAdmin = superAdmin;
    }

    @PostMapping("")
    public ResponseEntity<Map<String, Integer>> newInvitation(@Validated @RequestBody InvitationRequest invitationRequest,
                                                              @Parameter(hidden = true) User user) {
        if (!invitationRequest.getIntendedAuthority().equals(Authority.SUPER_USER)
                && CollectionUtils.isEmpty(invitationRequest.getRoleIdentifiers())) {
            throw new NotAllowedException("Invitation for non-super-user must contain at least one role");
        }
        //We need to assert validations on the roles soo we need to load them
        List<Role> requestedRoles = invitationRequest.getRoleIdentifiers().stream()
                .map(id -> roleRepository.findById(id).orElseThrow(NotFoundException::new)).toList();
        Authority intendedAuthority = invitationRequest.getIntendedAuthority();
        UserPermissions.assertValidInvitation(user, intendedAuthority, requestedRoles);

        boolean isOverrideSettingsAllowed = requestedRoles.stream().allMatch(Role::isOverrideSettingsAllowed);
        if (!isOverrideSettingsAllowed) {
            invitationRequest.setEduIDOnly(requestedRoles.stream().anyMatch(Role::isEduIDOnly));
            invitationRequest.setEnforceEmailEquality(requestedRoles.stream().anyMatch(Role::isEnforceEmailEquality));
            if (intendedAuthority.equals(Authority.GUEST)) {
                Integer defaultExpiryDays = requestedRoles.stream().max(Comparator.comparingInt(Role::getDefaultExpiryDays)).get().getDefaultExpiryDays();
                invitationRequest.setRoleExpiryDate(Instant.now().plus(defaultExpiryDays, ChronoUnit.DAYS));
            }
        }

        List<Invitation> invitations = invitationRequest.getInvites().stream()
                .filter(emailFormatValidator::isValid)
                .map(invitee -> new Invitation(
                        intendedAuthority,
                        HashGenerator.generateHash(),
                        invitee,
                        invitationRequest.isEnforceEmailEquality(),
                        invitationRequest.isEduIDOnly(),
                        invitationRequest.getMessage(),
                        user,
                        invitationRequest.getExpiryDate(),
                        invitationRequest.getRoleExpiryDate(),
                        requestedRoles.stream()
                                .map(InvitationRole::new)
                                .collect(toSet())))
                .toList();
        invitationRepository.saveAll(invitations);

        List<GroupedProviders> groupedProviders = getGroupedProviders(requestedRoles);
        invitations.forEach(invitation -> mailBox.sendInviteMail(user, invitation, groupedProviders));
        invitations.forEach(invitation -> AccessLogger.invitation(LOG, Event.Created, invitation));
        return Results.createResult();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInvitation(@PathVariable("id") Long id,
                                                 @Parameter(hidden = true) User user) {
        LOG.debug("/deleteInvitation");
        //We need to assert validations on the roles soo we need to load them
        Invitation invitation = invitationRepository.findById(id).orElseThrow(NotFoundException::new);
        List<Role> requestedRoles = invitation.getRoles().stream()
                .map(InvitationRole::getRole).toList();
        Authority intendedAuthority = invitation.getIntendedAuthority();
        UserPermissions.assertValidInvitation(user, intendedAuthority, requestedRoles);

        AccessLogger.invitation(LOG, Event.Deleted, invitation);
        invitationRepository.delete(invitation);

        return Results.deleteResult();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Integer>> resendInvitation(@PathVariable("id") Long id,
                                                                 @Parameter(hidden = true) User user) {
        LOG.debug("/resendInvitation");
        //We need to assert validations on the roles soo we need to load them
        Invitation invitation = invitationRepository.findById(id).orElseThrow(NotFoundException::new);
        List<Role> requestedRoles = invitation.getRoles().stream()
                .map(InvitationRole::getRole).toList();
        Authority intendedAuthority = invitation.getIntendedAuthority();
        UserPermissions.assertValidInvitation(user, intendedAuthority, requestedRoles);
        List<GroupedProviders> groupedProviders = getGroupedProviders(requestedRoles);

        mailBox.sendInviteMail(user, invitation, groupedProviders);
        if (invitation.getExpiryDate().isBefore(Instant.now())) {
            invitation.setExpiryDate(Instant.now().plus(Period.ofDays(14)));
            invitationRepository.save(invitation);
        }

        AccessLogger.invitation(LOG, Event.Resend, invitation);

        return Results.createResult();
    }

    @GetMapping("public")
    public ResponseEntity<Invitation> getInvitation(@RequestParam("hash") String hash) {
        Invitation invitation = invitationRepository.findByHash(hash).orElseThrow(NotFoundException::new);
        if (!invitation.getStatus().equals(Status.OPEN)) {
            throw new InvitationStatusException();
        }
        manage.deriveRemoteApplications(invitation.getRoles().stream().map(InvitationRole::getRole).toList());
        return ResponseEntity.ok(invitation);
    }

    @GetMapping("all")
    public ResponseEntity<List<Invitation>> all(@Parameter(hidden = true) User user) {
        LOG.debug("/all invitations");
        UserPermissions.assertAuthority(user, Authority.INVITER);
        if (user.isSuperUser()) {
            return ResponseEntity.ok(invitationRepository.findAll());//findByStatus(Status.OPEN));
        }
        List<Role> roles = user.getUserRoles().stream().map(UserRole::getRole).toList();
        return ResponseEntity.ok(invitationRepository.findByRoles_roleIsIn(roles));
    }


    @PostMapping("accept")
    public ResponseEntity<Map<String, String>> accept(@Validated @RequestBody AcceptInvitation acceptInvitation,
                                                       Authentication authentication) {
        Invitation invitation = invitationRepository.findByHash(acceptInvitation.hash()).orElseThrow(NotFoundException::new);
        if (!invitation.getId().equals(acceptInvitation.invitationId())) {
            throw new NotFoundException();
        }
        if (!invitation.getStatus().equals(Status.OPEN)) {
            throw new InvitationStatusException();
        }
        if (invitation.getExpiryDate().isBefore(Instant.now())) {
            throw new InvitationExpiredException();
        }
        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
        Map<String, Object> attributes = token.getPrincipal().getAttributes();
        String sub = (String) attributes.get("sub");
        Optional<User> optionalUser = userRepository.findBySubIgnoreCase(sub);
        User user = optionalUser.orElseGet(() -> {
            boolean superUser = this.superAdmin.getUsers().stream().anyMatch(superSub -> superSub.equals(sub))
                    || invitation.getIntendedAuthority().equals(Authority.SUPER_USER);
            return new User(superUser, attributes);
        });

        checkEmailEquality(user, invitation);
        user.setLastActivity(Instant.now());

        invitation.setStatus(Status.ACCEPTED);
        invitation.setSubInvitee(sub);
        invitationRepository.save(invitation);
        AccessLogger.invitation(LOG, Event.Accepted, invitation);

        /*
         * Chicken & egg problem. The user including his / hers roles must be first provisioned, and then we
         * need to send the updateRoleRequests for each new Role of this user.
         */
        List<UserRole> newUserRoles = new ArrayList<>();
        invitation.getRoles()
                .forEach(invitationRole -> {
                    Role role = invitationRole.getRole();
                    Optional<UserRole> optionalUserRole = user.getUserRoles().stream()
                            .filter(userRole -> userRole.getRole().getId().equals(role.getId())).findFirst();
                    if (optionalUserRole.isPresent()) {
                        UserRole userRole = optionalUserRole.get();
                        if (!userRole.getAuthority().hasEqualOrHigherRights(invitation.getIntendedAuthority())) {
                            userRole.setAuthority(invitation.getIntendedAuthority());
                            userRole.setEndDate(invitation.getRoleExpiryDate());
                        }
                    } else {
                        UserRole userRole = new UserRole(
                                invitation.getInviter().getName(),
                                user,
                                role,
                                invitation.getIntendedAuthority(),
                                invitation.getRoleExpiryDate());
                        user.addUserRole(userRole);
                        newUserRoles.add(userRole);
                    }
                });
        userRepository.save(user);
        AccessLogger.user(LOG, Event.Created, user);

        //Already provisioned users in the remote systems are ignored / excluded
        Optional<GraphResponse> graphResponse = provisioningService.newUserRequest(user);
        newUserRoles.forEach(userRole -> provisioningService.updateGroupRequest(userRole, OperationType.Add));

        LOG.info(String.format("User %s accepted invitation with role(s) %s",
                user.getName(),
                invitation.getRoles().stream().map(role -> role.getRole().getName()).collect(Collectors.joining(", "))));

        Map<String, String> body = graphResponse.map(graph -> Map.of("inviteRedeemUrl", graph.inviteRedeemUrl())).
                orElse(Map.of("status", "ok"));
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @GetMapping("roles/{roleId}")
    public ResponseEntity<List<Invitation>> byRole(@PathVariable("roleId") Long roleId, @Parameter(hidden = true) User user) {
        LOG.debug("/me");
        Role role = roleRepository.findById(roleId).orElseThrow(NotFoundException::new);
        UserPermissions.assertRoleAccess(user, role, Authority.INVITER);
        List<Invitation> invitations = invitationRepository.findByStatusAndRoles_role(Status.OPEN, role);
        return ResponseEntity.ok(invitations);
    }

    public Manage getManage() {
        return manage;
    }

    private void checkEmailEquality(User user, Invitation invitation) {
        if (invitation.isEnforceEmailEquality() && !invitation.getEmail().equalsIgnoreCase(user.getEmail())) {
            throw new InvitationEmailMatchingException(
                    String.format("Invitation email %s does not match user email %s", invitation.getEmail(), user.getEmail()));
        }
    }


}
