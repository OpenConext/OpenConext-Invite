package access.repository;

import access.AbstractTest;
import access.model.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

    @Test
    void searchByApplication() {
        List<String> manageIdentifiers = IntStream.range(1, 7).boxed().map(String::valueOf).collect(Collectors.toList());
        List<Map<String, Object>> results = userRepository.searchByApplication(manageIdentifiers, "exam*", 3);
        assertEquals(3, results.size());
        List<Map<String, Object>> converted = objectMapper.convertValue(results, new TypeReference<>() {
        });
        assertEquals(3, converted.size());
    }

}