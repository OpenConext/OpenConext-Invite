package access.cron;

import access.AbstractTest;
import access.model.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.Period;
import java.util.List;

import static access.Seed.GUEST_SUB;
import static org.junit.jupiter.api.Assertions.*;

class ResourceCleanerTest extends AbstractTest {

    @Autowired
    private ResourceCleaner subject;

    @Test
    void cleanUsersWithoutActivity() throws JsonProcessingException {
        long beforeUsers = userRepository.count();
        markUser(GUEST_SUB);

        stubForManageProvisioning(List.of("1"));
        //Because there are RemoteProvisionedGroups
        stubForCreateScimRole();
        stubForCreateScimUser();
        stubForUpdateScimRole();

        subject.clean();
        assertEquals(beforeUsers, userRepository.count() + 1);
    }

    @Test
    void cleanUsersWithoutRoles() throws JsonProcessingException {
        long beforeUsers = userRepository.count();
        deleteUserRoles(GUEST_SUB);

        stubForManageProvisioning(List.of("1"));
        //Because there are RemoteProvisionedGroups
        stubForCreateScimRole();
        stubForCreateScimUser();
        stubForUpdateScimRole();

        subject.clean();
        assertEquals(beforeUsers, userRepository.count() + 1);
    }

    @Test
    void cleanUserRoles() throws JsonProcessingException {
        long beforeUserRoles = userRoleRepository.count();
        markUserRole(GUEST_SUB);

        stubForManageProvisioning(List.of("1"));
        //Because there is no RemoteProvisionedGroup
        stubForCreateScimRole();
        stubForCreateScimUser();
        stubForUpdateScimRole();

        subject.clean();
        assertEquals(beforeUserRoles, userRoleRepository.count() + 3);
    }

    @Test
    void notCronJobResponsible() {
        ResourceCleaner resourceCleaner = new ResourceCleaner(null, null, null, 1, false);
        resourceCleaner.clean();
    }

    private void markUser(String sub) {
        User user = userRepository.findBySubIgnoreCase(sub).get();
        Instant past = Instant.now().minus(Period.ofDays(1050));
        user.setLastActivity(past);
        user.getUserRoles().forEach(userRole -> userRole.setEndDate(past));
        userRepository.save(user);
    }

    private void deleteUserRoles(String sub) {
        User user = userRepository.findBySubIgnoreCase(sub).get();
        user.getUserRoles().clear();
        userRepository.save(user);
    }

    private void markUserRole(String sub) {
        User user = userRepository.findBySubIgnoreCase(sub).get();
        Instant past = Instant.now().minus(Period.ofDays(1050));
        user.getUserRoles().forEach(userRole -> userRole.setEndDate(past));
        userRepository.save(user);
    }


}