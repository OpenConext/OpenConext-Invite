package access.model;

import access.manage.EntityType;
import access.manage.Identity;
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
        Map<Identity, List<Role>> rolesByApplication = roles.stream()
                .collect(Collectors.groupingBy(role -> new Identity(role.getManageId(), role.getManageType())));
        assertEquals(2, rolesByApplication.size());
        assertEquals(2, rolesByApplication.get(new Identity("1", EntityType.OIDC10_RP)).size());
        assertEquals(1, rolesByApplication.get(new Identity("2", EntityType.SAML20_SP)).size());
    }
}