package invite.api;

import invite.repository.RoleRepository;
import invite.repository.UserRoleRepository;

public interface UserRoleResource {

    RoleRepository getRoleRepository();

    UserRoleRepository getUserRoleRepository();
}
