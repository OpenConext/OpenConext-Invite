package invite.manage;

import invite.AbstractTest;
import invite.model.Role;
import invite.provision.ProvisioningType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class LocalManageTest extends AbstractTest {

    @Test
    void deriveRemoteApplications() {
        List<Role> roles = roleRepository.findAll();
        assertEquals(6, roles.size());
        localManage.addManageMetaData(roles);
        roles.forEach(role -> role.getApplicationMaps().forEach(map -> assertNotNull(map.get("id"))));
    }

    @Test
    void provisioningScimTrailingSlash() {
        Map<String, Object> provider = Map.of(
                "type", EntityType.PROVISIONING.collectionName(),
                "_id", "12345678",
                "data", Map.of("metaDataFields", Map.of(
                        "provisioning_type", ProvisioningType.scim.name(),
                        "scim_url", "https://scum.url/",
                        "scim_user", "user",
                        "scim_password", "secret"
                ))
        );
        Map<String, Object> application = localManage.transformProvider(provider);

        assertEquals("12345678", application.get("id"));
        assertEquals("https://scum.url", application.get("scim_url"));
    }
}