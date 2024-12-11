package access.repository;

import access.model.Invitation;
import access.model.Role;
import access.model.Status;
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


    @Query(value = """
            SELECT i.id, i.email, i.intended_authority,i.created_at, i.expiry_date,
            u.name, u.email,
            (SELECT GROUP_CONCAT(DISTINCT r.name) FROM roles r WHERE r.id = ir.role_id) as role_names
            FROM invitations i INNER JOIN users u ON u.id = i.inviter_id INNER JOIN invitation_roles ir ON ir.invitation_id = i.id
            INNER JOIN roles r ON r.id = ir.role_id
            WHERE i.status = ?1            
            """,
            countQuery = "SELECT count(*) FROM invitations WHERE status = ?1",
            queryRewriter = InvitationRepository.class,
            nativeQuery = true)
    Page<Map<String, Object>> searchByStatusPage(Status status, Pageable pageable);

    @Query(value = """
            SELECT i.id, i.email, i.intended_authority,i.created_at, i.expiry_date,
            u.name, u.email,
            (SELECT GROUP_CONCAT(DISTINCT r.name) FROM roles r WHERE r.id = ir.role_id) as role_names
            FROM invitations i INNER JOIN users u ON u.id = i.inviter_id INNER JOIN invitation_roles ir ON ir.invitation_id = i.id
            INNER JOIN roles r ON r.id = ir.role_id
            WHERE i.status = ?1 AND r.id = ?2
            """,
            countQuery = """
                    SELECT count(*) FROM invitations
                    INNER JOIN invitation_roles ir ON ir.invitation_id = i.id
                    INNER JOIN roles r ON r.id = ir.role_id
                    WHERE status = ?1 and role_id = ?1
                    """,
            queryRewriter = InvitationRepository.class,
            nativeQuery = true)
    Page<Map<String, Object>> searchByStatusAndRolePage(Status status, Long roleId, Pageable pageable);

    @Override
    default String rewrite(String query, Sort sort) {
        Sort.Order roleNameSort = sort.getOrderFor("role_names");
        if (roleNameSort != null) {
            return query.replace("order by i.role_names", "order by role_names");
        }
        Sort.Order nameSort = sort.getOrderFor("name");
        if (nameSort != null) {
            return query.replace("order by i.name", "order by u.name");
        }
        return query;
    }


}
