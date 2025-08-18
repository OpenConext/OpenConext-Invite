package invite.repository;

import invite.AbstractTest;
import invite.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.Map;
import java.util.Optional;

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


}