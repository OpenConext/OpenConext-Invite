package invite.model;

import invite.provision.scim.GroupURN;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Formula;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

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

    @Column(name = "organization_guid")
    private String organizationGUID;

    @Column(name = "remote_api_user")
    private String remoteApiUser;

    @Column(name = "inviter_display_name")
    private String inviterDisplayName;

    @Formula(value = "(SELECT COUNT(*) FROM user_roles ur WHERE ur.role_id=id)")
    private Long userRoleCount;

    @OneToMany(mappedBy = "role",
            fetch = FetchType.EAGER,
            orphanRemoval = true,
            cascade = CascadeType.ALL)
    private Set<ApplicationUsage> applicationUsages = new HashSet<>();

    @Embedded
    private Auditable auditable = new Auditable();

    @Transient
    private List<Map<String, Object>> applicationMaps;

    public Role(Long id,
                String name,
                String description,
                Long userRoleCount) {
        //Only used after native query and returned for Role overview in the GUI
        this.id = id;
        this.name = name;
        this.description = description;
        this.userRoleCount = userRoleCount;
    }

    public Role(String name,
                String description,
                Set<ApplicationUsage> applicationUsages,
                Integer defaultExpiryDays,
                boolean enforceEmailEquality,
                boolean eduIDOnly) {
        this(name, GroupURN.sanitizeRoleShortName(name), description, applicationUsages,
                defaultExpiryDays, enforceEmailEquality, eduIDOnly, Collections.emptyList());
    }

    public Role(@NotNull String name,
                @NotNull String shortName,
                String description,
                Set<ApplicationUsage> applicationUsages,
                Integer defaultExpiryDays,
                boolean enforceEmailEquality,
                boolean eduIDOnly,
                List<Map<String, Object>> applicationMaps) {
        this.name = name;
        this.shortName = shortName;
        this.description = description;
        this.defaultExpiryDays = defaultExpiryDays;
        this.enforceEmailEquality = enforceEmailEquality;
        this.eduIDOnly = eduIDOnly;
        this.applicationUsages = applicationUsages;
        this.applicationUsages.forEach(applicationUsage -> applicationUsage.setRole(this));
        this.applicationMaps = applicationMaps;
        this.identifier = UUID.randomUUID().toString();
    }

    @Transient
    @JsonIgnore
    public List<String> applicationIdentifiers() {
        return applicationUsages.stream()
                .map(applicationUsage -> applicationUsage.getApplication().getManageId()).toList();
    }

    @Transient
    @JsonIgnore
    public Set<Application> applicationsUsed() {
        return applicationUsages.stream()
                .map(ApplicationUsage::getApplication).collect(Collectors.toSet());
    }

    public void setApplicationUsages(Set<ApplicationUsage> applicationUsages) {
        this.applicationUsages = applicationUsages;
        this.applicationUsages.forEach(applicationUsage -> applicationUsage.setRole(this));
    }

}
