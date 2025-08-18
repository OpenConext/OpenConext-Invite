package invite.repository;

import invite.AbstractTest;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserRoleRepositoryTest extends AbstractTest {

    @SneakyThrows
    @Test
    void searchGuestsByPage() {
        PageRequest pageRequest = PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "authority"));
        Long roleID = roleRepository.findByName("Wiki").get().getId();
        Page<Map<String, Object>> userRoles = userRoleRepository.searchGuestsByPage(roleID, pageRequest);
        assertEquals(2L, userRoles.getTotalElements());
        assertEquals(1, userRoles.getContent().size());
    }

    @SneakyThrows
    @Test
    void searchGuestsByPageWithKeyword() {
        PageRequest pageRequest = PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "u.email"));
        Long roleID = roleRepository.findByName("Wiki").get().getId();
        Page<Map<String, Object>> userRoles = userRoleRepository.searchGuestsByPageWithKeyword(roleID, "doe*", pageRequest);
        assertEquals(2L, userRoles.getTotalElements());
        assertEquals(1, userRoles.getContent().size());
    }

    @SneakyThrows
    @Test
    void searchNonGuestsByPage() {
        PageRequest pageRequest = PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "authority"));
        Long roleID = roleRepository.findByName("Wiki").get().getId();
        Page<Map<String, Object>> userRoles = userRoleRepository.searchNonGuestsByPage(roleID, pageRequest);
        assertEquals(2L, userRoles.getTotalElements());
        assertEquals(1, userRoles.getContent().size());
    }

    @SneakyThrows
    @Test
    void searchNonGuestsByPageWithKeyword() {
        PageRequest pageRequest = PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "u.email"));
        Long roleID = roleRepository.findByName("Wiki").get().getId();
        Page<Map<String, Object>> userRoles = userRoleRepository.searchNonGuestsByPageWithKeyword(roleID, "doe*", pageRequest);
        assertEquals(2L, userRoles.getTotalElements());
        assertEquals(1, userRoles.getContent().size());
    }
}