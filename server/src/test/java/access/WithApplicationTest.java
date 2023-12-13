package access;

import access.manage.EntityType;
import access.model.Application;
import access.model.ApplicationUsage;

import java.util.Set;

public class WithApplicationTest {

    public Set<ApplicationUsage> application(String manageId, EntityType entityType) {
        return Set.of(new ApplicationUsage(new Application(manageId, entityType), "https://landingpage.com"));
    }

}
