package access.api;

import access.config.HashGenerator;
import access.exception.NotAllowedException;
import access.exception.NotFoundException;
import access.logging.AccessLogger;
import access.logging.Event;
import access.mail.MailBox;
import access.model.*;
import access.security.RemoteUser;
import access.security.RemoteUserPermissions;
import access.security.UserPermissions;
import access.validation.EmailFormatValidator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

import static java.util.stream.Collectors.toSet;

public class InvitationOperations {

    private static final Log LOG = LogFactory.getLog(InvitationOperations.class);

    private final EmailFormatValidator emailFormatValidator = new EmailFormatValidator();

    private final InvitationResource invitationResource;

    public InvitationOperations(InvitationResource invitationResource) {
        this.invitationResource = invitationResource;
    }

    public ResponseEntity<InvitationResponse> sendInvitation(InvitationRequest invitationRequest,
                                                             User user,
                                                             RemoteUser remoteUser) {
        Authority intendedAuthority = invitationRequest.getIntendedAuthority();
        if (!List.of(Authority.INSTITUTION_ADMIN, Authority.SUPER_USER).contains(intendedAuthority)
                && CollectionUtils.isEmpty(invitationRequest.getRoleIdentifiers())) {
            throw new NotAllowedException("Invitation for non-super-user or institution-admin must contain at least one role");
        }
        //We need to assert validations on the roles soo we need to load them
        List<Role> requestedRoles = invitationRequest.getRoleIdentifiers().stream()
                .map(id -> invitationResource.getRoleRepository().findById(id).orElseThrow(() -> new NotFoundException("Role not found"))).toList();

        if (user != null) {
            UserPermissions.assertValidInvitation(user, intendedAuthority, requestedRoles);
        } else {
            RemoteUserPermissions.assertApplicationAccess(remoteUser, requestedRoles);
        }

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
                .filter(email -> {
                    boolean valid = emailFormatValidator.isValid(email);
                    if (!valid) {
                        LOG.debug("Not sending invalid email for invitation: " + email);
                    }
                    return valid;
                })
                .map(invitee -> new Invitation(
                        intendedAuthority,
                        HashGenerator.generateRandomHash(),
                        invitee,
                        invitationRequest.isEnforceEmailEquality(),
                        invitationRequest.isEduIDOnly(),
                        invitationRequest.isGuestRoleIncluded(),
                        invitationRequest.getMessage(),
                        invitationRequest.getLanguage(),
                        user,
                        invitationRequest.getExpiryDate(),
                        invitationRequest.getRoleExpiryDate(),
                        requestedRoles.stream()
                                .map(InvitationRole::new)
                                .collect(toSet())))
                .toList();
        if (user == null) {
            invitations.forEach(invitation -> invitation.setRemoteApiUser(remoteUser.getName()));
        }
        this.invitationResource.getInvitationRepository().saveAll(invitations);

        List<GroupedProviders> groupedProviders = this.invitationResource.getManage().getGroupedProviders(requestedRoles);
        MailBox mailBox = this.invitationResource.getMailBox();
        List<RecipientInvitationURL> recipientInvitationURLs = invitations.stream()
                .map(invitation -> new RecipientInvitationURL(invitation.getEmail(),
                        mailBox.inviteMailURL(invitation)))
                .toList();
        if (!invitationRequest.isSuppressSendingEmails()) {
            invitations.forEach(invitation -> mailBox.sendInviteMail(user, invitation, groupedProviders, invitationRequest.getLanguage()));
        }
        invitations.forEach(invitation -> AccessLogger.invitation(LOG, Event.Created, invitation));
        return ResponseEntity.status(HttpStatus.CREATED).body(new InvitationResponse(HttpStatus.CREATED.value(), recipientInvitationURLs));
    }

}
