package access.model;


import access.manage.EntityType;
import access.provision.scim.GroupURN;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.util.*;

@Entity(name = "roles")
@NoArgsConstructor
@Getter
@Setter
public class Role implements Serializable, Provisionable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    @NotNull
    private String name;

    @Column(name = "short_name")
    @NotNull
    private String shortName;

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

    @OneToMany(mappedBy = "role",
            orphanRemoval = true,
            fetch = FetchType.LAZY,
            cascade = CascadeType.ALL)
    @JsonIgnore
    private Set<RemoteProvisionedGroup> remoteProvisionedGroups = new HashSet<>();

    @Embedded
    private Auditable auditable = new Auditable();

    @Transient
    private Map<String, Object> application;

    public Role(String name,
                String description,
                String landingPage,
                String manageId,
                EntityType manageType,
                Integer defaultExpiryDays) {
        this(name, GroupURN.sanitizeRoleShortName(name), description, landingPage, manageId, manageType, defaultExpiryDays, Collections.emptyMap());
    }

    public Role(@NotNull String name,
                @NotNull String shortName,
                String description,
                String landingPage,
                String manageId,
                EntityType manageType,
                Integer defaultExpiryDays,
                Map<String, Object> application) {
        this.name = name;
        this.shortName = shortName;
        this.description = description;
        this.landingPage = landingPage;
        this.manageId = manageId;
        this.manageType = manageType;
        this.defaultExpiryDays = defaultExpiryDays == null ? 365 : defaultExpiryDays;
        this.application = application;
    }

}
