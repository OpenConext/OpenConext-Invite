package access.provision;

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
    }

    @Test
    void provisioningEva() {
        this.assertInvariant(this.provisioningMap(ProvisioningType.eva));
        this.assertInvariant(this.provisioningMap(ProvisioningType.eva, "eva_url"));
        Provisioning provisioning = new Provisioning(this.provisioningMap(ProvisioningType.eva, "eva_url", "eva_token"));
        assertEquals(30, provisioning.getEvaGuestAccountDuration());
    }

    @Test
    void provisioningGraph() {
        this.assertInvariant(this.provisioningMap(ProvisioningType.graph));
        this.assertInvariant(this.provisioningMap(ProvisioningType.graph, "graph_url"));
        this.provisioningMap(ProvisioningType.graph, "graph_url", "graph_token");
    }

    private void assertInvariant(Map<String, Object> provider) {
        assertThrows(AssertionError.class, () -> new Provisioning(provider));
    }

    private Map<String, Object> provisioningMap(ProvisioningType type, String... snakeCaseAttributes) {
        HashMap<Object, Object> metaDataFields = new HashMap<>();
        metaDataFields.put("provisioning_type", type.name());
        Stream.of(snakeCaseAttributes).forEach(attr -> metaDataFields.put(attr, attr));
        return Map.of("data", Map.of("metaDataFields", metaDataFields));
    }
}