package access.repository;

import access.model.Role;
import access.model.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, Long> {

    List<UserRole> findByEndDateBefore(Instant instant);

    List<UserRole> findByEndDateBeforeAndExpiryNotifications(Instant instant, Integer expiryNotifications);

    List<UserRole> findByRole(Role role);

    List<UserRole> findByRoleName(String roleName);

    @Modifying
    @Query(value = "DELETE FROM user_roles WHERE id = ?1", nativeQuery = true)
    @Transactional(isolation = Isolation.SERIALIZABLE)
    void deleteUserRoleById(Long id);

    @Modifying
    @Query(value = "UPDATE user_roles SET expiry_notifications= ?1 WHERE id = ?2", nativeQuery = true)
    @Transactional(isolation = Isolation.SERIALIZABLE)
    void updateExpiryNotifications(Integer expiryNotifications, Long id);


    @Query(value = """
                    SELECT ur.authority, ur.end_date, ur.created_at, u.name, u.email, u.schac_home_organization
                    FROM user_roles ur INNER JOIN users u on u.id = ur.user_id
                    WHERE ur.role_id = ?1 AND (ur.authority = 'GUEST' OR ur.guest_role_included )
            """,
            countQuery = """
                    SELECT COUNT(ur.id) FROM user_roles ur INNER JOIN users u on u.id = ur.user_id
                    WHERE ur.role_id = ?1 AND (ur.authority = 'GUEST' OR ur.guest_role_included )
                    """,
            nativeQuery = true)
    Page<Map<String, Object>> searchGuestsByPage(Long roleId, Pageable pageable);

    @Query(value = """
            SELECT ur.authority, ur.end_date, ur.created_at, u.name, u.email, u.schac_home_organization
            FROM user_roles ur INNER JOIN users u on u.id = ur.user_id
            WHERE ur.role_id = ?1 AND (ur.authority = 'GUEST' OR ur.guest_role_included )
                      AND MATCH (u.given_name, u.family_name, u.email) AGAINST (?2  IN BOOLEAN MODE)
            """,
            countQuery = """
                    SELECT COUNT(ur.id) FROM user_roles ur INNER JOIN users u on u.id = ur.user_id
                    WHERE ur.role_id = ?1  AND (ur.authority = 'GUEST' OR ur.guest_role_included )
                    AND MATCH (u.given_name, u.family_name, u.email) AGAINST (?2  IN BOOLEAN MODE)
                    """,
            nativeQuery = true)
    Page<Map<String, Object>> searchGuestsByPageWithKeyword(Long roleId, String keyWord, Pageable pageable);

    @Query(value = """
            SELECT ur.authority, ur.end_date, ur.created_at, u.name, u.email, u.schac_home_organization
            FROM user_roles ur INNER JOIN users u on u.id = ur.user_id WHERE ur.role_id = ?1 AND ur.authority <> 'GUEST'
            """,
            countQuery = """
                    SELECT COUNT(ur.id) FROM user_roles ur INNER JOIN users u on u.id = ur.user_id
                    WHERE ur.role_id = ?1 AND ur.authority <> 'GUEST'
                    """,
            nativeQuery = true)
    Page<Map<String, Object>> searchNonGuestsByPage(Long roleId, Pageable pageable);

    @Query(value = """
            SELECT ur.authority, ur.end_date, ur.created_at, u.name, u.email, u.schac_home_organization
            FROM user_roles ur INNER JOIN users u on u.id = ur.user_id WHERE ur.role_id = ?1
            AND ur.authority <> 'GUEST' AND MATCH (u.given_name, u.family_name, u.email) AGAINST (?2  IN BOOLEAN MODE)
            """,
            countQuery = """
                    SELECT COUNT(ur.id) FROM user_roles ur INNER JOIN users u on u.id = ur.user_id
                    WHERE ur.role_id = ?1 AND ur.authority <> 'GUEST'
                    AND MATCH (u.given_name, u.family_name, u.email) AGAINST (?2  IN BOOLEAN MODE)
                    """,
            nativeQuery = true)
    Page<Map<String, Object>> searchNonGuestsByPageWithKeyword(Long roleId, String keyWord, Pageable pageable);
}
