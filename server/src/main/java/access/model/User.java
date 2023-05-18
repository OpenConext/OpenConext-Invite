package access.model;

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
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Entity(name = "users")
@NoArgsConstructor
@Getter
@Setter
public class User implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "super_user")
    @NotNull
    private boolean superUser;

    @Column
    @NotNull
    private String sub;

    @Column(name = "eduperson_principal_name")
    @NotNull
    private String eduPersonPrincipalName;

    @Column(name = "given_name")
    private String givenName;

    @Column(name = "family_name")
    private String familyName;

    @Column
    private String email;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "last_activity")
    private Instant lastActivity = Instant.now();

    @OneToMany(mappedBy = "user", orphanRemoval = true, fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Set<UserRole> userRoles = new HashSet<>();

    public User(Map<String, Object> attributes) {
        this(false, attributes);
    }

    public User(boolean superUser, Map<String, Object> attributes) {
        this(superUser,
                (String) attributes.get("eduperson_principal_name"),
                (String) attributes.get("sub"),
                (String) attributes.get("given_name"),
                (String) attributes.get("family_name"),
                (String) attributes.get("email"));
    }

    public User(boolean superUser, String eppn, String sub, String givenName, String familyName, String email) {
        this.superUser = superUser;
        this.eduPersonPrincipalName = eppn;
        this.sub = sub;
        this.givenName = givenName;
        this.familyName = familyName;
        this.email = email;
        this.createdAt = Instant.now();
    }

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getName() {
        return String.format("%s %s", givenName, familyName);
    }

    @JsonIgnore
    public void addUserRole(UserRole role) {
        this.userRoles.add(role);
        role.setUser(this);
    }

    @JsonIgnore
    public void removeUserRole(UserRole role) {
        //This is required by Hibernate - children can't be de-referenced
        Set<UserRole> newRoles = userRoles.stream().filter(ur -> !ur.getId().equals(role.getId())).collect(Collectors.toSet());
        userRoles.clear();
        userRoles.addAll(newRoles);
    }


    @JsonIgnore
    public boolean hasChanged(Map<String, Object> tokenAttributes) {
        User user = new User(tokenAttributes);
        boolean changed = !this.toScimString().equals(user.toScimString());
        if (changed) {
            this.familyName = user.familyName;
            this.givenName = user.givenName;
            this.email = user.email;
        }
        return changed;
    }

    private String toScimString() {
        return String.format("%s %s <%s>",
                this.givenName,
                this.familyName,
                this.email);

    }

}
