package access.provision;

import access.AbstractTest;
import access.model.*;
import access.provision.scim.OperationType;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

import static access.AbstractTest.GUEST_SUB;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ProvisioningServiceDefaultTest extends AbstractTest {

    @Autowired
    private ProvisioningServiceDefault provisioningService;

    @Test
    void newUserRequest() throws JsonProcessingException {
        User user = userRepository.findBySubIgnoreCase(GUEST_SUB).get();

        this.stubForManageProvisioning(List.of("3"));
        String remoteScimIdentifier = this.stubForCreateEvaUser();
        provisioningService.newUserRequest(user);
        List<RemoteProvisionedUser> remoteProvisionedUsers = remoteProvisionedUserRepository.findAll();
        assertEquals(1, remoteProvisionedUsers.size());
        assertEquals(remoteScimIdentifier, remoteProvisionedUsers.get(0).getRemoteIdentifier());
    }

    @Test
    void deleteUserRequest() throws JsonProcessingException {
        User user = userRepository.findBySubIgnoreCase(GUEST_SUB).get();

        //Mock that this user was provisioned earlier
        RemoteProvisionedUser remoteProvisionedUser = new RemoteProvisionedUser(user, UUID.randomUUID().toString(), "10");
        remoteProvisionedUserRepository.save(remoteProvisionedUser);

        this.stubForManageProvisioning(List.of("3"));
        this.stubForDeleteEvaUser();

        provisioningService.deleteUserRequest(user);
        List<RemoteProvisionedUser> remoteProvisionedUsers = remoteProvisionedUserRepository.findAll();
        assertEquals(0, remoteProvisionedUsers.size());
    }

    @Test
    void deleteGraphUserRequest() throws JsonProcessingException {
        User user = userRepository.findBySubIgnoreCase(GUEST_SUB).get();

        //Mock that this user was provisioned earlier
        RemoteProvisionedUser remoteProvisionedUser = new RemoteProvisionedUser(user, UUID.randomUUID().toString(), "9");
        remoteProvisionedUserRepository.save(remoteProvisionedUser);

        this.stubForManageProvisioning(List.of("2", "6"));
        this.stubForDeleteGraphUser();

        provisioningService.deleteUserRequest(user);
        List<RemoteProvisionedUser> remoteProvisionedUsers = remoteProvisionedUserRepository.findAll();
        assertEquals(0, remoteProvisionedUsers.size());
    }

    @Test
    void updateGroupRequest() {
        //We only provision GUEST users
        provisioningService.updateGroupRequest(new UserRole(Authority.INVITER, null), OperationType.Add);
    }
}