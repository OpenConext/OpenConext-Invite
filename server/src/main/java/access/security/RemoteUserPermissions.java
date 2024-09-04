package access.security;

import access.exception.UserRestrictionException;
import org.apache.commons.lang3.stream.Streams;

public class RemoteUserPermissions {

    private RemoteUserPermissions() {
    }

    public static void assertScopeAccess(RemoteUser remoteUser, String... scopes) {
        if (remoteUser == null) {
            throw new UserRestrictionException();
        }
        if (scopes == null || scopes.length == 0) {
            return;
        }
        if (!Streams.of(scopes).allMatch(scope -> remoteUser.getScopes().contains(scope))) {
            throw new UserRestrictionException();
        }
    }

}
