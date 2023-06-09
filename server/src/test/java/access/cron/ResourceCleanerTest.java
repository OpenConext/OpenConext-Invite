package access.cron;

import access.AbstractTest;
import access.model.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.Period;
import java.util.List;

import static access.Seed.guestSub;
import static org.junit.jupiter.api.Assertions.*;

class ResourceCleanerTest extends AbstractTest {

    @Autowired
    private ResourceCleaner subject;

    @Test
    void cleanUsers() throws JsonProcessingException {
        long beforeUsers = userRepository.count();
        markUser();

        stubForProvisioning(List.of("1"));
        //Because there is no RemoteProvisionedGroup
        stubForCreateRole();
        stubForCreateUser();
        stubForUpdateRole();

        subject.clean();
        assertEquals(beforeUsers, userRepository.count() + 1);
    }

    @Test
    void cleanUserRoles() throws JsonProcessingException {
        long beforeUserRoles = userRoleRepository.count();
        markUserRole();

        stubForProvisioning(List.of("1"));
        //Because there is no RemoteProvisionedGroup
        stubForCreateRole();
        stubForCreateUser();
        stubForUpdateRole();

        subject.clean();
        assertEquals(beforeUserRoles, userRoleRepository.count() + 1);
    }

    @Test
    void notCronJobResponsible() {
        ResourceCleaner resourceCleaner = new ResourceCleaner(null, null, null, 1, false);
        resourceCleaner.clean();
    }

    private void markUser() {
        User user = userRepository.findBySubIgnoreCase(guestSub).get();
        Instant past = Instant.now().minus(Period.ofDays(1050));
        user.setLastActivity(past);
        user.getUserRoles().forEach(userRole -> userRole.setEndDate(past));
        userRepository.save(user);
    }

    private void markUserRole() {
        User user = userRepository.findBySubIgnoreCase(guestSub).get();
        Instant past = Instant.now().minus(Period.ofDays(1050));
        user.getUserRoles().forEach(userRole -> userRole.setEndDate(past));
        userRepository.save(user);
    }


}