package access.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    void fromAttributes() {
        Map<String, Object> attributes = Map.of(
                "email", "john.doe@example.com"
        );
        User user = new User(false, attributes);
        assertEquals("John Doe", user.getName());

        user.updateAttributes(attributes);
        assertEquals("John Doe", user.getName());

        user = new User(false, Map.of(
                "name", "John Doe"
        ));
        assertEquals("John Doe", user.getName());

        user = new User(false, Map.of(
                "preferred_username", "John Doe"
        ));
        assertEquals("John Doe", user.getName());

        user = new User(false, Map.of(
                "given_name", "John",
                "family_name", "Doe"
        ));
        assertEquals("John Doe", user.getName());

        user = new User(false, Map.of(
                "sub", "urn:collab:person:example.com:manager"
        ));
        assertEquals("Manager", user.getName());

        user = new User(false, Map.of(
                "sub", "manager"
        ));
        assertEquals("Manager", user.getName());

        user = new User(false, Map.of(
                "name", "John Doe"
        ));
        assertEquals("John Doe", user.getName());
        assertEquals("John", user.getGivenName());
        assertEquals("Doe", user.getFamilyName());

        user = new User(false, Map.of());
        assertNull(user.getName());
    }

    @Test
    void latestUserRole() {
        User user = new User();
        assertTrue(user.latestUserRole().isEmpty());
        addUserRole(user, 5, 8, 2);

        Instant createdAt = user.latestUserRole().get().getCreatedAt();
        assertEquals(2, createdAt.until(Instant.now(), ChronoUnit.DAYS));
    }

    @Test
    void asMapDefensive() {
        User user = new User();
        user.setSub("urn:sub");
        Map<String, Object> map = user.asMap();
        assertEquals(user.getSub(), map.get("email"));
    }

    @Test
    void asMapSubMissing() {
        assertThrows(IllegalArgumentException.class, () -> {
            User user = new User();
            user.asMap();
        });
    }

    private void addUserRole(User user, int... pastDays) {
        Instant now = Instant.now();
        Arrays.stream(pastDays).forEach(pastDay -> {
            UserRole userRole = new UserRole();
            userRole.setCreatedAt(now.minus(pastDay, ChronoUnit.DAYS));
            user.addUserRole(userRole);
        });
    }

}