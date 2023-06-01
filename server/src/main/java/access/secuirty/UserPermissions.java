package access.secuirty;

import access.exception.UserRestrictionException;
import access.model.Authority;
import access.model.User;

import java.util.Map;

public class UserPermissions {

    private UserPermissions() {
    }

    public static void assertSuperUser(User user) {
        if (!user.isSuperUser()) {
            throw new UserRestrictionException();
        }
    }

    public static void assertAuthority(User user, Authority authority) {
        if (!user.isSuperUser() && user.getUserRoles().stream()
                .noneMatch(userRole -> userRole.getAuthority().hasEqualOrHigherRights(authority)))
            throw new UserRestrictionException();
    }

    public static void assertManageRole(Map<String, Object> provider, User user) {
        if (!user.isSuperUser()) {
            user.getUserRoles().stream()
                    .filter(userRole -> userRole.getAuthority().hasEqualOrHigherRights(Authority.MANAGER)
                            && userRole.getRole().getManageId().equals(provider.get("id")))
                    .findFirst()
                    .orElseThrow(UserRestrictionException::new);
        }
    }

}
