package access.model;


import access.manage.EntityType;
import access.provision.scim.GroupURN;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.Formula;

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

    @Column(name = "enforce_email_equality")
    private boolean enforceEmailEquality;

    @Column(name = "edu_id_only")
    private boolean eduIDOnly;

    @Column(name = "manage_type")
    @Enumerated(EnumType.STRING)
    private EntityType manageType;

    @Column(name = "block_expiry_date")
    private boolean blockExpiryDate;

    @Column(name = "override_settings_allowed")
    private boolean overrideSettingsAllowed;

    @Formula(value = "(SELECT COUNT(*) FROM user_roles ur WHERE ur.role_id=id)")
    private Long userRoleCount;

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
                Integer defaultExpiryDays,
                boolean enforceEmailEquality,
                boolean eduIDOnly) {
        this(name, GroupURN.sanitizeRoleShortName(name), description, landingPage, manageId, manageType, defaultExpiryDays,
                enforceEmailEquality, eduIDOnly,
                Collections.emptyMap());
    }

    public Role(@NotNull String name,
                @NotNull String shortName,
                String description,
                String landingPage,
                String manageId,
                EntityType manageType,
                Integer defaultExpiryDays,
                boolean enforceEmailEquality,
                boolean eduIDOnly,
                Map<String, Object> application) {
        this.name = name;
        this.shortName = shortName;
        this.description = description;
        this.landingPage = landingPage;
        this.manageId = manageId;
        this.manageType = manageType;
        this.defaultExpiryDays = defaultExpiryDays;
        this.enforceEmailEquality = enforceEmailEquality;
        this.eduIDOnly = eduIDOnly;
        this.application = application;
    }

}
