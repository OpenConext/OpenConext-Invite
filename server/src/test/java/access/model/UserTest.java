package access.model;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
    }
}