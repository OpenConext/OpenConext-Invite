package access.provision;

import access.model.*;
import access.provision.graph.GraphResponse;
import access.provision.scim.OperationType;

import java.util.List;
import java.util.Optional;

public interface ProvisioningService {

    Optional<GraphResponse> newUserRequest(User user);

    void updateUserRequest(User user);

    void deleteUserRequest(User user);

    void newGroupRequest(Role role);

    void updateGroupRequest(UserRole userRole, OperationType operationType);

    void updateGroupRequest(List<String> previousManageIdentifiers, Role newRole, boolean nameChanged);

    void deleteGroupRequest(Role role);

}
