package access.repository;

import access.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findBySubIgnoreCase(String sub);

    List<User> findByOrganizationGUIDAndInstitutionAdmin(String organizationGUID, boolean institutionAdmin);

    List<User> findBySuperUserTrue();

    Optional<User> findByEduPersonPrincipalNameIgnoreCase(String eppn);

    Optional<User> findByEmailIgnoreCase(String email);

    List<User> findByLastActivityBefore(Instant instant);

    @Query(value = "SELECT * FROM users WHERE MATCH (given_name, family_name, email) against (?1  IN BOOLEAN MODE) " +
            "AND id > 0 LIMIT ?2",
            nativeQuery = true)
    List<User> search(String keyWord, int limit);

    @Query(value = "SELECT u.id, u.email, u.name, u.schac_home_organization, u.created_at, u.last_activity, " +
            "ur.authority, r.name AS role_name, r.id AS role_id, ur.end_date " +
            "FROM users u " +
            "INNER JOIN user_roles ur ON ur.user_id = u.id " +
            "INNER JOIN roles r ON r.id = ur.role_id " +
            "INNER JOIN application_usages au ON au.role_id = r.id " +
            "INNER JOIN applications a ON a.id = au.application_id " +
            "WHERE a.manage_id in ?1 AND " +
            "MATCH (u.given_name, u.family_name, u.email) AGAINST (?2  IN BOOLEAN MODE) " +
            "LIMIT ?3",
            nativeQuery = true)
    List<Map<String, Object>> searchByApplication(List<String> manageIdentifiers, String keyWord, int limit);

    @Query(value = """
             select u.name, u.email, u.schac_home_organization, u.sub, u.super_user, u.institution_admin,
                (SELECT GROUP_CONCAT(DISTINCT ur.authority) FROM user_roles ur WHERE ur.user_id = u.id) AS authority from users u
            """,
            countQuery = "SELECT count(*) FROM users",
            nativeQuery = true)
    Page<Map<String, Object>> searchByPage(Pageable pageable );

    @Query(value = """
             select u.name, u.email, u.schac_home_organization, u.sub, u.super_user, u.institution_admin,
                (SELECT GROUP_CONCAT(DISTINCT ur.authority) FROM user_roles ur WHERE ur.user_id = u.id) AS authority
             from users u WHERE MATCH (given_name, family_name, email) against (?1  IN BOOLEAN MODE)
            """,
            countQuery = "SELECT count(*) FROM users WHERE MATCH (given_name, family_name, email) against (?1  IN BOOLEAN MODE)",
            nativeQuery = true)
    Page<Map<String, Object>> searchByPageWithKeyword(String keyWord, Pageable pageable );

    @Query(value = "SELECT u.id, u.email, u.name, u.schac_home_organization, u.created_at, u.last_activity, " +
            "ur.authority, r.name AS role_name, r.id AS role_id, ur.end_date " +
            "FROM users u " +
            "INNER JOIN user_roles ur ON ur.user_id = u.id " +
            "INNER JOIN roles r ON r.id = ur.role_id " +
            "INNER JOIN application_usages au ON au.role_id = r.id " +
            "INNER JOIN applications a ON a.id = au.application_id " +
            "WHERE a.manage_id in ?1",
            nativeQuery = true)
    List<Map<String, Object>> searchByApplicationAllUsers(List<String> manageIdentifiers);

    @Query(value = "SELECT * FROM users u WHERE super_user = 0 AND institution_admin = 0 " +
            "AND NOT EXISTS (SELECT ur.id FROM user_roles ur WHERE ur.user_id = u.id)",
            nativeQuery = true)
    List<User> findNonSuperUserWithoutUserRoles();

}
