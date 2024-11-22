package access.repository;

import access.AbstractTest;
import access.model.Role;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RoleRepositoryTest extends AbstractTest {

    @Test
    void search() {
        List<Role> roles = roleRepository.search("desc*", 3);
        assertEquals(3, roles.size());
    }

    @Test
    void searchByPage() {
        PageRequest pageRequest = PageRequest.of(3, 1, Sort.by(Sort.Direction.DESC, "name"));
        Page<Map<String, Object>> page = roleRepository.searchByPage(pageRequest);
        assertEquals(6L, page.getTotalElements());
        assertEquals(1, page.getContent().size());
        assertEquals("Network", page.getContent().get(0).get("name"));
    }

    @Test
    void searchByPageWithKeyword() {
        PageRequest pageRequest = PageRequest.of(1, 3, Sort.by(Sort.Direction.DESC, "name"));
        Page<Map<String, Object>> page = roleRepository.searchByPageWithKeyword("desc*", pageRequest);
        assertEquals(6L, page.getTotalElements());
        assertEquals(3, page.getContent().size());
        List<String> names = page.getContent().stream().map(m -> (String) m.get("name")).toList();
        //Sorted by name in descending order
        assertEquals(List.of("Network", "Mail", "Calendar"), names);
    }

    @Test
    void searchByPageWithMultipleApplicationsUsages() {
        PageRequest pageRequest = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "name"));
        //See AbstractTest#seed Storage has two applications linked
        Page<Map<String, Object>> page = roleRepository.searchByPageWithKeyword("Storage", pageRequest);
        assertEquals(1, page.getTotalElements());
        assertEquals(1, page.getContent().size());
    }

    @Test
    void searchByPageWiki() {
        PageRequest pageRequest = PageRequest.of(0, 15, Sort.by(Sort.Direction.ASC, "description"));
        Page<Map<String, Object>> page = roleRepository.searchByPageWithKeyword("wiki*", pageRequest);
        assertEquals(1L, page.getTotalElements());
        assertEquals(1, page.getContent().size());
        assertEquals(3L, page.getContent().get(0).get("userRoleCount"));
    }
}