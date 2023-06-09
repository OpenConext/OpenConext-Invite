package access.scim;

import com.fasterxml.jackson.core.JsonProcessingException;
import access.model.*;

import java.io.Serializable;
import java.util.Optional;

public interface SCIMService {

    void newUserRequest(User user);

    void deleteUserRequest(User user);

    void newGroupRequest(Role role);

    void updateGroupRequest(UserRole userRole, OperationType operationType);

    void deleteGroupRequest(Role role);

}
