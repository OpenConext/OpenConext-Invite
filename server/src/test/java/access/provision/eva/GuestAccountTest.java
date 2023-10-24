package access.provision.eva;

import access.model.User;
import access.provision.Provisioning;
import access.provision.ProvisioningType;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

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
}