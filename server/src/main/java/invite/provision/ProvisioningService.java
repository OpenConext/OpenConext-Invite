package invite.provision;

import invite.model.Role;
import invite.model.User;
import invite.model.UserRole;
import invite.provision.graph.GraphResponse;
import invite.provision.scim.OperationType;

import java.util.List;
import java.util.Optional;

public interface ProvisioningService {

    Optional<GraphResponse> newUserRequest(User user);

    void updateUserRequest(User user);

    void updateUserRoleRequest(UserRole userRole);

    void deleteUserRoleRequest(UserRole userRole);

    void deleteUserRequest(User user);

    void deleteUserRequest(User user, UserRole userRole);

    void deleteUserRequest(Role role);

    void newGroupRequest(Role role);

    void updateGroupRequest(UserRole userRole, OperationType operationType);

    void updateGroupRequest(List<String> previousManageIdentifiers, Role newRole, boolean nameChanged);

    void deleteGroupRequest(Role role);

    List<Provisioning> getProvisionings(List<UserRole> userRoles);


}
