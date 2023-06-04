package access.repository;

import access.AbstractTest;
import access.model.Role;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RoleRepositoryTest extends AbstractTest {

    @Test
    void search() {
        List<Role> roles = roleRepository.search("desc*", 3);
        assertEquals(3, roles.size());
    }

    @Test
    void findDistinctByManageId() {
        List<String[]> manageIdentifiers = roleRepository.findDistinctManageIdentifiers();
        assertEquals(5, manageIdentifiers.size());
    }
}