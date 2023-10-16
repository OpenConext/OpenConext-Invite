package access.repository;

import access.model.RemoteProvisionedGroup;
import access.model.RemoteProvisionedUser;
import access.model.Role;
import access.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface RemoteProvisionedUserRepository extends JpaRepository<RemoteProvisionedUser, Long> {

    Optional<RemoteProvisionedUser> findByManageProvisioningIdAndUser(String manageId, User user);

}
