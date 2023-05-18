package access.repository;

import access.model.Invitation;
import access.model.Status;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvitationRepository extends JpaRepository<Invitation, Long> {

    @EntityGraph(value = "findByHash", type = EntityGraph.EntityGraphType.LOAD,
            attributePaths = {"inviter", "roles", "roles.role", "roles.role.application"})
    Optional<Invitation> findByHash(String hash);

    Optional<Invitation> findByIdAndStatus(Long id, Status status);

    List<Invitation> findByStatusAndRoles_role_application_id(Status status, Long applicationId);
}
