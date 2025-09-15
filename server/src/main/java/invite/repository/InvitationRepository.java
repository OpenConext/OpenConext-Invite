package invite.repository;

import invite.model.Authority;
import invite.model.Invitation;
import invite.model.Role;
import invite.model.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryRewriter;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface InvitationRepository extends JpaRepository<Invitation, Long>, QueryRewriter {

    @EntityGraph(value = "findByHash", type = EntityGraph.EntityGraphType.LOAD,
            attributePaths = {"inviter", "roles", "roles.role"})
    Optional<Invitation> findByHash(String hash);

    Optional<Invitation> findTopBySubInviteeOrderByCreatedAtDesc(String email);

    List<Invitation> findByStatus(Status status);

    List<Invitation> findByStatusAndRoles_role(Status status, Role role);

    long countByStatus(Status status);

    @Query(value = """
            SELECT i.id, i.email, i.remote_api_user, i.intended_authority, i.created_at, i.expiry_date,
            u.id as user_id, u.name, u.email as inviter_email
            FROM invitations i LEFT JOIN users u ON u.id = i.inviter_id
            WHERE i.status = ?1
            """,
            countQuery = "SELECT count(*) FROM invitations WHERE status = ?1",
            queryRewriter = InvitationRepository.class,
            nativeQuery = true)
    Page<Map<String, Object>> searchByStatusPage(String status, Pageable pageable);

    @Query(value = """
            SELECT i.id, i.email, i.remote_api_user, i.intended_authority,i.created_at, i.expiry_date,
            u.id as user_id, u.name, u.email as inviter_email
            FROM invitations i LEFT JOIN users u ON u.id = i.inviter_id
            WHERE i.status = ?1 AND
            (MATCH(i.email) AGAINST(?2 IN BOOLEAN MODE)
             OR MATCH (u.given_name, u.family_name, u.email, u.schac_home_organization) against (?2  IN BOOLEAN MODE))
            """,
            countQuery = """
                     SELECT count(*) FROM invitations i LEFT JOIN users u ON u.id = i.inviter_id
                     WHERE status = ?1 AND
                     (MATCH(i.email) AGAINST(?2 IN BOOLEAN MODE)
                     OR MATCH (u.given_name, u.family_name, u.email, u.schac_home_organization) against (?2 IN BOOLEAN MODE))
                    """,
            queryRewriter = InvitationRepository.class,
            nativeQuery = true)
    Page<Map<String, Object>> searchByStatusPageWithKeyword(String status, String keyWord, Pageable pageable);

    @Query(value = """
            SELECT i.id, i.email, i.remote_api_user, i.intended_authority,i.created_at, i.expiry_date,
            u.id as user_id, u.name, u.email as inviter_email
            FROM invitations i LEFT JOIN users u ON u.id = i.inviter_id INNER JOIN invitation_roles ir ON ir.invitation_id = i.id
            INNER JOIN roles r ON r.id = ir.role_id
            WHERE i.status = ?1 AND r.id = ?2
            """,
            countQuery = """
                    SELECT count(*) FROM invitations i
                    INNER JOIN invitation_roles ir ON ir.invitation_id = i.id
                    INNER JOIN roles r ON r.id = ir.role_id
                    WHERE status = ?1 and role_id = ?2
                    """,
            queryRewriter = InvitationRepository.class,
            nativeQuery = true)
    Page<Map<String, Object>> searchByStatusAndRolePage(String status, Long roleId, Pageable pageable);

    @Query(value = """
            SELECT i.id, i.email, i.remote_api_user, i.intended_authority,i.created_at, i.expiry_date,
            u.id as user_id, u.name, u.email as inviter_email
            FROM invitations i LEFT JOIN users u ON u.id = i.inviter_id INNER JOIN invitation_roles ir ON ir.invitation_id = i.id
            INNER JOIN roles r ON r.id = ir.role_id
            WHERE i.status = ?1 AND r.id = ?2 AND
            (MATCH(i.email) AGAINST(?3 IN BOOLEAN MODE)
             OR MATCH (u.given_name, u.family_name, u.email, u.schac_home_organization) against (?3 IN BOOLEAN MODE))
            """,
            countQuery = """
                    SELECT count(*) FROM invitations i
                    INNER JOIN invitation_roles ir ON ir.invitation_id = i.id
                    INNER JOIN roles r ON r.id = ir.role_id
                    LEFT JOIN users u ON u.id = i.inviter_id
                    WHERE status = ?1 and role_id = ?2 AND
                    (MATCH(i.email) AGAINST(?3 IN BOOLEAN MODE)
                     OR MATCH (u.given_name, u.family_name, u.email, u.schac_home_organization) against (?3 IN BOOLEAN MODE))
                    """,
            queryRewriter = InvitationRepository.class,
            nativeQuery = true)
    Page<Map<String, Object>> searchByStatusAndRoleWithKeywordPage(String status, Long roleId, String keyWord, Pageable pageable);

    @Query(value = """
                SELECT ir.invitation_id as id, r.name, r.id as role_id, a.manage_id
                FROM roles r INNER JOIN invitation_roles ir ON ir.role_id = r.id
                INNER JOIN application_usages au ON au.role_id = r.id
                INNER JOIN applications a ON a.id = au.application_id
                WHERE ir.invitation_id IN ?1
            """, nativeQuery = true)
    List<Map<String, Object>> findRoles(List<Long> invitationIdentifiers);

    @Override
    default String rewrite(String query, Sort sort) {
        Sort.Order nameSort = sort.getOrderFor("name");
        if (nameSort != null) {
            return query.replace("order by i.name", "order by u.name");
        }
        return query;
    }


}
