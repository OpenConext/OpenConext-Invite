package access.repository;

import access.AbstractTest;
import access.model.User;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

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
        Page<Map<String, Object>> usersPage = userRepository.searchByPage(PageRequest.of(1, 3, Sort.by(Sort.Direction.ASC, "name")));
        assertEquals(3, usersPage.getContent().size());
    }

    @Test
    void searchByPage() {
        PageRequest pageRequest = PageRequest.of(1, 3, Sort.by(Sort.Direction.DESC, "given_name"));
        Page<Map<String, Object>> users = userRepository.searchByPageWithKeyword("exam*", pageRequest);
        assertEquals(6L, users.getTotalElements());
        assertEquals(3, users.getContent().size());
    }

    @Test
    void searchByPageSingleResult() {
        PageRequest pageRequest = PageRequest.of(0, 3, Sort.by(Sort.Direction.DESC, "given_name"));
        Page<Map<String, Object>> users = userRepository.searchByPageWithKeyword("mary*", pageRequest);
        assertEquals(1L, users.getTotalElements());
        assertEquals(1, users.getContent().size());
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