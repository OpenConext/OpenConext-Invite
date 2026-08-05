package invite.model;

import invite.config.RequestedAuthnContext;
import invite.exception.InvalidInputException;
import invite.manage.ManageIdentifier;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InvitationRequest implements Serializable {

    @NotNull
    private Authority intendedAuthority;

    private String message;

    private Language language;

    private boolean enforceEmailEquality;

    private boolean eduIDOnly;

    private String requestedAuthnContext;

    private boolean guestRoleIncluded;

    private boolean suppressSendingEmails;

    private List<String> invites = new ArrayList<>();

    private List<Invite> invitesWithInternalPlaceholderIdentifiers = new ArrayList<>();

    private List<Long> roleIdentifiers = new ArrayList<>();

    private List<ManageIdentifier> manageIdentifiers = new ArrayList<>();

    private String organizationGUID;

    private Instant roleExpiryDate;

    @NotNull
    private Instant expiryDate;

    public void verify() {
        if (CollectionUtils.isEmpty(invitesWithInternalPlaceholderIdentifiers) && CollectionUtils.isEmpty(invites)) {
            throw new InvalidInputException("Either at least one value for invitesWithInternalPlaceholderIdentifiers or invites is required");
        }
        if (!eduIDOnly && requestedAuthnContext != null) {
            throw new InvalidInputException("Not allowed to set requestedAuthnContext for not eduIDOnly");
        }
    }
}
