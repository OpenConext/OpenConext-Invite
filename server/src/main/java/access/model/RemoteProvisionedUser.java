package access.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;


@Entity(name = "remote_provisioned_users")
@NoArgsConstructor
public class RemoteProvisionedUser implements Serializable, RemoteScimIdentifier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private User user;

    @Column(name = "remote_scim_identifier")
    @NotNull
    private String remoteScimIdentifier;

    @Column(name = "manage_provisioning_id")
    @NotNull
    private String manageProvisioningId;

    public RemoteProvisionedUser(User user, @NotNull String remoteScimIdentifier, @NotNull String manageProvisioningId) {
        this.user = user;
        this.remoteScimIdentifier = remoteScimIdentifier;
        this.manageProvisioningId = manageProvisioningId;
    }

    @Override
    public String getRemoteScimIdentifier() {
        return this.remoteScimIdentifier;
    }
}
