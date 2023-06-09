package access.provision;

import access.model.*;
import access.provision.scim.OperationType;

public interface ProvisioningService {

    void newUserRequest(User user);

    void deleteUserRequest(User user);

    void newGroupRequest(Role role);

    void updateGroupRequest(UserRole userRole, OperationType operationType);

    void deleteGroupRequest(Role role);

}
