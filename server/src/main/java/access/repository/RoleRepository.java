package access.repository;

import access.model.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    @Query(value = "SELECT *, (SELECT COUNT(*) FROM user_roles ur WHERE ur.role_id=r.id) as userRoleCount " +
            "FROM roles r WHERE MATCH (name, description) against (?1  IN BOOLEAN MODE) AND id > 0 LIMIT ?2",
            nativeQuery = true)
    List<Role> search(String keyWord, int limit);


    @Query(value = """
            SELECT r.id as id, r.name as name, r.description as description,
                               a.manage_id as manage_id, a.manage_type as manage_type,
                (SELECT COUNT(*) FROM user_roles ur WHERE ur.role_id=r.id) as user_role_count
            FROM roles r INNER JOIN application_usages au on au.role_id = r.id
                INNER JOIN applications a on au.application_id = a.id
            """,
            countQuery = """
                    SELECT COUNT(r.id) FROM roles r
                    """,
            nativeQuery = true)
    Page<Map<String, Object>> searchByPage(Pageable pageable);

    @Query(value = """
            SELECT r.id as id, r.name as name, r.description as description,
                               a.manage_id as manage_id, a.manage_type as manage_type,
                (SELECT COUNT(*) FROM user_roles ur WHERE ur.role_id=r.id) as user_role_count
            FROM roles r INNER JOIN application_usages au on au.role_id = r.id
                INNER JOIN applications a on au.application_id = a.id
            WHERE MATCH (name, description) against (?1 IN BOOLEAN MODE)
            """,
            countQuery = """
                    SELECT COUNT(r.id) FROM roles r WHERE MATCH (name, description) against (?1 IN BOOLEAN MODE)
                    """,
            nativeQuery = true)
    Page<Map<String, Object>> searchByPageWithKeyword(String keyword, Pageable pageable);

    List<Role> findByApplicationUsagesApplicationManageId(String manageId);

    List<Role> findByOrganizationGUID(String organizationGUID);

    Optional<Role> findByName(String name);

}
