package access.api;

import access.repository.RoleRepository;
import access.repository.UserRoleRepository;

public interface UserRoleResource {

    RoleRepository getRoleRepository();

    UserRoleRepository getUserRoleRepository();
}
