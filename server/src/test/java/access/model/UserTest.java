package access.model;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class UserTest {

    @Test
    void fromAttributes() {
        User user = new User(false, Map.of(
                "email", "john.doe@example.com"
        ));
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

        user = new User(false, Map.of());
        assertNull(user.getName());
    }
}