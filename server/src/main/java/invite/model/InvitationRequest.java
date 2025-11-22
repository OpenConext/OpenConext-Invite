package invite.model;

import invite.exception.InvalidInputException;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.time.Instant;
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

    private RequestedAuthnContext requestedAuthnContext;

    private boolean guestRoleIncluded;

    private boolean suppressSendingEmails;

    private List<String> invites;

    private List<Invite> invitesWithInternalPlaceholderIdentifiers;

    private List<Long> roleIdentifiers;

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
