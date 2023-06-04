package access.api;

import access.config.HashGenerator;
import access.exception.NotFoundException;
import access.mail.MailBox;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static access.SwaggerOpenIdConfig.OPEN_ID_SCHEME_NAME;
import static java.util.stream.Collectors.toSet;

@RestController
@RequestMapping(value = {"/api/v1/invitations", "/api/external/v1/invitations"}, produces = MediaType.APPLICATION_JSON_VALUE)
@Transactional
@SecurityRequirement(name = OPEN_ID_SCHEME_NAME, scopes = {"openid"})
public class InvitationController {

    private static final Log LOG = LogFactory.getLog(InvitationController.class);

    private final MailBox mailBox;
    private final InvitationRepository invitationRepository;
    private final RoleRepository roleRepository;

    public InvitationController(MailBox mailBox,
                                InvitationRepository invitationRepository,
                                RoleRepository roleRepository) {
        this.mailBox = mailBox;
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
        //TODO send mails
        invitationRepository.saveAll(invitations);
        return ResponseEntity.ok().build();
    }

}
