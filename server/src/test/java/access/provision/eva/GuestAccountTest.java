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
        HashMap<Object, Object> metaDataFields = new HashMap<>();
        metaDataFields.put("provisioning_type", ProvisioningType.graph.name());
        metaDataFields.put("graph_url", "https://graph");
        metaDataFields.put("graph_client_id", "client");
        metaDataFields.put("graph_secret", "secret");
        Map<String, Object> data = Map.of("data", Map.of("metaDataFields", metaDataFields));

        assertThrows(AssertionError.class, () -> new GuestAccount(new User(), new Provisioning(data)));
    }
}