package access.manage;

import access.AbstractTest;
import access.model.Role;
import org.junit.jupiter.api.Test;

import java.util.List;

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
}