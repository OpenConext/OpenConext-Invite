package invite.security;

import invite.exception.UserRestrictionException;
import invite.manage.EntityType;
import invite.model.Application;
import invite.model.ApplicationUsage;
import invite.model.Role;
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
                new RemoteUser("user", "secret", null, List.of(Scope.profile), List.of(), false), Scope.profile);
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
        RemoteUser remoteUser = new RemoteUser("user", "secret", null, List.of(), List.of(application), false);
        RemoteUserPermissions.assertApplicationAccess(remoteUser, role);
        RemoteUserPermissions.assertApplicationAccess(remoteUser, List.of(role));
    }

    @Test
    void assertApplicationAccessDevMode() {
        Role role = new Role();
        Application application = new Application("1", EntityType.SAML20_SP);
        Set<ApplicationUsage> applicationUsages = Set.of(
                new ApplicationUsage(application, "landingPage")
        );
        role.setApplicationUsages(applicationUsages);
        RemoteUser remoteUser = new RemoteUser("user", "secret", null, List.of(), List.of(new Application("5", EntityType.SAML20_SP)), false);
        assertThrows(UserRestrictionException.class, () -> RemoteUserPermissions.assertApplicationAccess(remoteUser, role));

        RemoteUser remoteUserDevMode = new RemoteUser(remoteUser);
        remoteUserDevMode.setLocalDevMode(true);
        RemoteUserPermissions.assertApplicationAccess(remoteUserDevMode, role);
    }

}