package invite.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity(name = "user_roles_audit")
@NoArgsConstructor
@Getter
@Setter
public class UserRoleAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "user_email", nullable = false)
    private String userEmail;

    @Column(name = "role_id", nullable = false)
    private Long roleId;

    @Column(name = "role_name", nullable = false)
    private String roleName;

    @Column(name = "end_date")
    private Instant endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false)
    private ActionType action;

    @Enumerated(EnumType.STRING)
    @Column
    @NotNull
    private Authority authority;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    public UserRoleAudit(ActionType action, UserRole userRole, String createdBy) {
        User user = userRole.getUser();
        Role role = userRole.getRole();
        this.userId = user.getId();
        this.userEmail = user.getEmail();
        this.roleId = role.getId();
        this.roleName = role.getName();
        this.endDate = userRole.getEndDate();
        this.action = action;
        this.authority = userRole.getAuthority();
        this.createdAt = Instant.now();
        this.createdBy = createdBy;
    }

    public enum ActionType {
        ADD, DELETE, UPDATE
    }

}
