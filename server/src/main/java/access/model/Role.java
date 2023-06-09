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
import java.util.*;

@Entity(name = "roles")
@NoArgsConstructor
@Getter
@Setter
public class Role implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    @NotNull
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "landing_page")
    private String landingPage;

    @Column(name = "default_expiry_days")
    private Integer defaultExpiryDays;

    @Column(name = "manage_id")
    private String manageId;

    @Column(name = "manage_type")
    @Enumerated(EnumType.STRING)
    private EntityType manageType;

    @OneToMany(mappedBy = "role", orphanRemoval = true, fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<RemoteProvisionedGroup> remoteProvisionedGroups = new HashSet<>();

    @OneToMany(mappedBy = "role", orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<UserRole> userRoles = new HashSet<>();

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

}
