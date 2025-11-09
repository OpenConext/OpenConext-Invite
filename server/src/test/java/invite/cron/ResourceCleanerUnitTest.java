package invite.cron;

import com.fasterxml.jackson.core.JsonProcessingException;
import invite.model.Authority;
import invite.model.Role;
import invite.model.User;
import invite.model.UserRole;
import invite.provision.ProvisioningService;
import invite.provision.scim.OperationType;
import invite.repository.UserRepository;
import invite.repository.UserRoleRepository;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.time.Instant;
import java.time.Period;
import java.util.List;
import java.util.Map;

import static invite.AbstractTest.GUEST_SUB;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

class ResourceCleanerUnitTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserRoleRepository userRoleRepository = mock(UserRoleRepository.class);
    private final ProvisioningService provisioningService = mock(ProvisioningService.class);

    private final ResourceCleaner subject = new ResourceCleaner(userRepository,
            userRoleRepository,
            provisioningService,
            5,
            true);


    @Test
    void cleanUsers() {
        List<User> users = List.of(new User(), new User());
        when(userRepository.findByLastActivityBefore(any(Instant.class)))
                .thenReturn(users);
        when(userRepository.findNonSuperUserWithoutUserRoles())
                .thenReturn(users);
        when(userRoleRepository.findByEndDateBeforeAndExpiryNotifications(any(Instant.class),eq(1)))
                .thenReturn(List.of(new UserRole("Inviter", new User(), new Role(), Authority.INVITER)));

        doThrow(new RuntimeException())
                .when(provisioningService)
                .deleteUserRequest(any(User.class));
        doThrow(new RuntimeException())
                .when(provisioningService)
                .updateGroupRequest(any(UserRole.class), eq(OperationType.Remove));

        Map<String, List<? extends Serializable>> results = subject.doClean();
        assertEquals(2, results.get("DeletedNonActiveUsers").size());
        assertEquals(2, results.get("DeletedOrphanUsers").size());
        assertEquals(1, results.get("DeletedExpiredUserRoles").size());
    }

    @Test
    void cleanUsersWithoutRoles() {
    }

    @Test
    void cleanUserRoles() {
    }


}