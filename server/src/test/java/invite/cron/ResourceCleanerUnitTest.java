package invite.cron;

import invite.audit.UserRoleAuditService;
import invite.model.Authority;
import invite.model.Role;
import invite.model.User;
import invite.model.UserRole;
import invite.provision.ProvisioningService;
import invite.provision.scim.OperationType;
import invite.repository.InvitationRepository;
import invite.repository.UserRepository;
import invite.repository.UserRoleAuditRepository;
import invite.repository.UserRoleRepository;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
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
    private final InvitationRepository invitationRepository = mock(InvitationRepository.class);
    private final UserRoleAuditRepository userRoleAuditRepository = mock(UserRoleAuditRepository.class);
    private final ProvisioningService provisioningService = mock(ProvisioningService.class);
    private final UserRoleAuditService userRoleAuditService = new UserRoleAuditService(userRoleAuditRepository);
    private final DataSource dataSource = mock(DataSource.class);

    private final ResourceCleaner subject = new ResourceCleaner(
            userRepository,
            userRoleRepository,
            provisioningService,
            dataSource,
            userRoleAuditRepository,
            userRoleAuditService,
            invitationRepository,
            5,
            5,
            5);


    @SneakyThrows
    @Test
    @SuppressWarnings("raw")
    void cleanUsers() {
        List<User> users = List.of(new User(), new User());
        when(userRepository.findByLastActivityBefore(any(Instant.class)))
                .thenReturn(users);
        when(userRepository.findNonSuperUserWithoutUserRoles())
                .thenReturn(users);
        when(userRoleRepository.findByEndDateBeforeAndExpiryNotifications(any(Instant.class), eq(1)))
                .thenReturn(List.of(new UserRole("Inviter", new User(), new Role(), Authority.INVITER)));
        when(userRoleAuditRepository.deleteByCreatedAtBefore(any(Instant.class))).thenReturn(5);
        when(invitationRepository.deleteByExpiryDateBefore(any(Instant.class))).thenReturn(3);
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
                .updateGroupRequest(any(UserRole.class), eq(OperationType.remove));

        Map<String, Object> results = subject.doClean();
        assertEquals(2, ((List<?>) results.get("DeletedNonActiveUsers")).size());
        assertEquals(2, ((List<?>) results.get("DeletedOrphanUsers")).size());
        assertEquals(1, ((List<?>) results.get("DeletedExpiredUserRoles")).size());
        assertEquals(5, (int) results.get("DeletedUserRoleAudits"));
        assertEquals(3, (int) results.get("DeletedExpiredInvitations"));
    }

}