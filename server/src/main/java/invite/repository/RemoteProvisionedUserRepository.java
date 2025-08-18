package invite.repository;

import invite.model.RemoteProvisionedUser;
import invite.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RemoteProvisionedUserRepository extends JpaRepository<RemoteProvisionedUser, Long> {

    Optional<RemoteProvisionedUser> findByManageProvisioningIdAndUser(String manageId, User user);

}
