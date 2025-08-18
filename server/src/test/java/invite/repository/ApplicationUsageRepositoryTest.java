package invite.repository;

import invite.AbstractTest;
import invite.model.Application;
import invite.model.ApplicationUsage;
import invite.model.Role;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ApplicationUsageRepositoryTest extends AbstractTest {

    @Test
    void findByRoleIdAndApplicationManageIdAndApplicationManageTypeOrderByApplicationId() {
        Role role = roleRepository.findByName("wiki").get();
        ApplicationUsage applicationUsage = role.getApplicationUsages().iterator().next();
        Application application = applicationUsage.getApplication();
        Optional<ApplicationUsage> optionalApplicationUsage = applicationUsageRepository.findByRoleIdAndApplicationManageIdAndApplicationManageTypeOrderByApplicationId(
                role.getId(),
                application.getManageId(),
                application.getManageType()
        );
        assertTrue(optionalApplicationUsage.isPresent());

        optionalApplicationUsage = applicationUsageRepository.findByRoleIdAndApplicationManageIdAndApplicationManageTypeOrderByApplicationId(
                role.getId(),
                UUID.randomUUID().toString(),
                application.getManageType()
        );
        assertTrue(optionalApplicationUsage.isEmpty());

        optionalApplicationUsage = applicationUsageRepository.findByRoleIdAndApplicationManageIdAndApplicationManageTypeOrderByApplicationId(
                null,
                application.getManageId(),
                application.getManageType()
        );
        assertTrue(optionalApplicationUsage.isEmpty());
    }
}