package access.repository;

import access.AbstractTest;
import access.model.User;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static access.AbstractTest.MANAGE_SUB;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserRepositoryTest extends AbstractTest {

    @Test
    void findBySubIgnoreCase() {
        User user = userRepository.findBySubIgnoreCase(MANAGE_SUB.toUpperCase()).get();
        assertEquals(MANAGE_SUB, user.getSub());
    }

    @Test
    void findBySubNull() {
        Optional<User> user = userRepository.findBySubIgnoreCase(null);
        assertTrue(user.isEmpty());
    }

    @Test
    void search() {
        List<User> users = userRepository.search("exam*", 3);
        assertEquals(3, users.size());
    }
}