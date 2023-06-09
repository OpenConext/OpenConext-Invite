package access.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;


@Entity(name = "remote_provisioned_groups")
@NoArgsConstructor
@Getter
@Setter
public class RemoteProvisionedGroup implements Serializable, RemoteScimIdentifier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Role role;

    @Column(name = "remote_scim_identifier")
    @NotNull
    private String remoteScimIdentifier;

    @Column(name = "manage_provisioning_id")
    @NotNull
    private String manageProvisioningId;

    public RemoteProvisionedGroup(Role role, @NotNull String remoteScimIdentifier, @NotNull String manageProvisioningId) {
        this.role = role;
        this.remoteScimIdentifier = remoteScimIdentifier;
        this.manageProvisioningId = manageProvisioningId;
    }
}
