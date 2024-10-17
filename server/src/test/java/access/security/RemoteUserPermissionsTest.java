package access.security;

import access.exception.UserRestrictionException;
import access.manage.EntityType;
import access.model.Application;
import access.model.ApplicationUsage;
import access.model.Role;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertThrows;

class RemoteUserPermissionsTest {

    @Test
    void assertScopeAccess() {
        assertThrows(UserRestrictionException.class, () -> RemoteUserPermissions.assertScopeAccess(null));
        assertThrows(UserRestrictionException.class, () -> RemoteUserPermissions.assertScopeAccess(new RemoteUser(), Scope.profile));

        RemoteUserPermissions.assertScopeAccess(new RemoteUser());
        RemoteUserPermissions.assertScopeAccess(
                new RemoteUser("user", "secret", null, List.of(Scope.profile), List.of()), Scope.profile);
    }

    @Test
    void assertApplicationAccess() {
        Role role = new Role();
        Application application = new Application("1", EntityType.SAML20_SP);
        Set<ApplicationUsage> applicationUsages = Set.of(
                new ApplicationUsage(application, "landingPage")
        );
        role.setApplicationUsages(applicationUsages);
        assertThrows(UserRestrictionException.class, () -> RemoteUserPermissions.assertApplicationAccess(null, role));
        assertThrows(UserRestrictionException.class, () -> RemoteUserPermissions.assertApplicationAccess(new RemoteUser(), role));
        RemoteUser remoteUser = new RemoteUser("user", "secret", null, List.of(), List.of(application));
        RemoteUserPermissions.assertApplicationAccess(remoteUser, role);
        RemoteUserPermissions.assertApplicationAccess(remoteUser, List.of(role));
    }

}