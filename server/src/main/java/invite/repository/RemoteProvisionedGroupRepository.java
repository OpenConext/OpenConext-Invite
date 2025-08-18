package invite.repository;

import invite.model.RemoteProvisionedGroup;
import invite.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RemoteProvisionedGroupRepository extends JpaRepository<RemoteProvisionedGroup, Long> {

    Optional<RemoteProvisionedGroup> findByManageProvisioningIdAndRole(String manageId, Role role);

}

