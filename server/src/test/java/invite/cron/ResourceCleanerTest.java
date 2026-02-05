package invite.cron;

import com.fasterxml.jackson.core.JsonProcessingException;
import invite.AbstractTest;
import invite.model.User;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Instant;
import java.time.Period;
import java.util.List;

import static invite.cron.ResourceCleaner.LOCK_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ResourceCleanerTest extends AbstractTest {

    @Autowired
    private ResourceCleaner subject;

    @Autowired
    private DataSource dataSource;

    @Test
    void cleanUsersWithoutActivity() throws JsonProcessingException {
        long beforeUsers = userRepository.count();
        markUserAsVeryInactive(GUEST_SUB);

        stubForManageProvisioning(List.of("1"));
        //Because there are no RemoteProvisionedGroups
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
        //Because there are no RemoteProvisionedGroups
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

    @SneakyThrows
    @Test
    void lockAlreadyAcquired() {
        Connection conn = dataSource.getConnection();
        subject.tryGetLock(conn, LOCK_NAME);

        long beforeUsers = userRepository.count();
        markUserAsVeryInactive(GUEST_SUB);

        subject.clean();
        //Nothing happened
        assertEquals(beforeUsers, userRepository.count());
    }

    private void markUserAsVeryInactive(String sub) {
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
        user.getUserRoles().forEach(userRole -> {
            userRole.setEndDate(past);
            userRole.setExpiryNotifications(1);
        });
        userRepository.save(user);
    }


}