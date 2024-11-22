package access.repository;

import access.model.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryRewriter;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long>, QueryRewriter {

    @Query(value = "SELECT *, (SELECT COUNT(*) FROM user_roles ur WHERE ur.role_id=r.id) as userRoleCount " +
            "FROM roles r WHERE MATCH (name, description) against (?1  IN BOOLEAN MODE) AND id > 0 LIMIT ?2",
            nativeQuery = true)
    List<Role> search(String keyWord, int limit);


    @Query(value = """
            SELECT r.id as id, r.name as name, r.description as description,
                (SELECT COUNT(*) FROM user_roles ur WHERE ur.role_id=r.id) as userRoleCount
            FROM roles r
            """,
            countQuery = """
                    SELECT COUNT(r.id) FROM roles r
                    """,
            queryRewriter = RoleRepository.class,
            nativeQuery = true)
    Page<Map<String, Object>> searchByPage(Pageable pageable);

    @Query(value = """
            SELECT r.id as id, r.name as name, r.description as description,
                (SELECT COUNT(*) FROM user_roles ur WHERE ur.role_id=r.id) as userRoleCount
            FROM roles r WHERE MATCH (name, description) against (?1 IN BOOLEAN MODE)
            """,
            countQuery = """
                    SELECT COUNT(r.id) FROM roles r WHERE MATCH (name, description) against (?1 IN BOOLEAN MODE)
                    """,
            queryRewriter = RoleRepository.class,
            nativeQuery = true)
    Page<Map<String, Object>> searchByPageWithKeyword(String keyword, Pageable pageable);


    @Query(value = """
            SELECT r.id as id, r.name as name, r.description as description,
                (SELECT COUNT(*) FROM user_roles ur WHERE ur.role_id=r.id) as userRoleCount
            FROM roles r WHERE r.organization_guid = ?1
            """,
            countQuery = """
                    SELECT COUNT(r.id) FROM roles r WHERE r.organization_guid = ?1
                    """,
            queryRewriter = RoleRepository.class,
            nativeQuery = true)
    Page<Map<String, Object>> searchByPageAndOrganizationGUID(String organizationGUID, Pageable pageable);

    @Query(value = """
            SELECT r.id as role_id, a.manage_id as manage_id, a.manage_type as manage_type
            FROM applications a INNER JOIN application_usages au on au.application_id = a.id
            INNER JOIN roles r on au.role_id = r.id WHERE r.id in ?1
            """,
            nativeQuery = true)
    List<Map<String, Object>> findApplications(List<Long> roleIdentifiers);

    List<Role> findByApplicationUsagesApplicationManageId(String manageId);

    List<Role> findByOrganizationGUID(String organizationGUID);

    Optional<Role> findByName(String name);

    @Override
    default String rewrite(String query, Sort sort) {
        Sort.Order userRoleCount = sort.getOrderFor("userRoleCount");
        if (userRoleCount != null) {
            //Spring can not sort on aggregated columns
            return query.replace(" order by r.userRoleCount", " order by userRoleCount");
        }
        return query;
    }

}
