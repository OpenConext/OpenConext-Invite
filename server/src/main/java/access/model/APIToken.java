package access.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;

@Entity(name = "api_tokens")
@NoArgsConstructor
@Getter
@Setter
public class APIToken implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_guid")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String organizationGUID;

    @Column(name = "hashed_value")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String hashedValue;

    @Column(name = "description")
    private String description;

    @Column(name = "created_at")
    private Instant createdAt;

    public APIToken(String organizationGUID, String hashedValue, String description) {
        this.organizationGUID = organizationGUID;
        this.hashedValue = hashedValue;
        this.description = description;
        this.createdAt = Instant.now();
    }


}
