package access.repository;

import access.AbstractTest;
import access.model.User;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserRepositoryTest extends AbstractTest {

    @Test
    void findBySubIgnoreCase() {
        User user = userRepository.findBySubIgnoreCase("manager@example.com").get();
        assertEquals("manager@example.com", user.getSub());
    }

    @Test
    void search() {
        List<User> users = userRepository.search("exam*", 3);
        assertEquals(3, users.size());
    }
}