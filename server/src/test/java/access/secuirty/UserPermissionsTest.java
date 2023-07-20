package access.secuirty;

import access.exception.UserRestrictionException;
import access.manage.EntityType;
import access.model.Authority;
import access.model.Role;
import access.model.User;
import access.model.UserRole;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertThrows;

class UserPermissionsTest {

    private final static Random random = new SecureRandom();

    @Test
    void assertSuperUser() {
        User user = new User(true, Map.of());
        UserPermissions.assertManagerRole(Map.of(), user);
        UserPermissions.assertSuperUser(user);
        UserPermissions.assertValidInvitation(user, Authority.SUPER_USER, List.of());
        UserPermissions.assertAuthority(user, Authority.MANAGER);
        UserPermissions.assertRoleAccess(user, null);
    }

    @Test
    void assertValidInvitationSuperUser() {
        assertThrows(UserRestrictionException.class, () ->
                UserPermissions.assertValidInvitation(new User(new HashMap<>()),Authority.SUPER_USER, new ArrayList<>()));
    }

    @Test
    void assertNotSuperUser() {
        User user = new User(false, Map.of());
        assertThrows(UserRestrictionException.class, () -> UserPermissions.assertSuperUser(user));
    }

    @Test
    void assertAuthority() {
        User user = userWithRole(Authority.MANAGER, "identifier");
        UserPermissions.assertAuthority(user, Authority.MANAGER);
    }

    @Test
    void assertInvalidAuthority() {
        User user = userWithRole(Authority.INVITER, "identifier");
        assertThrows(UserRestrictionException.class, () -> UserPermissions.assertAuthority(user, Authority.MANAGER));
    }

    @Test
    void assertValidManagerInvitation() {
        User user = userWithRole(Authority.MANAGER, "mail");
        userWithRole(user, Authority.MANAGER, "cloud");
        UserPermissions.assertValidInvitation(user, Authority.INVITER, user.getUserRoles().stream().map(UserRole::getRole).toList());
    }

    @Test
    void assertValidInvitation() {
        User user = userWithRole(Authority.INVITER, "mail");
        UserPermissions.assertValidInvitation(user, Authority.GUEST, user.getUserRoles().stream().map(UserRole::getRole).toList());
    }

    @Test
    void assertInvalidManagerInvitation() {
        User user = userWithRole(Authority.INVITER, "mail");
        assertThrows(UserRestrictionException.class, () -> UserPermissions.assertValidInvitation(user, Authority.INVITER, user.getUserRoles().stream().map(UserRole::getRole).toList()));
    }

    @Test
    void assertInvalidInvitation() {
        User user = userWithRole(Authority.INVITER, "mail");
        Role role = new Role("name", "description", "https://landingpage.com","manageIdentifier", EntityType.SAML20_SP, 365, false, false);
        role.setId(random.nextLong());
        List<Role> roles = List.of(role);
        assertThrows(UserRestrictionException.class, () -> UserPermissions.assertValidInvitation(user, Authority.GUEST, roles));
    }

    @Test
    void assertManagerRole() {
        String identifier = UUID.randomUUID().toString();
        User user = userWithRole(Authority.MANAGER, identifier);
        UserPermissions.assertManagerRole(Map.of("id", identifier), user);
    }

    @Test
    void assertNotManagerRole() {
        String identifier = UUID.randomUUID().toString();
        User user = userWithRole(Authority.INVITER, identifier);
        assertThrows(UserRestrictionException.class, () -> UserPermissions.assertManagerRole(Map.of("id", identifier), user));
    }

    @Test
    void assertManagerRoleNotProvisioning() {
        User user = userWithRole(Authority.MANAGER, "identifier");
        assertThrows(UserRestrictionException.class, () -> UserPermissions.assertManagerRole(Map.of("id", "nope"), user));
    }

    @Test
    void assertRoleAccess() {
        String identifier = UUID.randomUUID().toString();
        User user = userWithRole(Authority.GUEST, identifier);
        UserPermissions.assertRoleAccess(user, user.getUserRoles().iterator().next().getRole());
    }

    @Test
    void assertRoleAccessManager() {
        String identifier = UUID.randomUUID().toString();
        User user = userWithRole(Authority.MANAGER, identifier);
        Role role = new Role("name", "description", "https://landingpage.com",identifier, EntityType.SAML20_SP, 365, false, false);
        role.setId(random.nextLong());
        UserPermissions.assertRoleAccess(user, role);
    }

    @Test
    void assertNoRoleAccess() {
        String identifier = UUID.randomUUID().toString();
        User user = userWithRole(Authority.GUEST, identifier);
        Role role = new Role("name", "description", "https://landingpage.com",identifier, EntityType.SAML20_SP, 365, false, false);
        role.setId(random.nextLong());
        assertThrows(UserRestrictionException.class, () -> UserPermissions.assertRoleAccess(user, role));
    }

    private User userWithRole(Authority authority, String manageIdentifier) {
        return userWithRole(new User(), authority, manageIdentifier);
    }

    private User userWithRole(User user, Authority authority, String manageIdentifier) {
        Role role = new Role("name", "description", "https://landingpage.com",manageIdentifier, EntityType.SAML20_SP, 365, false, false);
        role.setId(random.nextLong());
        user.addUserRole(new UserRole(authority, role));
        return user;
    }
}