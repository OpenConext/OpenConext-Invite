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
    void findByOrganizationGUID_ApplicationUsagesApplicationManageId() {
        //mysql> select r.id, r.name,r.organization_guid, a.manage_id, a.manage_type from roles r
        // inner join application_usages au on au.role_id = r.id
        // inner join applications a on a.id = au.application_id;
    }

}