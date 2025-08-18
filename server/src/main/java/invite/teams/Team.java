package invite.teams;

import invite.model.Application;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Team implements Serializable {

    private String urn;
    private String name;
    private String description;
    private List<Membership> memberships;
    private List<Application> applications = new ArrayList<>();

}
