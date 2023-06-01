package access.repository;

import access.AbstractTest;
import access.model.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserRepositoryTest extends AbstractTest {

    @Test
    void findBySubIgnoreCase() {
        User user = userRepository.findBySubIgnoreCase("manager@example.com").get();
        assertEquals("manager@example.com", user.getSub());
    }
}