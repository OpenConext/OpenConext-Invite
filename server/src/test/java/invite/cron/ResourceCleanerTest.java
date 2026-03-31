package invite.cron;

import com.fasterxml.jackson.core.JsonProcessingException;
import invite.AbstractTest;
import invite.model.Invitation;
import invite.model.User;
import invite.model.UserRoleAudit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResourceCleanerTest extends AbstractTest {

    @Autowired
    private ResourceCleaner resourceCleaner;

    @Test
    void cleanUsersWithoutActivity() throws JsonProcessingException {
        long beforeUsers = userRepository.count();
        markUserAsVeryInactive(GUEST_SUB);

        stubForManageProvisioning(List.of("1"));
        //Because there are no RemoteProvisionedGroups
        stubForCreateScimRole();
        stubForCreateScimUser();
        stubForUpdateScimRole();

        resourceCleaner.clean();
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

        resourceCleaner.clean();
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

        resourceCleaner.clean();
        assertEquals(beforeUserRoles, userRoleRepository.count() + 3);
    }

    @Test
    void cleanUserRoleAudits() {
        Instant past = Instant.now().minus(400, ChronoUnit.DAYS);
        seedUserRoleAudits(past);
        assertEquals(4, userRoleAuditRepository.count());

        List<UserRoleAudit> userRoleAudits = this.userRoleAuditRepository.findAll();
        userRoleAudits.forEach(userRoleAudit -> {
            userRoleAudit.setCreatedAt(past);
            userRoleAuditRepository.save(userRoleAudit);
        });

        resourceCleaner.clean();

        assertEquals(0, userRoleAuditRepository.count());
    }

    @Test
    void cleanExpiredInvitations() {
        long count = invitationRepository.count();
        Instant past = Instant.now().minus(400, ChronoUnit.DAYS);
        Invitation invitation = invitationRepository.findByHash(GRAPH_INVITATION_HASH).get();
        invitation.setExpiryDate(past);
        invitationRepository.save(invitation);

        resourceCleaner.clean();

        assertEquals(count - 1, invitationRepository.count());
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