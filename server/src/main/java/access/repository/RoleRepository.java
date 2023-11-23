package access.repository;

import access.model.DistinctManagerIdentifiers;
import access.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    @Query(value = "SELECT *, (SELECT COUNT(*) FROM user_roles ur WHERE ur.role_id=r.id) as userRoleCount " +
            "FROM roles r WHERE MATCH (name, description) against (?1  IN BOOLEAN MODE) AND id > 0 LIMIT ?2",
            nativeQuery = true)
    List<Role> search(String keyWord, int limit);


    @Query(value = "SELECT *, (SELECT COUNT(*) FROM user_roles ur WHERE ur.role_id=r.id) AS userRoleCount " +
            "FROM roles r WHERE json_contains(applications->'$[*].manageId', json_array(?1))",
            nativeQuery = true)
    List<Role> findByApplicationsManageId(String manageId);

    @Query(value = "SELECT DISTINCT JSON_EXTRACT(applications,'$[*].manageId','$[0].manageType') FROM roles", nativeQuery = true)
    List<String[]> findDistinctManageIdentifiers();

    @Query(value = "SELECT *, (SELECT COUNT(*) FROM user_roles ur WHERE ur.role_id=r.id) as userRoleCount " +
            "FROM roles r WHERE json_contains(applications->'$[*].manageId', json_array(?1)) and short_name = ?2",
            nativeQuery = true)
    Optional<Role> findByShortNameIgnoreCaseAndApplicationsManageId(String managerId, String name);

    List<Role> findByName(String name);

}
