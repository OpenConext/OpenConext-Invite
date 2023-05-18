package access.model;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.LazyInitializationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication;
import org.springframework.util.StringUtils;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Entity(name = "roles")
@NoArgsConstructor
@Getter
@Setter
@EntityListeners(NameHolderListener.class)
public class Role implements Serializable, NameHolder, RemoteSCIMIdentifier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    @NotNull
    private String name;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "remote_scim_identifier")
    private String remoteScimIdentifier;

    @Enumerated(EnumType.STRING)
    @Column
    @NotNull
    private Authority authority = Authority.INVITER;

    @Column(name = "instant_available")
    private boolean instantAvailable;

    @Column(name = "default_expiry_days")
    private Integer defaultExpiryDays;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Application application;

    @Embedded
    private Auditable auditable = new Auditable();

    public Role(String name, Application application) {
        this.name = name;
        this.application = application;
    }

    public Role(Long id, String roleName) {
        this.id = id;
        this.name = roleName;
    }

    @JsonProperty(value = "application", access = JsonProperty.Access.READ_ONLY)
    public Map<String, Object> getApplicationMap() {
        try {
            Application application = this.getApplication();

            Map<String, Object> applicationMap = new HashMap<>();
            applicationMap.put("id", application.getId());
            applicationMap.put("name", application.getName());
            return applicationMap;
        } catch (LazyInitializationException e) {
            return null;
        }
    }

    @Override
    @JsonIgnore
    public void nameUrnCompatibilityCheck() {
        this.name = compatibleUrnName(this.name);
    }

}
