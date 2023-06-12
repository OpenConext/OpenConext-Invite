package access.api;

import access.config.HashGenerator;
import access.exception.InvitationEmailMatchingException;
import access.exception.InvitationExpiredException;
import access.exception.InvitationStatusException;
import access.exception.NotFoundException;
import access.mail.MailBox;
import access.manage.ManageIdentifier;
import access.manage.Manage;
import access.model.*;
import access.provision.ProvisioningService;
import access.provision.scim.OperationType;
import access.repository.InvitationRepository;
import access.repository.RoleRepository;
import access.repository.UserRepository;
import access.secuirty.SuperAdmin;
import access.secuirty.UserPermissions;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static access.SwaggerOpenIdConfig.OPEN_ID_SCHEME_NAME;
import static java.util.stream.Collectors.toSet;

@RestController
@RequestMapping(value = {"/api/v1/invitations", "/api/external/v1/invitations"}, produces = MediaType.APPLICATION_JSON_VALUE)
@Transactional
@SecurityRequirement(name = OPEN_ID_SCHEME_NAME, scopes = {"openid"})
@EnableConfigurationProperties(SuperAdmin.class)
public class InvitationController {

    private static final Log LOG = LogFactory.getLog(InvitationController.class);

    private final MailBox mailBox;
    private final Manage manage;
    private final InvitationRepository invitationRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
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
        LOG.debug("/newInvitation");
        //We need to assert validations on the roles soo we need to load them
        List<Role> requestedRoles = invitationRequest.getRoleIdentifiers().stream()
                .map(id -> roleRepository.findById(id).orElseThrow(NotFoundException::new)).toList();
        Authority intendedAuthority = invitationRequest.getIntendedAuthority();
        UserPermissions.assertValidInvitation(user, intendedAuthority, requestedRoles);

        List<Invitation> invitations = invitationRequest.getInvites().stream()
                .map(invitee -> new Invitation(
                        intendedAuthority,
                        HashGenerator.generateHash(),
                        invitee,
                        invitationRequest.isEnforceEmailEquality(),
                        user,
                        requestedRoles.stream()
                                .map(role -> new InvitationRole(role, invitationRequest.getExpiryDate()))
                                .collect(toSet())))
                .toList();
        invitationRepository.saveAll(invitations);

        //We need to display the roles per manage application with the logo
        List<GroupedProviders> groupedProviders = requestedRoles.stream()
                .collect(Collectors.groupingBy(role -> new ManageIdentifier(role.getManageId(), role.getManageType())))
                .entrySet().stream()
                .map(entry -> new GroupedProviders(
                        manage.providerById(entry.getKey().entityType(), entry.getKey().id()),
                        entry.getValue(),
                        UUID.randomUUID().toString())
                ).toList();
        invitations.forEach(invitation -> mailBox.sendInviteMail(user, invitation, groupedProviders));
        return Results.createResult();
    }

    @GetMapping("public")
    public ResponseEntity<MetaInvitation> getInvitation(@RequestParam("hash") String hash) {
        Invitation invitation = invitationRepository.findByHash(hash).orElseThrow(NotFoundException::new);
        if (!invitation.getStatus().equals(Status.OPEN)) {
            throw new InvitationStatusException();
        }
        List<Map<String, Object>> providers = invitation.getRoles().stream()
                .map(invitationRole -> invitationRole.getRole())
                .map(role -> manage.providerById(role.getManageType(), role.getManageId()))
                .toList();
        return ResponseEntity.ok(new MetaInvitation(invitation, providers));
    }


    @PostMapping("accept")
    public ResponseEntity<Map<String, Integer>> accept(@Validated @RequestBody AcceptInvitation acceptInvitation,
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
            boolean superUser = this.superAdmin.getUsers().stream().anyMatch(superSub -> superSub.equals(sub));
            return new User(superUser, attributes);
        });

        checkEmailEquality(user, invitation);
        user.setLastActivity(Instant.now());

        invitation.setStatus(Status.ACCEPTED);
        invitationRepository.save(invitation);

        /*
         * Chicken & egg problem. The user including his / hers roles must be first provisioned, and then we
         * need to send the updateRoleRequests for each new Role of this user.
         */
        List<UserRole> newUserRoles = new ArrayList<>();
        invitation.getRoles()
                .forEach(invitationRole -> {
                    Role role = invitationRole.getRole();
                    if (user.getUserRoles().stream().noneMatch(userRole -> userRole.getRole().getId().equals(role.getId()))) {
                        UserRole userRole = new UserRole(
                                invitation.getInviter().getName(),
                                user,
                                role,
                                invitation.getIntendedAuthority(),
                                invitationRole.getEndDate());
                        user.addUserRole(userRole);
                        newUserRoles.add(userRole);
                    }
                });
        userRepository.save(user);
        //Already provisioned users in the remote systems are ignored / excluded
        provisioningService.newUserRequest(user);
        newUserRoles.forEach(userRole -> provisioningService.updateGroupRequest(userRole, OperationType.Add));

        LOG.info(String.format("User %s accepted invitation with role(s) %s",
                user.getName(),
                invitation.getRoles().stream().map(role -> role.getRole().getName()).collect(Collectors.joining(", "))));

        return Results.createResult();
    }

    private void checkEmailEquality(User user, Invitation invitation) {
        if (invitation.isEnforceEmailEquality() && !invitation.getEmail().equalsIgnoreCase(user.getEmail())) {
            throw new InvitationEmailMatchingException(
                    String.format("Invitation email %s does not match user email %s", invitation.getEmail(), user.getEmail()));
        }
    }

}
