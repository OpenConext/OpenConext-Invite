package invite.model;


import invite.manage.EntityType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    @OneToMany(mappedBy = "application",
            fetch = FetchType.LAZY,
            orphanRemoval = true)
    @JsonIgnore
    private Set<ApplicationUsage> applicationUsages = new HashSet<>();

    @OneToMany(mappedBy = "application",
            fetch = FetchType.LAZY,
            orphanRemoval = true)
    @JsonIgnore
    private Set<UserApplication> userApplications = new HashSet<>();

    @OneToMany(mappedBy = "application",
            fetch = FetchType.LAZY,
            orphanRemoval = true)
    @JsonIgnore
    private Set<InvitationApplication> invitationApplications = new HashSet<>();

    public Application(String manageId, EntityType manageType) {
        this.manageId = manageId;
        this.manageType = manageType;
    }

}
