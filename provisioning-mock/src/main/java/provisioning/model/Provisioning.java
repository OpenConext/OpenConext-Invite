package provisioning.model;


import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;

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

    @Column(name = "created_at")
    private Instant createdAt;

    @Transient
    private String prettyMessage;

    public Provisioning(ProvisioningType provisioningType, JsonNode message, HttpMethod method, ResourceType resourceType, String url) {
        this.provisioningType = provisioningType;
        this.message = message;
        this.method = method;
        this.resourceType = resourceType;
        this.url = url;
        this.createdAt = Instant.now();
    }
}
