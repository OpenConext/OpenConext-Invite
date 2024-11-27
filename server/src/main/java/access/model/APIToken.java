package access.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
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
    private String organizationGUID;

    @Column(name = "hashed_value")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String hashedValue;

    @Column(name = "super_user_token")
    private boolean superUserToken;

    @Column(name = "description")
    @NotBlank
    private String description;

    @Column(name = "created_at")
    private Instant createdAt;

    public APIToken(String organizationGUID, String hashedValue, boolean superUserToken, String description) {
        this.organizationGUID = organizationGUID;
        this.hashedValue = hashedValue;
        this.superUserToken = superUserToken;
        this.description = description;
        this.createdAt = Instant.now();
    }


}
