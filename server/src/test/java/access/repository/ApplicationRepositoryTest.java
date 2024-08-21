package access.repository;

import access.AbstractTest;
import access.manage.EntityType;
import access.model.Application;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApplicationRepositoryTest extends AbstractTest {

    @Test
    void findByManageIdAndManageType() {
        Application application = applicationRepository.findByManageIdAndManageType("2", EntityType.SAML20_SP).get();
        assertEquals("2", application.getManageId());
    }
}