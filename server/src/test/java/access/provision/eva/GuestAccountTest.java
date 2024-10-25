package access.provision.eva;

import access.model.Role;
import access.model.User;
import access.provision.Provisioning;
import access.provision.ProvisioningType;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;

class GuestAccountTest {

    @Test
    void getRequest() {
        Map<String, Object> map = new HashMap<>();
        map.put("provisioning_type", ProvisioningType.graph.name());
        map.put("graph_url", "https://graph");
        map.put("graph_client_id", "client");
        map.put("graph_secret", "secret");

        assertThrows(AssertionError.class, () -> new GuestAccount(new User(), new Provisioning(map)));
    }

    @Test
    void noUserRoles() {
        Map<String, Object> map = new HashMap<>();
        map.put("provisioning_type", ProvisioningType.eva.name());
        map.put("eva_token", "secret");
        map.put("eva_url", "https://eva");
        map.put("graph_secret", "secret");

        assertThrows(AssertionError.class, () -> new GuestAccount(new User(), new Provisioning(map)));
    }

}