package access.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity(name = "identity_providers")
@NoArgsConstructor
@Getter
@Setter
public class IdentityProvider {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "manage_id")
    private String manageId;

    @Column(name = "manage_name")
    private String manageName;

    @Column(name = "manage_entityid")
    private String manageEntityId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invitation_id")
    @JsonIgnore
    @ToString.Exclude
    private Invitation invitation;

    public IdentityProvider(String manageId, String manageName, String manageEntityId) {
        this.manageId = manageId;
        this.manageName = manageName;
        this.manageEntityId = manageEntityId;
    }
}
