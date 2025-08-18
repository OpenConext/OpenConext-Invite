package invite.provision.graph;

import invite.config.ObjectMapperHolder;
import invite.exception.RemoteException;
import invite.manage.EntityType;
import invite.manage.LocalManage;
import invite.model.User;
import invite.provision.Provisioning;
import com.fasterxml.jackson.databind.ObjectMapper;
import crypto.RSAKeyStore;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;

//We test the non-happy paths here and the happy-paths through the controllers
class GraphClientTest {

    final GraphClient graphClient = new GraphClient("http://localhost:8888",
            "test.eduid.nl",
            new RSAKeyStore(),
            new ObjectMapper());
    final LocalManage localManage = new LocalManage(ObjectMapperHolder.objectMapper);

    @Test
    void newUserRequest() {
        Provisioning provisioning = getProvisioning();
        assertThrows(RemoteException.class, () -> graphClient.newUserRequest(provisioning, new User()));
    }

    @Test
    void updateUserRequest() {
        Provisioning provisioning = getProvisioning();
        assertThrows(RemoteException.class, () -> graphClient.updateUserRequest(new User(), provisioning, "remote_id"));
    }

    @Test
    void deleteUserRequest() {
        Provisioning provisioning = getProvisioning();
        assertThrows(RemoteException.class, () -> graphClient.deleteUser(new User(), provisioning, "remote_id"));
    }

    private Provisioning getProvisioning() {
        Map<String, Object> provisioningData = localManage.providerById(EntityType.PROVISIONING, "9");
        return new Provisioning(provisioningData);
    }

}