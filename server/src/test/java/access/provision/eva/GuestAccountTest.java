package access.provision.eva;

import access.exception.InvalidInputException;
import access.manage.EntityType;
import access.model.*;
import access.provision.Provisioning;
import access.provision.ProvisioningType;
import org.junit.jupiter.api.Test;
import org.springframework.util.MultiValueMap;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static access.provision.eva.GuestAccount.EVA_DATE_PATTERN;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
    void invalidDateTill() {
        Map<String, Object> map = new HashMap<>();
        map.put("provisioning_type", ProvisioningType.eva.name());
        map.put("eva_token", "secret");
        map.put("eva_url", "https://eva");
        map.put("applications", List.of(Map.of("id", "manageId", "type", EntityType.SAML20_SP.name())));

        User user = new User();
        ApplicationUsage applicationUsage = new ApplicationUsage(new Application("manageId", EntityType.SAML20_SP), "https://landingpage.com");
        Role role = new Role(
                "name", "description", Set.of(applicationUsage), -5, false, false);
        UserRole userRole = new UserRole(Authority.GUEST, role);

        user.getUserRoles().add(userRole);

        assertThrows(InvalidInputException.class, () -> new GuestAccount(user, new Provisioning(map)));
    }

    @Test
    void nullDateTill() {
        Map<String, Object> map = new HashMap<>();
        map.put("provisioning_type", ProvisioningType.eva.name());
        map.put("eva_token", "secret");
        map.put("eva_url", "https://eva");
        map.put("applications", List.of(Map.of("id", "manageId", "type", EntityType.SAML20_SP.name())));

        User user = new User();
        ApplicationUsage applicationUsage = new ApplicationUsage(new Application("manageId", EntityType.SAML20_SP), "https://landingpage.com");
        Role role = new Role(
                "name", "description", Set.of(applicationUsage), -5, false, false);
        UserRole userRole = new UserRole(Authority.GUEST, role);
        userRole.setEndDate(null);

        user.getUserRoles().add(userRole);

        assertThrows(AssertionError.class, () -> new GuestAccount(user, new Provisioning(map)));
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