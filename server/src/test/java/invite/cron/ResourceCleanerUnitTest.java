package invite.cron;

import invite.model.Authority;
import invite.model.Role;
import invite.model.User;
import invite.model.UserRole;
import invite.provision.ProvisioningService;
import invite.provision.scim.OperationType;
import invite.repository.UserRepository;
import invite.repository.UserRoleRepository;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ResourceCleanerUnitTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserRoleRepository userRoleRepository = mock(UserRoleRepository.class);
    private final ProvisioningService provisioningService = mock(ProvisioningService.class);
    private final DataSource dataSource = mock(DataSource.class);

    private final ResourceCleaner subject = new ResourceCleaner(
            userRepository,
            userRoleRepository,
            provisioningService,
            dataSource,
            5);


    @SneakyThrows
    @Test
    void cleanUsers() {
        List<User> users = List.of(new User(), new User());
        when(userRepository.findByLastActivityBefore(any(Instant.class)))
                .thenReturn(users);
        when(userRepository.findNonSuperUserWithoutUserRoles())
                .thenReturn(users);
        when(userRoleRepository.findByEndDateBeforeAndExpiryNotifications(any(Instant.class), eq(1)))
                .thenReturn(List.of(new UserRole("Inviter", new User(), new Role(), Authority.INVITER)));
        doNothing().when(provisioningService).deleteUserRequest(any(User.class));
        doNothing().when(provisioningService).updateGroupRequest(any(UserRole.class), any(OperationType.class));

        Connection conn = mock(Connection.class);
        PreparedStatement psGetLock = mock(PreparedStatement.class);
        PreparedStatement psReleaseLock = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(conn);
        when(conn.prepareStatement("SELECT GET_LOCK(?, ?)"))
                .thenReturn(psGetLock);
        when(psGetLock.executeQuery()).thenReturn(rs);
        when(conn.prepareStatement("SELECT RELEASE_LOCK(?)"))
                .thenReturn(psReleaseLock);
        when(dataSource.getConnection()).thenReturn(conn);

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

}