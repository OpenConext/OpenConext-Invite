package invite.provision;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProvisioningTest {

    @Test
    void provisioningScim() {
        this.assertInvariant(this.provisioningMap(ProvisioningType.scim));
        this.assertInvariant(this.provisioningMap(ProvisioningType.scim, "scim_url"));
        this.assertInvariant(this.provisioningMap(ProvisioningType.scim, "scim_url", "scim_user"));
        new Provisioning(this.provisioningMap(ProvisioningType.scim, "scim_url", "scim_user", "scim_password"));
        new Provisioning(this.provisioningMap(ProvisioningType.scim, "scim_url", "scim_bearer_token"));
    }

    @Test
    void provisioningEva() {
        this.assertInvariant(this.provisioningMap(ProvisioningType.eva));
        this.assertInvariant(this.provisioningMap(ProvisioningType.eva, "eva_url"));
        Provisioning provisioning = new Provisioning(this.provisioningMap(ProvisioningType.eva, "eva_url", "eva_token"));
        assertEquals("eva_token", provisioning.getEvaToken());
    }

    @Test
    void provisioningGraph() {
        this.assertInvariant(this.provisioningMap(ProvisioningType.graph));
        this.assertInvariant(this.provisioningMap(ProvisioningType.graph, "graph_client_id"));
        this.provisioningMap(ProvisioningType.graph, "graph_client_id", "graph_secret");
    }

    private void assertInvariant(Map<String, Object> provider) {
        assertThrows(AssertionError.class, () -> new Provisioning(provider));
    }

    private Map<String, Object> provisioningMap(ProvisioningType type, String... snakeCaseAttributes) {
        HashMap<String, Object> data = new HashMap<>();
        data.put("provisioning_type", type.name());
        Stream.of(snakeCaseAttributes).forEach(attr -> data.put(attr, attr));
        return data;
    }
}