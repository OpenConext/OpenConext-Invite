package invite.model;

import invite.manage.EntityType;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApplicationTest {

    @Test
    void testEquals() {
        Application application = new Application("1", EntityType.SAML20_SP);
        Application otherApplication = new Application("1", EntityType.SAML20_SP);
        assertEquals(application, otherApplication);

        Set<Application> applications = new HashSet<>(List.of(application, otherApplication));
        assertEquals(1, applications.size());

        assertEquals(0, application.getApplicationUsages().size());
    }
}