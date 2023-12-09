package access.repository;

import access.AbstractTest;
import access.manage.EntityType;
import access.model.Application;
import access.model.Role;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ApplicationRepositoryTest extends AbstractTest {

    @Test
    void findByManageIdAndManageType() {
        Application application = applicationRepository.findByManageIdAndManageType("2", EntityType.SAML20_SP).get();
        assertEquals("2", application.getManageId());
    }
}