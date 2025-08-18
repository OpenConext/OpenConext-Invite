package invite.repository;

import invite.model.APIToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface APITokenRepository extends JpaRepository<APIToken, Long> {

    List<APIToken> findByOrganizationGUID(String organizationGUID);

    List<APIToken> findBySuperUserTokenTrue();

    Optional<APIToken> findByHashedValue(String hashedValue);
}
