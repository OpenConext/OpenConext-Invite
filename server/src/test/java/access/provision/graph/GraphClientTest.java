package access.provision.graph;

import access.exception.RemoteException;
import access.manage.EntityType;
import access.manage.LocalManage;
import access.model.User;
import access.provision.Provisioning;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;

//We test the non-happy paths here and the happy-paths through the controllers
class GraphClientTest {

    final GraphClient graphClient = new GraphClient("http://localhost:8080", "test.eduid.nl");
    final LocalManage localManage = new LocalManage(new ObjectMapper(), false);

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