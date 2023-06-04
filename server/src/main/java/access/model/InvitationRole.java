package access.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.Instant;

@Entity(name = "invitation_roles")
@NoArgsConstructor
@Getter
@Setter
@ToString
public class InvitationRole implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "end_date")
    private Instant endDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invitation_id")
    @JsonIgnore
    @ToString.Exclude
    private Invitation invitation;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id")
    private Role role;

    public InvitationRole(Role role) {
        this.role = role;
    }

    public InvitationRole(Role role, Instant endDate) {
        this.role = role;
        this.endDate = endDate;
    }
}
