package access.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.LazyInitializationException;
import org.springframework.util.StringUtils;


import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Entity(name = "applications")
@NoArgsConstructor
@Getter
@Setter
@EntityListeners(NameHolderListener.class)
public class Application implements Serializable, NameHolder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "manage_id")
    @NotNull
    private String manageId;

    @Column(name = "name")
    @NotNull
    private String name;

    @OneToMany(mappedBy = "application", orphanRemoval = true, fetch = FetchType.EAGER)
    private Set<Role> roles = new HashSet<>();

    @Embedded
    private Auditable auditable = new Auditable();

    public Application(String manageId, String name) {
        this.manageId = manageId;
        this.name = name;
    }

    @JsonIgnore
    public void addRole(Role role) {
        this.roles.add(role);
        role.setApplication(this);
    }


    @Override
    public void nameUrnCompatibilityCheck() {
        this.name = compatibleUrnName(this.name);
    }
}
