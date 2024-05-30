package access.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.time.Instant;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Entity(name = "invitations")
@NoArgsConstructor
@Getter
@Setter
public class Invitation implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "intended_authority")
    private Authority intendedAuthority;

    @Enumerated(EnumType.STRING)
    private Language language;

    @Enumerated(EnumType.STRING)
    private Status status;

    @Column
    private String email;

    @Column
    private String message;

    @Column
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String hash;

    @Column(name = "sub_invitee")
    private String subInvitee;

    @Column(name = "enforce_email_equality")
    private boolean enforceEmailEquality;

    @Column(name = "edu_id_only")
    private boolean eduIDOnly;

    @Column(name = "guest_role_included")
    private boolean guestRoleIncluded;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "expiry_date")
    private Instant expiryDate;

    @Column(name = "role_expiry_date")
    private Instant roleExpiryDate;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "inviter_id")
    @JsonIgnore
    private User inviter;

    @OneToMany(mappedBy = "invitation", orphanRemoval = true, fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Set<InvitationRole> roles = new HashSet<>();

    @Transient
    private boolean emailEqualityConflict = false;

    public Invitation(Authority intendedAuthority,
                      String hash,
                      String email,
                      boolean enforceEmailEquality,
                      boolean eduIDOnly,
                      boolean guestRoleIncluded,
                      String message,
                      Language language,
                      User inviter,
                      Instant expiryDate,
                      Instant roleExpiryDate,
                      @NotEmpty Set<InvitationRole> roles) {
        this.intendedAuthority = intendedAuthority;
        this.hash = hash;
        this.enforceEmailEquality = enforceEmailEquality;
        this.eduIDOnly = eduIDOnly;
        this.guestRoleIncluded = guestRoleIncluded;
        this.message = message;
        this.inviter = inviter;
        this.status = Status.OPEN;
        this.roles = roles;
        this.email = email;
        this.expiryDate = expiryDate == null ? Instant.now().plus(Period.ofDays(14)) : expiryDate;
        this.roleExpiryDate = this.roleExpiryDate(roles, roleExpiryDate, intendedAuthority);
        this.createdAt = Instant.now();
        roles.forEach(role -> role.setInvitation(this));
        this.language = language;
    }

    private Instant roleExpiryDate(@NotEmpty Set<InvitationRole> roles, Instant roleExpiryDate, Authority intendedAuthority) {
        if (roleExpiryDate != null || !intendedAuthority.equals(Authority.GUEST)) {
            return roleExpiryDate;
        }
        Integer days = roles.stream()
                .map(invitationRole -> invitationRole.getRole().getDefaultExpiryDays())
                .filter(Objects::nonNull)
                .min(Comparator.naturalOrder())
                .orElse(365);
        return Instant.now().plus(days, ChronoUnit.DAYS);
    }

    //used in the mustache templates
    @JsonIgnore
    public List<String> anyRoles() {
        return CollectionUtils.isEmpty(this.roles) ? Collections.emptyList() : Arrays.asList("will-iterate-once");
    }

    @JsonProperty(value = "inviter", access = JsonProperty.Access.READ_ONLY)
    public Map<String, Object> getInviterEmail() {
        User inviter = this.getInviter();
        return inviter != null ? Map.of(
                "email", inviter.getEmail(),
                "name", StringUtils.hasText(inviter.getName()) ? inviter.getName() : inviter.getEmail(),
                    "user_id", inviter.getId())
                : Collections.emptyMap();
    }

}
