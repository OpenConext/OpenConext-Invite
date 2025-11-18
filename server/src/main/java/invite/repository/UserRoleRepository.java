package invite.repository;

import invite.model.Authority;
import invite.model.Role;
import invite.model.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryRewriter;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, Long>, QueryRewriter {

    List<UserRole> findByEndDateBefore(Instant instant);

    List<UserRole> findByEndDateBeforeAndExpiryNotifications(Instant instant, Integer expiryNotifications);

    List<UserRole> findByRole(Role role);

    List<UserRole> findByRoleAndAuthorityIn(Role role, List<Authority> authorities);

    List<UserRole> findByRoleName(String roleName);

    long countByAuthority(Authority authority);

    @Modifying
    @Query(value = "DELETE FROM user_roles WHERE id = ?1", nativeQuery = true)
    @Transactional(isolation = Isolation.SERIALIZABLE)
    void deleteUserRoleById(Long id);

    @Modifying
    @Query(value = "UPDATE user_roles SET expiry_notifications= ?1 WHERE id = ?2", nativeQuery = true)
    @Transactional(isolation = Isolation.SERIALIZABLE)
    void updateExpiryNotifications(Integer expiryNotifications, Long id);


    @Query(value = """
            SELECT ur.id, ur.authority, ur.end_date as endDate, ur.created_at as createdAt, u.id as user_id, 
                   u.name, u.email, u.schac_home_organization, r.id as role_id
            FROM user_roles ur INNER JOIN users u on u.id = ur.user_id
                        INNER JOIN roles r on r.id = ur.role_id
            WHERE ur.role_id = ?1 AND (ur.authority = 'GUEST' OR ur.guest_role_included )
            """,
            countQuery = """
                    SELECT COUNT(ur.id) FROM user_roles ur INNER JOIN users u on u.id = ur.user_id
                    WHERE ur.role_id = ?1 AND (ur.authority = 'GUEST' OR ur.guest_role_included )
                    """,
            queryRewriter = UserRoleRepository.class,
            nativeQuery = true)
    Page<Map<String, Object>> searchGuestsByPage(Long roleId, Pageable pageable);

    @Query(value = """
            SELECT ur.id, ur.authority, ur.end_date as endDate, ur.created_at as createdAt, u.id as user_id,u.name, 
                               u.email, u.schac_home_organization, r.id as role_id
            FROM user_roles ur 
                    INNER JOIN users u on u.id = ur.user_id
                    INNER JOIN roles r on r.id = ur.role_id
            WHERE ur.role_id = ?1 AND (ur.authority = 'GUEST' OR ur.guest_role_included )
                      AND MATCH (u.given_name, u.family_name, u.email, u.schac_home_organization) AGAINST (?2  IN BOOLEAN MODE)
            """,
            countQuery = """
                    SELECT COUNT(ur.id) FROM user_roles ur INNER JOIN users u on u.id = ur.user_id
                    WHERE ur.role_id = ?1  AND (ur.authority = 'GUEST' OR ur.guest_role_included )
                    AND MATCH (u.given_name, u.family_name, u.email, u.schac_home_organization) AGAINST (?2  IN BOOLEAN MODE)
                    """,
            queryRewriter = UserRoleRepository.class,
            nativeQuery = true)
    Page<Map<String, Object>> searchGuestsByPageWithKeyword(Long roleId, String keyWord, Pageable pageable);

    @Query(value = """
            SELECT ur.id, ur.authority, ur.end_date as endDate, ur.created_at as createdAt, u.id as user_id,
                               u.name, u.email, u.schac_home_organization, r.id as role_id
            FROM user_roles ur
                        INNER JOIN roles r on r.id = ur.role_id
                        INNER JOIN users u on u.id = ur.user_id WHERE ur.role_id = ?1 AND ur.authority <> 'GUEST'
            """,
            countQuery = """
                    SELECT COUNT(ur.id) FROM user_roles ur INNER JOIN users u on u.id = ur.user_id
                    WHERE ur.role_id = ?1 AND ur.authority <> 'GUEST'
                    """,
            queryRewriter = UserRoleRepository.class,
            nativeQuery = true)
    Page<Map<String, Object>> searchNonGuestsByPage(Long roleId, Pageable pageable);

    @Query(value = """
            SELECT ur.id, ur.authority, ur.end_date as endDate, ur.created_at as createdAt, u.id as user_id,
                               u.name, u.email, u.schac_home_organization, r.id as role_id
            FROM user_roles ur
                    INNER JOIN roles r on r.id = ur.role_id
                    INNER JOIN users u on u.id = ur.user_id WHERE ur.role_id = ?1
            AND ur.authority <> 'GUEST' AND MATCH (u.given_name, u.family_name, u.email, u.schac_home_organization) AGAINST (?2  IN BOOLEAN MODE)
            """,
            countQuery = """
                    SELECT COUNT(ur.id) FROM user_roles ur INNER JOIN users u on u.id = ur.user_id
                    WHERE ur.role_id = ?1 AND ur.authority <> 'GUEST'
                    AND MATCH (u.given_name, u.family_name, u.email, u.schac_home_organization) AGAINST (?2  IN BOOLEAN MODE)
                    """,
            queryRewriter = UserRoleRepository.class,
            nativeQuery = true)
    Page<Map<String, Object>> searchNonGuestsByPageWithKeyword(Long roleId, String keyWord, Pageable pageable);

    @Query(value = """
            SELECT ur.id, ur.authority, ur.end_date as endDate, ur.created_at as createdAt, u.id as user_id,
                               u.name, u.email, u.schac_home_organization, r.id as role_id
            FROM user_roles ur
                    INNER JOIN roles r on r.id = ur.role_id
                    INNER JOIN users u on u.id = ur.user_id WHERE ur.role_id = ?1
            AND ur.authority <> 'GUEST' AND 
              (u.email LIKE ?2 or u.schac_home_organization LIKE ?2)
            """,
            countQuery = """
                    SELECT COUNT(ur.id) FROM user_roles ur INNER JOIN users u on u.id = ur.user_id
                    WHERE ur.role_id = ?1 AND ur.authority <> 'GUEST' 
                    AND (u.email LIKE ?2 or u.schac_home_organization LIKE ?2)
                    """,
            queryRewriter = UserRoleRepository.class,
            nativeQuery = true)
    Page<Map<String, Object>> searchNonGuestsByPageWithStrictSearch(Long roleId, String keyWord, Pageable pageable);

    @Query(value = """
            SELECT ur.id, ur.authority, ur.end_date as endDate, ur.created_at as createdAt, u.id as user_id,
                               u.name, u.email, u.schac_home_organization, r.id as role_id
            FROM user_roles ur
                    INNER JOIN roles r on r.id = ur.role_id
                    INNER JOIN users u on u.id = ur.user_id WHERE ur.role_id = ?1
            AND (ur.authority = 'GUEST' OR ur.guest_role_included ) AND 
              (u.email LIKE ?2 or u.schac_home_organization LIKE ?2)
            """,
            countQuery = """
                    SELECT COUNT(ur.id) FROM user_roles ur INNER JOIN users u on u.id = ur.user_id
                    WHERE ur.role_id = ?1 AND (ur.authority = 'GUEST' OR ur.guest_role_included )
                    AND (u.email LIKE ?2 or u.schac_home_organization LIKE ?2)
                    """,
            queryRewriter = UserRoleRepository.class,
            nativeQuery = true)
    Page<Map<String, Object>> searchGuestsByPageWithStrictSearch(Long roleId, String keyWord, Pageable pageable);

    @Override
    default String rewrite(String query, Sort sort) {
        Sort.Order nameSort = sort.getOrderFor("name");
        if (nameSort != null) {
            //Spring can not sort on aggregated columns
            return query.replace("order by ur.name", "order by u.name");
        }
        Sort.Order schacHomeSort = sort.getOrderFor("schac_home_organization");
        if (schacHomeSort != null) {
            //Spring can not sort on aggregated columns
            return query.replace("order by ur.schac_home_organization", "order by u.schac_home_organization");
        }
        return query;
    }
}
