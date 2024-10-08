package access.repository;

import access.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    @Query(value = "SELECT *, (SELECT COUNT(*) FROM user_roles ur WHERE ur.role_id=r.id) as userRoleCount " +
            "FROM roles r WHERE MATCH (name, description) against (?1  IN BOOLEAN MODE) AND id > 0 LIMIT ?2",
            nativeQuery = true)
    List<Role> search(String keyWord, int limit);

    List<Role> findByApplicationUsagesApplicationManageId(String manageId);

    List<Role> findByOrganizationGUID_ApplicationUsagesApplicationManageId(String organizationGUID, String manageId);

    List<Role> findByName(String name);

}
