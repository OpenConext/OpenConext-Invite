package access.repository;

import access.AbstractTest;
import access.model.Application;
import access.model.ApplicationUsage;
import access.model.Role;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ApplicationUsageRepositoryTest extends AbstractTest {

    @Test
    void findByRoleIdAndApplicationManageIdAndApplicationManageType() {
        Role role = roleRepository.findByName("wiki").get();
        ApplicationUsage applicationUsage = role.getApplicationUsages().iterator().next();
        Application application = applicationUsage.getApplication();
        Optional<ApplicationUsage> optionalApplicationUsage = applicationUsageRepository.findByRoleIdAndApplicationManageIdAndApplicationManageType(
                role.getId(),
                application.getManageId(),
                application.getManageType()
        );
        assertTrue(optionalApplicationUsage.isPresent());

        optionalApplicationUsage = applicationUsageRepository.findByRoleIdAndApplicationManageIdAndApplicationManageType(
                role.getId(),
                UUID.randomUUID().toString(),
                application.getManageType()
        );
        assertTrue(optionalApplicationUsage.isEmpty());

        optionalApplicationUsage = applicationUsageRepository.findByRoleIdAndApplicationManageIdAndApplicationManageType(
                null,
                application.getManageId(),
                application.getManageType()
        );
        assertTrue(optionalApplicationUsage.isEmpty());
    }
}