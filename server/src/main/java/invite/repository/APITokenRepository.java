package invite.repository;

import invite.model.APIToken;
import invite.model.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface APITokenRepository extends JpaRepository<APIToken, Long> {

    List<APIToken> findByOrganizationGUID(String organizationGUID);

    List<APIToken> findByOwner(User user);

    @EntityGraph(value = "findAll", type = EntityGraph.EntityGraphType.LOAD,
            attributePaths = {"owner"})
    @Override
    List<APIToken> findAll();

    Optional<APIToken> findByHashedValue(String hashedValue);
}
