package access.model;


import access.config.ApplicationConverter;
import access.provision.scim.GroupURN;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
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

    @Column(name = "urn")
    private String urn;

    @Column(name = "default_expiry_days")
    private Integer defaultExpiryDays;

    @Column(name = "enforce_email_equality")
    private boolean enforceEmailEquality;

    @Column(name = "edu_id_only")
    private boolean eduIDOnly;

    @Column(name = "block_expiry_date")
    private boolean blockExpiryDate;

    @Column(name = "override_settings_allowed")
    private boolean overrideSettingsAllowed;

    @Column(name = "teams_origin")
    private boolean teamsOrigin;


    @Column(name = "identifier")
    private String identifier;

    @Formula(value = "(SELECT COUNT(*) FROM user_roles ur WHERE ur.role_id=id)")
    private Long userRoleCount;

    @Column
    @Convert(converter = ApplicationConverter.class)
    private Set<Application> applications = new HashSet<>();

    @OneToMany(mappedBy = "role",
            orphanRemoval = true,
            fetch = FetchType.LAZY,
            cascade = CascadeType.ALL)
    @JsonIgnore
    private Set<RemoteProvisionedGroup> remoteProvisionedGroups = new HashSet<>();

    @Embedded
    private Auditable auditable = new Auditable();

    @Transient
    private List<Map<String, Object>> applicationMaps;

    public Role(String name,
                String description,
                String landingPage,
                Set<Application> applications,
                Integer defaultExpiryDays,
                boolean enforceEmailEquality,
                boolean eduIDOnly) {
        this(name, GroupURN.sanitizeRoleShortName(name), description, landingPage, applications,
                defaultExpiryDays, enforceEmailEquality, eduIDOnly, Collections.emptyList());
    }

    public Role(@NotNull String name,
                @NotNull String shortName,
                String description,
                String landingPage,
                Set<Application> applications,
                Integer defaultExpiryDays,
                boolean enforceEmailEquality,
                boolean eduIDOnly,
                List<Map<String, Object>> applicationMaps) {
        this.name = name;
        this.shortName = shortName;
        this.description = description;
        this.landingPage = landingPage;
        this.defaultExpiryDays = defaultExpiryDays;
        this.enforceEmailEquality = enforceEmailEquality;
        this.eduIDOnly = eduIDOnly;
        this.applications = applications;
        this.applicationMaps = applicationMaps;
        this.identifier = UUID.randomUUID().toString();
    }

    @Transient
    @JsonIgnore
    public List<String> applicationIdentifiers() {
        return applications.stream().map(Application::getManageId).toList();
    }

}
