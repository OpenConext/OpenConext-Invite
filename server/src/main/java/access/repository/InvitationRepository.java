package access.repository;

import access.model.Invitation;
import access.model.Role;
import access.model.Status;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvitationRepository extends JpaRepository<Invitation, Long> {

    @EntityGraph(value = "findByHash", type = EntityGraph.EntityGraphType.LOAD,
            attributePaths = {"inviter", "roles", "roles.role"})
    Optional<Invitation> findByHash(String hash);

    Optional<Invitation> findTopBySubInviteeOrderByCreatedAtDesc(String email);

    List<Invitation> findByStatus(Status status);

    List<Invitation> findByStatusAndRoles_role(Status status, Role role);

    List<Invitation> findByRoles_roleIsIn(List<Role> roles);
}
