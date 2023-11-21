package access.model;


import access.manage.EntityType;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;


import java.io.Serializable;
import java.util.Set;

@Entity(name = "applications")
@NoArgsConstructor
@Getter
@Setter
public class Application implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "manage_id")
    private String manageId;

    @Column(name = "manage_type")
    @Enumerated(EnumType.STRING)
    private EntityType manageType;

    @ManyToMany(mappedBy = "applications")
    private Set<Role> roles;

    public Application(String manageId, EntityType manageType) {
        this.manageId = manageId;
        this.manageType = manageType;
    }
}
