package access.secuirty;

import access.exception.UserRestrictionException;
import access.model.User;

public class UserPermissions {

    private UserPermissions() {
    }

    public static void assertSuperUser(User authenticatedUser) {
        if (!authenticatedUser.isSuperUser()) {
            throw new UserRestrictionException();
        }
    }

}
