package access.model;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    private boolean enforceEmailEquality;

    private boolean eduIDOnly;

    private boolean guestRoleIncluded;

    @NotEmpty
    private List<String> invites;

    private List<Long> roleIdentifiers;

    private Instant roleExpiryDate;

    @NotNull
    private Instant expiryDate;
}
