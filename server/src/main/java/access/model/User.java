package access.model;

import access.manage.ManageIdentifier;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Entity(name = "users")
@NoArgsConstructor
@Getter
@Setter
public class User implements Serializable, Provisionable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    @NotNull
    private String sub;

    @Column(name = "super_user")
    @NotNull
    private boolean superUser;

    @Column(name = "eduperson_principal_name")
    @NotNull
    private String eduPersonPrincipalName;

    @Column(name = "given_name")
    private String givenName;

    @Column(name = "family_name")
    private String familyName;

    @Column(name = "schac_home_organization")
    private String schacHomeOrganization;

    @Column
    private String email;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "last_activity")
    private Instant lastActivity;

    @OneToMany(mappedBy = "user", orphanRemoval = true, fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Set<UserRole> userRoles = new HashSet<>();

    @OneToMany(mappedBy = "user", orphanRemoval = true, fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JsonIgnore
    private Set<RemoteProvisionedUser> remoteProvisionedUsers = new HashSet<>();

    @Transient
    private List<Map<String, Object>> providers;

    public User(Map<String, Object> attributes) {
        this(false, attributes);
    }

    public User(boolean superUser, Map<String, Object> attributes) {
        this(superUser,
                (String) attributes.get("eduperson_principal_name"),
                (String) attributes.get("sub"),
                (String) attributes.get("schac_home_organization"),
                (String) attributes.get("given_name"),
                (String) attributes.get("family_name"),
                (String) attributes.get("email"));
    }

    public User(boolean superUser, String eppn, String sub, String schacHomeOrganization, String givenName, String familyName, String email) {
        this.superUser = superUser;
        this.eduPersonPrincipalName = eppn;
        this.sub = sub;
        this.schacHomeOrganization = schacHomeOrganization;
        this.givenName = givenName;
        this.familyName = familyName;
        this.email = email;
        this.createdAt = Instant.now();
        this.lastActivity = Instant.now();
    }

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getName() {
        return String.format("%s %s", givenName, familyName);
    }


    @JsonIgnore
    public void addUserRole(UserRole userRole) {
        this.userRoles.add(userRole);
        userRole.setUser(this);
    }

    @JsonIgnore
    public void removeUserRole(UserRole role) {
        //This is required by Hibernate - children can't be de-referenced
        Set<UserRole> newRoles = userRoles.stream().filter(ur -> !ur.getId().equals(role.getId())).collect(Collectors.toSet());
        userRoles.clear();
        userRoles.addAll(newRoles);
    }

    @JsonIgnore
    public Set<ManageIdentifier> manageIdentifierSet() {
        return userRoles.stream()
                .filter(userRole -> userRole.getAuthority().equals(Authority.GUEST))
                .map(userRole -> new ManageIdentifier(userRole.getRole().getManageId(), userRole.getRole().getManageType()))
                .collect(Collectors.toSet());
    }

    @JsonIgnore
    public Map<String, Object> asMap() {
        return Map.of(
                "id", id,
                "name", getName(),
                "email", email,
                "createdAt", createdAt,
                "lastActivity", lastActivity,
                "schacHomeOrganization", schacHomeOrganization,
                "sub", sub
        );
    }

    @JsonIgnore
    public void updateAttributes(Map<String, Object> attributes) {
        this.eduPersonPrincipalName = (String) attributes.get("eduperson_principal_name");
        this.schacHomeOrganization = (String) attributes.get("schac_home_organization");
        this.givenName = (String) attributes.get("given_name");
        this.familyName = (String) attributes.get("family_name");
        this.email = (String) attributes.get("email");
        this.lastActivity = Instant.now();
    }

}
