package access.secuirty;

import access.exception.UserRestrictionException;
import access.model.Authority;
import access.model.User;

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

}
