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

    @Query(value = """
             SELECT u.name, u.email, u.schac_home_organization, u.super_user, u.institution_admin,
                u.created_at, u.last_activity,
                (SELECT GROUP_CONCAT(DISTINCT ur.authority) FROM user_roles ur WHERE ur.user_id = u.id) AS authority
                FROM users u
            """,
            countQuery = "SELECT count(*) FROM users",
            nativeQuery = true)
    Page<Map<String, Object>> searchByPage(Pageable pageable );

    @Query(value = """
             SELECT u.name, u.email, u.schac_home_organization, u.super_user, u.institution_admin,
                u.created_at, u.last_activity,
            (SELECT GROUP_CONCAT(DISTINCT ur.authority) FROM user_roles ur WHERE ur.user_id = u.id) AS authority
              FROM users u WHERE MATCH (given_name, family_name, email) against (?1  IN BOOLEAN MODE)
            """,
            countQuery = "SELECT count(*) FROM users WHERE MATCH (given_name, family_name, email) against (?1  IN BOOLEAN MODE)",
            nativeQuery = true)
    Page<Map<String, Object>> searchByPageWithKeyword(String keyWord, Pageable pageable );

    @Query(value = """
            SELECT distinct(u.id), u.email, u.name, u.schac_home_organization, u.created_at, u.last_activity
            FROM users u
            INNER JOIN user_roles ur ON ur.user_id = u.id
            INNER JOIN roles r ON r.id = ur.role_id
            WHERE r.organization_guid = ?1
            """ ,
            nativeQuery = true)
    Page<Map<String, Object>> searchByPageRoleUsers(String organisationGUID, Pageable pageable);

    @Query(value = """
            SELECT distinct(u.id), u.email, u.name, u.schac_home_organization, u.created_at, u.last_activity
            FROM users u
            INNER JOIN user_roles ur ON ur.user_id = u.id
            INNER JOIN roles r ON r.id = ur.role_id
            WHERE r.organization_guid = ?1 AND
            MATCH (given_name, family_name, email) against (?2  IN BOOLEAN MODE)
            """ ,
            nativeQuery = true)
    Page<Map<String, Object>> searchByPageRoleUsersWithKeyWord(String organisationGUID, String query, Pageable pageable);

    @Query(value = """
            SELECT ur.user_id, ur.authority, ur.end_date, r.name, r.id
            FROM user_roles ur INNER JOIN roles r ON r.id = ur.role_id
            WHERE ur.user_id in ?1
            """,
            nativeQuery = true)
    List<Map<String, Object>> findUserRoles(List<Long> userIdentifiers);



    @Query(value = "SELECT * FROM users u WHERE super_user = 0 AND institution_admin = 0 " +
            "AND NOT EXISTS (SELECT ur.id FROM user_roles ur WHERE ur.user_id = u.id)",
            nativeQuery = true)
    List<User> findNonSuperUserWithoutUserRoles();

}
