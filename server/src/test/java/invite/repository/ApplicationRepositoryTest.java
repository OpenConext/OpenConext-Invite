package invite.repository;

import invite.AbstractTest;
import invite.manage.EntityType;
import invite.model.Application;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApplicationRepositoryTest extends AbstractTest {

    @Test
    void findByManageIdAndManageTypeOrderById() {
        Application application = applicationRepository.findByManageIdAndManageTypeOrderById("2", EntityType.SAML20_SP).get();
        assertEquals("2", application.getManageId());
    }

    @Test
    void countByApplications() {
        List<Map<String, Object>> applications = applicationRepository.countByApplications();
        Map<String, Object> application = applications.stream()
                .filter(m -> m.get("manage_id").equals("5"))
                .findFirst()
                .get();
        assertEquals(2L, application.get("role_count"));
    }

}