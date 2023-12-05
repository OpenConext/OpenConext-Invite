package access;

import access.manage.EntityType;
import access.model.Application;

import java.util.Set;

public class WithApplicationTest {

    public Set<Application> application(String manageId, EntityType entityType) {
        return Set.of(new Application(manageId, entityType));
    }

}
