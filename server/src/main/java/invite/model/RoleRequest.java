package invite.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import invite.provision.scim.GroupURN;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Formula;

import java.io.Serializable;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@NoArgsConstructor
@Getter
@Setter
public class RoleRequest implements Serializable{

    @NotNull
    @NotBlank
    @Schema(description = "Unique name of the role", example = "Guest for my application", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "Brief explanation of this role's purpose", example = "Full access to all modules")
    private String description;

    @Schema(description = "Number of days until the memberschip automaticly expires. Use either defaultExpiryDays or defaultExpiryDate", example = "365")
    private Integer defaultExpiryDays;

    @Schema(description = "Specific timestamp end date for this role membership. Use either defaultExpiryDays or defaultExpiryDate", example = "2026-12-31T23:59:59Z")
    private Instant defaultExpiryDate;

    @Schema(description = "If true, the user's email must match the invitation email", defaultValue = "false")
    private boolean enforceEmailEquality;

    @Schema(description = "The invite can only be accepted by an eduID account", defaultValue = "false")
    private boolean eduIDOnly;

    @Schema(description = "Allow changing the settings when sending an invite.", defaultValue = "false")
    private boolean overrideSettingsAllowed;

    @Schema(description = "The unique identifier of the associated organization, only to be used bu SUPER-admins and internal API's", example = "550e8400-e29b-41d4-a716-446655440000")
    private String organizationGUID;

    @Schema(description = "The email address used", example = "550e8400-e29b-41d4-a716-446655440000")
    private String inviterDisplayName;

    @Schema(description = "The ACR value", example = "https://eduid.nl/trust/linked-institution")
    private String requestedAuthnContext;

    private Set<ApplicationUsage> applicationUsages = new HashSet<>();

    @Schema(description = "If true, the inviter receives an email when an invitation for this role is accepted", defaultValue = "false")
    private boolean notifyInviter;

    public RoleRequest(String name,
                       String description,
                       Integer defaultExpiryDays,
                       Instant defaultExpiryDate,
                       boolean enforceEmailEquality,
                       boolean eduIDOnly,
                       boolean overrideSettingsAllowed,
                       String organizationGUID,
                       String inviterDisplayName,
                       String requestedAuthnContext,
                       Set<ApplicationUsage> applicationUsages) {
        this.name = name;
        this.description = description;
        this.defaultExpiryDays = defaultExpiryDays;
        this.defaultExpiryDate = defaultExpiryDate;
        this.enforceEmailEquality = enforceEmailEquality;
        this.eduIDOnly = eduIDOnly;
        this.overrideSettingsAllowed = overrideSettingsAllowed;
        this.organizationGUID = organizationGUID;
        this.inviterDisplayName = inviterDisplayName;
        this.requestedAuthnContext = requestedAuthnContext;
        this.applicationUsages = applicationUsages;
    }

}
