package invite;

import invite.manage.EntityType;
import invite.model.Application;
import invite.model.ApplicationUsage;

import java.util.Set;

public class WithApplicationTest {

    public Set<ApplicationUsage> application(String manageId, EntityType entityType) {
        Application application = new Application(manageId, entityType);
        ApplicationUsage applicationUsage = new ApplicationUsage(application, "https://landingpage.com");
        application.getApplicationUsages().add(applicationUsage);
        return Set.of(applicationUsage);

    }

}
