package access.api;

import access.config.HashGenerator;
import access.exception.InvitationStatusException;
import access.exception.NotFoundException;
import access.mail.MailBox;
import access.manage.Identity;
import access.manage.Manage;
import access.model.*;
import access.repository.InvitationRepository;
import access.repository.RoleRepository;
import access.secuirty.UserPermissions;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static access.SwaggerOpenIdConfig.OPEN_ID_SCHEME_NAME;
import static java.util.stream.Collectors.toSet;

@RestController
@RequestMapping(value = {"/api/v1/invitations", "/api/external/v1/invitations"}, produces = MediaType.APPLICATION_JSON_VALUE)
@Transactional
@SecurityRequirement(name = OPEN_ID_SCHEME_NAME, scopes = {"openid"})
public class InvitationController {

    private static final Log LOG = LogFactory.getLog(InvitationController.class);

    private final MailBox mailBox;
    private final Manage manage;
    private final InvitationRepository invitationRepository;
    private final RoleRepository roleRepository;

    public InvitationController(MailBox mailBox,
                                Manage manage,
                                InvitationRepository invitationRepository,
                                RoleRepository roleRepository) {
        this.mailBox = mailBox;
        this.manage = manage;
        this.invitationRepository = invitationRepository;
        this.roleRepository = roleRepository;
    }

    @PostMapping("")
    public ResponseEntity<Void> newInvitation(@Validated @RequestBody InvitationRequest invitationRequest,
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
                .collect(Collectors.groupingBy(role -> new Identity(role.getManageId(), role.getManageType())))
                .entrySet().stream()
                .map(entry -> new GroupedProviders(
                        manage.providerById(entry.getKey().entityType(), entry.getKey().id()),
                        entry.getValue(),
                        UUID.randomUUID().toString())
                ).toList();
        invitations.forEach(invitation -> mailBox.sendInviteMail(user, invitation, groupedProviders));
        return ResponseEntity.ok().build();
    }

    @GetMapping("public")
    public ResponseEntity<Invitation> getInvitation(@RequestParam("hash") String hash) {
        Invitation invitation = invitationRepository.findByHash(hash).orElseThrow(NotFoundException::new);
        if (invitation.getStatus().equals(Status.OPEN)) {
            return ResponseEntity.ok(invitation);
        }
        throw new InvitationStatusException();
    }

}
