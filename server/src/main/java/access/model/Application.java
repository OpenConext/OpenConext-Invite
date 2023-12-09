package access.model;


import access.manage.EntityType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@Entity(name = "applications")
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(of = {"manageId", "manageType"})
public class Application implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "manage_id")
    @NotNull
    private String manageId;

    @Column(name = "manage_type")
    @Enumerated(EnumType.STRING)
    @NotNull
    private EntityType manageType;

    @Column(name = "landing_page")
    private String landingPage;

    @ManyToMany(mappedBy = "applications")
    @JsonIgnore
    private Set<Role> roles = new HashSet<>();

    public Application(String manageId, EntityType manageType) {
        this.manageId = manageId;
        this.manageType = manageType;
    }
}
