package access.model;

import access.manage.EntityType;
import access.manage.ManageIdentifier;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class RoleTest {

    @Test
    void groupBy() {
        List<Role> roles = List.of(
          new Role("cloud", "cloud","1", EntityType.OIDC10_RP),
                new Role("mail", "mail","1", EntityType.OIDC10_RP),
                new Role("wiki", "wiki","2", EntityType.SAML20_SP)
        );
        Map<ManageIdentifier, List<Role>> rolesByApplication = roles.stream()
                .collect(Collectors.groupingBy(role -> new ManageIdentifier(role.getManageId(), role.getManageType())));
        assertEquals(2, rolesByApplication.size());
        assertEquals(2, rolesByApplication.get(new ManageIdentifier("1", EntityType.OIDC10_RP)).size());
        assertEquals(1, rolesByApplication.get(new ManageIdentifier("2", EntityType.SAML20_SP)).size());
    }
}