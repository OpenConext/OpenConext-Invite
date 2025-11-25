package invite.api;

import invite.config.HashGenerator;
import invite.exception.NotAllowedException;
import invite.exception.NotFoundException;
import invite.logging.AccessLogger;
import invite.logging.Event;
import invite.mail.MailBox;
import invite.model.*;
import invite.repository.InvitationRepository;
import invite.security.RemoteUser;
import invite.security.RemoteUserPermissions;
import invite.security.UserPermissions;
import invite.validation.EmailFormatValidator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;

import java.time.Instant;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.*;

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
        invitationRequest.verify();
        Authority intendedAuthority = invitationRequest.getIntendedAuthority();
        if (!List.of(Authority.INSTITUTION_ADMIN, Authority.SUPER_USER).contains(intendedAuthority)
                && CollectionUtils.isEmpty(invitationRequest.getRoleIdentifiers())) {
            throw new NotAllowedException("Invitation for non-super-user or institution-admin must contain at least one role");
        }
        //We need to assert validations on the roles soo we need to load them
        List<Role> requestedRoles = invitationRequest.getRoleIdentifiers().stream()
                .map(id -> invitationResource.getRoleRepository().findById(id)
                        .orElseThrow(() -> new NotFoundException("Role not found"))).toList();

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
                Instant latest = calculateInvitationExpiry(requestedRoles);
                invitationRequest.setRoleExpiryDate(latest);
            }
        }
        List<Invite> invites = invitationRequest.getInvitesWithInternalPlaceholderIdentifiers();
        if (invites == null) {
            invites = new ArrayList<>();
        }
        List<String> requestInvites = invitationRequest.getInvites();
        if (requestInvites != null) {
            invites.addAll(requestInvites.stream().map(invite -> new Invite(invite, null)).toList());
        }
        List<Invitation> invitations = invites.stream()
                .filter(invite -> {
                    boolean valid = emailFormatValidator.isValid(invite.getEmail());
                    if (!valid) {
                        LOG.debug("Not sending invalid email for invitation: " + invite.getEmail());
                    }
                    return valid;
                })
                .map(invite -> new Invitation(
                        intendedAuthority,
                        HashGenerator.generateRandomHash(),
                        invite.getEmail(),
                        invitationRequest.isEnforceEmailEquality(),
                        invitationRequest.isEduIDOnly(),
                        invitationRequest.getRequestedAuthnContext(),
                        invitationRequest.isGuestRoleIncluded(),
                        invitationRequest.getMessage(),
                        invitationRequest.getLanguage(),
                        user,
                        invitationRequest.getExpiryDate(),
                        invitationRequest.getRoleExpiryDate(),
                        requestedRoles.stream()
                                .map(InvitationRole::new)
                                .collect(toSet()),
                        invite.getInternalPlaceholderIdentifier())
                ).toList();
        if (user == null) {
            invitations.forEach(invitation -> invitation.setRemoteApiUser(remoteUser.getName()));
        }
        if (intendedAuthority.equals(Authority.INSTITUTION_ADMIN)) {
            invitations.forEach(invitation -> invitation.setOrganizationGUID(
                    user.isSuperUser() ? invitationRequest.getOrganizationGUID() : user.getOrganizationGUID())
            );
        }

        this.invitationResource.getInvitationRepository().saveAll(invitations);

        List<GroupedProviders> groupedProviders = this.invitationResource.getManage().getGroupedProviders(requestedRoles);
        MailBox mailBox = this.invitationResource.getMailBox();
        List<RecipientInvitationURL> recipientInvitationURLs = invitations.stream()
                .map(invitation -> new RecipientInvitationURL(invitation.getEmail(),
                        mailBox.inviteMailURL(invitation)))
                .toList();


        if (!invitationRequest.isSuppressSendingEmails()) {

            invitations.forEach(invitation -> {
                Optional<String> idpName = this.identityProviderName(invitation);
                mailBox.sendInviteMail(user == null ? remoteUser : user,
                        invitation, groupedProviders, invitationRequest.getLanguage(), idpName);
            });
        }
        invitations.forEach(invitation -> AccessLogger.invitation(LOG, Event.Created, invitation));
        return ResponseEntity.status(HttpStatus.CREATED).body(new InvitationResponse(HttpStatus.CREATED.value(), recipientInvitationURLs));
    }

    public static Instant calculateInvitationExpiry(List<Role> requestedRoles) {
        Integer defaultExpiryDays = requestedRoles.stream()
                .filter(role -> role.getDefaultExpiryDays() != null)
                .max(Comparator.comparingInt(Role::getDefaultExpiryDays))
                .map(Role::getDefaultExpiryDays)
                .orElse(0);
        Instant now = Instant.now();
        Instant defaultExpiryDate = requestedRoles.stream()
                .filter(role -> role.getDefaultExpiryDate() != null)
                .max(Comparator.comparing(Role::getDefaultExpiryDate))
                .map(Role::getDefaultExpiryDate)
                .orElse(now);
        Instant expiryByDays = now.plus(defaultExpiryDays, ChronoUnit.DAYS);
        return expiryByDays.isAfter(defaultExpiryDate) ? expiryByDays : defaultExpiryDate;
    }

    public ResponseEntity<Map<String, Integer>> resendInvitation(Long id,
                                                                 User user,
                                                                 RemoteUser remoteUser) {
        String name = user != null ? user.getEduPersonPrincipalName() : remoteUser.getDisplayName();

        LOG.debug(String.format("/resendInvitation/%s by user %s", id, name));

        //We need to assert validations on the roles soo we need to load them
        InvitationRepository invitationRepository = this.invitationResource.getInvitationRepository();

        Invitation invitation = invitationRepository.findById(id).orElseThrow(() -> new NotFoundException("Invitation not found"));
        List<Role> requestedRoles = invitation.getRoles().stream()
                .map(InvitationRole::getRole).toList();
        Authority intendedAuthority = invitation.getIntendedAuthority();
        if (user != null) {
            UserPermissions.assertValidInvitation(user, intendedAuthority, requestedRoles);
        } else {
            RemoteUserPermissions.assertApplicationAccess(remoteUser, requestedRoles);
        }

        List<GroupedProviders> groupedProviders = this.invitationResource.getManage().getGroupedProviders(requestedRoles);
        Provisionable provisionable = user != null ? user : remoteUser;
        Optional<String> idpName = identityProviderName(invitation);

        this.invitationResource.getMailBox()
                .sendInviteMail(provisionable,
                        invitation,
                        groupedProviders,
                        invitation.getLanguage(),
                        idpName);
        if (invitation.getExpiryDate().isBefore(Instant.now())) {
            invitation.setExpiryDate(Instant.now().plus(Period.ofDays(14)));
            invitationRepository.save(invitation);
        }

        AccessLogger.invitation(LOG, Event.Resend, invitation);

        return Results.createResult();
    }

    private Optional<String> identityProviderName(Invitation invitation) {
        return Optional.ofNullable(invitation.getOrganizationGUID())
                .map(organisationGUID -> this.invitationResource.getManage().identityProvidersByInstitutionalGUID(organisationGUID))
                .stream()
                .flatMap(Collection::stream)
                .findFirst()
                .map(idp -> (String) idp.get("name:en"));

    }

}
