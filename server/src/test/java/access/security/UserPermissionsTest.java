package access.security;

import access.WithApplicationTest;
import access.exception.UserRestrictionException;
import access.manage.EntityType;
import access.model.*;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertThrows;

class UserPermissionsTest extends WithApplicationTest {

    private final static Random random = new SecureRandom();

    @Test
    void assertSuperUser() {
        User user = new User(true, Map.of());
        UserPermissions.assertManagerRole(List.of(), user);
        UserPermissions.assertSuperUser(user);
        UserPermissions.assertValidInvitation(user, Authority.SUPER_USER, List.of());
        UserPermissions.assertAuthority(user, Authority.MANAGER);
        UserPermissions.assertRoleAccess(user, null);
    }

    @Test
    void assertValidInvitationSuperUser() {
        assertThrows(UserRestrictionException.class, () ->
                UserPermissions.assertValidInvitation(new User(new HashMap<>()),Authority.SUPER_USER, new ArrayList<>()));
        User user = new User();
        user.setInstitutionAdmin(true);
        user.setApplications(List.of(Map.of("id", "1")));

        Role role = new Role();
        role.getApplications().add(new Application("1", EntityType.SAML20_SP));
        UserPermissions.assertValidInvitation(user, Authority.MANAGER, List.of(role));
    }

    @Test
    void assertValidInvitationInstitutionAdmin() {
        User user = new User();
        user.setInstitutionAdmin(true);
        user.setApplications(List.of(Map.of("id", "1")));

        Role role = new Role();
        role.getApplications().add(new Application("1", EntityType.SAML20_SP));

        UserPermissions.assertValidInvitation(new User(new HashMap<>()), Authority.INSTITUTION_ADMIN, new ArrayList<>());
    }

    @Test
    void assertInvalidInvitationInstitutionAdmin() {
        User user = new User();
        user.setApplications(List.of(Map.of("id", "1")));
        Role role = new Role();
        role.getApplications().add(new Application("1", EntityType.SAML20_SP));
        user.setUserRoles(Set.of(new UserRole(Authority.MANAGER, role)));

         assertThrows(UserRestrictionException.class, () ->
                UserPermissions.assertValidInvitation(new User(new HashMap<>()), Authority.INSTITUTION_ADMIN, List.of(role)));
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
        Role role = new Role("name", "description", "https://landingpage.com", application("manageIdentifier", EntityType.SAML20_SP), 365, false, false);
        role.setId(random.nextLong());
        List<Role> roles = List.of(role);
        assertThrows(UserRestrictionException.class, () -> UserPermissions.assertValidInvitation(user, Authority.GUEST, roles));
    }

    @Test
    void assertManagerRole() {
        String identifier = UUID.randomUUID().toString();
        User user = userWithRole(Authority.MANAGER, identifier);
        UserPermissions.assertManagerRole(List.of(Map.of("id", identifier)), user);
    }

    @Test
    void assertNotManagerRole() {
        String identifier = UUID.randomUUID().toString();
        User user = userWithRole(Authority.INVITER, identifier);
        assertThrows(UserRestrictionException.class, () -> UserPermissions.assertManagerRole(List.of(Map.of("id", identifier)), user));
    }

    @Test
    void assertManagerRoleNotProvisioning() {
        User user = userWithRole(Authority.MANAGER, "identifier");
        assertThrows(UserRestrictionException.class, () -> UserPermissions.assertManagerRole(List.of(Map.of("id", "nope")), user));
    }

    @Test
    void assertManagerRoleInstitutionAdmin() {
        User user = new User();
        user.setInstitutionAdmin(true);
        user.setApplications(List.of(Map.of("id", "1")));

        UserPermissions.assertManagerRole(List.of(Map.of("id", "1")), user);
    }

    @Test
    void assertRoleAccessInstitutionAdmin() {
        User user = new User();
        user.setInstitutionAdmin(true);
        user.setApplications(List.of(Map.of("id", "1")));

        Role role = new Role();
        role.getApplications().add(new Application("1", EntityType.SAML20_SP));

        UserPermissions.assertRoleAccess(user, role, Authority.INSTITUTION_ADMIN);
    }
    @Test
    void assertRoleAccess() {
        String identifier = UUID.randomUUID().toString();
        User user = userWithRole(Authority.GUEST, identifier);
        assertThrows(UserRestrictionException.class, () -> UserPermissions.assertRoleAccess(user, user.getUserRoles().iterator().next().getRole()));
    }

    @Test
    void assertRoleAccessManager() {
        String identifier = UUID.randomUUID().toString();
        User user = userWithRole(Authority.MANAGER, identifier);
        Role role = new Role("name", "description", "https://landingpage.com", application(identifier, EntityType.SAML20_SP), 365, false, false);
        role.setId(random.nextLong());
        UserPermissions.assertRoleAccess(user, role);
    }

    @Test
    void assertNoRoleAccess() {
        String identifier = UUID.randomUUID().toString();
        User user = userWithRole(Authority.GUEST, identifier);
        Role role = new Role("name", "description", "https://landingpage.com", application(identifier, EntityType.SAML20_SP), 365, false, false);
        role.setId(random.nextLong());
        assertThrows(UserRestrictionException.class, () -> UserPermissions.assertRoleAccess(user, role));
    }

    private User userWithRole(Authority authority, String manageIdentifier) {
        return userWithRole(new User(), authority, manageIdentifier);
    }

    private User userWithRole(User user, Authority authority, String manageIdentifier) {
        Role role = new Role("name", "description", "https://landingpage.com", application(manageIdentifier, EntityType.SAML20_SP), 365, false, false);
        role.setId(random.nextLong());
        user.addUserRole(new UserRole(authority, role));
        return user;
    }


}