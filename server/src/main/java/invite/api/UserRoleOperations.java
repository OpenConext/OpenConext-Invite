package invite.api;

import invite.exception.NotFoundException;
import invite.model.Role;
import invite.model.UserRole;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.ResponseEntity;

import java.util.List;

public class UserRoleOperations {

    private static final Log LOG = LogFactory.getLog(UserRoleOperations.class);

    private final UserRoleResource roleResource;

    public UserRoleOperations(UserRoleResource roleResource) {
        this.roleResource = roleResource;
    }

    public ResponseEntity<List<UserRole>> userRolesByRole(Long roleId,
                                                          RoleValidator roleValidator) {
        LOG.debug("/userRolesByRole/");

        Role role = this.roleResource.getRoleRepository().findById(roleId).orElseThrow(() -> new NotFoundException("Role not found"));
        roleValidator.validate(role);
        List<UserRole> userRoles = this.roleResource.getUserRoleRepository().findByRole(role);
        userRoles.forEach(userRole -> userRole.setUserInfo(userRole.getUser().asMap()));
        return ResponseEntity.ok(userRoles);
    }

}
