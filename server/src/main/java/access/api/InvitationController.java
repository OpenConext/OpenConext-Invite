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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
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

    private final SuperAdmin superAdmin;
    public InvitationController(MailBox mailBox,
                                Manage manage,
                                InvitationRepository invitationRepository,
                                UserRepository userRepository,
                                RoleRepository roleRepository,
                                SuperAdmin superAdmin) {
        this.mailBox = mailBox;
        this.manage = manage;
        this.invitationRepository = invitationRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
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
    public ResponseEntity<User> accept(@Validated @RequestBody AcceptInvitation acceptInvitation, Authentication authentication) {
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
        OAuth2User principal = token.getPrincipal();
        Map<String, Object> attributes = principal.getAttributes();
        String sub = (String) attributes.get("sub");
        Optional<User> optionalUser = userRepository.findBySubIgnoreCase(sub);
        User user = optionalUser.orElseGet(() -> {
            boolean superUser = this.superAdmin.getUsers().stream().anyMatch(superSub -> superSub.equals(sub));
            return userRepository.save(new User(superUser, attributes));
        });

        checkEmailEquality(user, invitation);

        invitation.setStatus(Status.ACCEPTED);
        Authority intendedAuthority = invitation.getIntendedAuthority();
        String email = invitation.getEmail();
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    private void checkEmailEquality(User user, Invitation invitation) {
        if (invitation.isEnforceEmailEquality() && !invitation.getEmail().equalsIgnoreCase(user.getEmail())) {
            throw new InvitationEmailMatchingException(
                    String.format("Invitation email %s does not match user email %s", invitation.getEmail(), user.getEmail()));
        }
    }

}
