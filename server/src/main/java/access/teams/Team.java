package access.teams;

import access.model.Application;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Team implements Serializable {

    private String urn;
    private String name;
    private String description;
    private String landingPage;
    private List<Membership> memberships;
    private List<Application> applications;

}
