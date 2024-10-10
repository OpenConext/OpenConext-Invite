package access.security;

import access.exception.UserRestrictionException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

class RemoteUserPermissionsTest {

    @Test
    void assertScopeAccess() {
        assertThrows(UserRestrictionException.class, () -> RemoteUserPermissions.assertScopeAccess(null));
        assertThrows(UserRestrictionException.class, () -> RemoteUserPermissions.assertScopeAccess(new RemoteUser(), Scope.profile));

        RemoteUserPermissions.assertScopeAccess(new RemoteUser());
        RemoteUserPermissions.assertScopeAccess(
                new RemoteUser("user", "secret", List.of(Scope.profile)), Scope.profile);

    }

}