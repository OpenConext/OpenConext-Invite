package access.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Set;

@Entity(name = "user_roles")
@NoArgsConstructor
@Getter
@Setter
public class UserRole implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "inviter")
    private String inviter;

    @Column(name = "end_date")
    private Instant endDate;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "expiry_notifications")
    private int expiryNotifications;

    @Column(name = "guest_role_included")
    private boolean guestRoleIncluded;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id")
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column
    @NotNull
    private Authority authority = Authority.GUEST;

    @Transient
    private Map<String, Object> userInfo;

    public UserRole(Authority authority, Role role) {
        this(null, null, role, authority);
    }

    public UserRole(String inviter, User user, Role role, Authority authority) {
        this(inviter, user, role, authority, false, null);
    }

    public UserRole(String inviter, User user, Role role, @NotNull Authority authority, boolean guestRoleIncluded, Instant endDate) {
        this.inviter = inviter;
        this.user = user;
        this.role = role;
        this.authority = authority;
        this.guestRoleIncluded = guestRoleIncluded;
        this.endDate = endDate == null && authority.equals(Authority.GUEST) ? Instant.now().plus(role.getDefaultExpiryDays(), ChronoUnit.DAYS) : endDate;
        this.createdAt = Instant.now();
    }

    @Transient
    @JsonIgnore
    public boolean hasAccessToApplication(Role otherRole) {
        Set<Application> mineApplications = this.role.applicationsUsed();
        Set<Application> otherApplications = otherRole.applicationsUsed();
        return mineApplications.stream()
                .anyMatch(application -> otherApplications.stream()
                        .anyMatch(otherApplication -> otherApplication.getManageId().equals(application.getManageId())));
    }
}
