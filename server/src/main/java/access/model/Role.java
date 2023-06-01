package access.model;


import access.manage.EntityType;
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
import java.util.Collections;
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

    @Column(name = "description")
    private String description;

    @Column(name = "remote_scim_identifier")
    private String remoteScimIdentifier;

    @Column(name = "landing_page")
    private String landingPage;

    @Column(name = "default_expiry_days")
    private Integer defaultExpiryDays;

    @Column(name = "manage_id")
    private String manageId;

    @Column(name = "manage_type")
    @Enumerated(EnumType.STRING)
    private EntityType manageType;

    @Embedded
    private Auditable auditable = new Auditable();

    @Transient
    private Map<String, Object> application;

    public Role(String name, String description, String manageId, EntityType manageType) {
        this(name, description, manageId, manageType, Collections.emptyMap());
    }

    public Role(String name, String description, String manageId, EntityType manageType, Map<String, Object> application) {
        this.name = name;
        this.description = description;
        this.manageId = manageId;
        this.manageType = manageType;
        this.application = application;
    }

    @Override
    @JsonIgnore
    public void nameUrnCompatibilityCheck() {
        this.name = compatibleUrnName(this.name);
    }

}
