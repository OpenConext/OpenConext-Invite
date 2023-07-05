package provisioning.model;


import access.provision.ProvisioningType;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


import java.io.Serializable;

@Entity(name = "provisionings")
@NoArgsConstructor
@Getter
@Setter
public class Provisioning implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "provisioning_type")
    private ProvisioningType provisioningType;

    @Column
    private JsonNode message;

    @Enumerated(EnumType.STRING)
    @Column(name = "method")
    private HttpMethod method;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type")
    private ResourceType resourceType;

    @Column
    private String url;

    public Provisioning(ProvisioningType provisioningType, JsonNode message, HttpMethod method, ResourceType resourceType, String url) {
        this.provisioningType = provisioningType;
        this.message = message;
        this.method = method;
        this.resourceType = resourceType;
        this.url = url;
    }
}
