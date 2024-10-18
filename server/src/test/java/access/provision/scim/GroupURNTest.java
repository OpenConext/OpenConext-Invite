package access.provision.scim;

import access.model.Role;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GroupURNTest {

    @Test
    void sanitizeRoleShortName() {
        assertEquals("expected_short_name_yep",
                GroupURN.sanitizeRoleShortName("expected   SHORT  name &&^*&%&^* yep"));
    }

    @Test
    void urnFromRole() {
        String identifier = UUID.randomUUID().toString();
        Role role = new Role();
        role.setShortName(GroupURN.sanitizeRoleShortName("expected   SHORT  name &&^*&%&^* yep"));
        role.setIdentifier(identifier);
        String urn = GroupURN.urnFromRole("prefix", role);
        assertEquals(String.format("prefix:%s:expected_short_name_yep", identifier), urn);
    }
}