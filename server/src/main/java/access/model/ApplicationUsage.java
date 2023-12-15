package access.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Entity(name = "application_usages")
@NoArgsConstructor
@Getter
@Setter
public class ApplicationUsage implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "landing_page")
    private String landingPage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id")
    @JsonIgnore
    private Role role;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "application_id")
    private Application application;

    public ApplicationUsage(Application application, String landingPage) {
        this.landingPage = landingPage;
        this.application = application;
    }

    public Application getApplication() {
        application.setLandingPage(this.getLandingPage());
        return application;
    }
}
