package invite;

import invite.manage.EntityType;
import invite.model.Application;
import invite.model.ApplicationUsage;

import java.util.Set;

public class WithApplicationTest {

    public Set<ApplicationUsage> application(String manageId, EntityType entityType) {
        return Set.of(new ApplicationUsage(new Application(manageId, entityType), "https://landingpage.com"));
    }

}
