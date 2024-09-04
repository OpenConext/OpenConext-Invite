package access.security;

import access.exception.UserRestrictionException;
import access.model.Authority;
import access.model.Role;
import access.model.User;
import access.model.UserRole;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class UserPermissions {

    private UserPermissions() {
    }

    public static void assertSuperUser(User user) {
        if (user == null) {
            throw new UserRestrictionException();
        }

        if (!user.isSuperUser()) {
            throw new UserRestrictionException();
        }
    }

    public static void assertInstitutionAdmin(User user) {
        if (user == null) {
            throw new UserRestrictionException();
        }

        if (user.isSuperUser() || user.isInstitutionAdmin() && StringUtils.hasText(user.getOrganizationGUID())) {
            return;
        }
        throw new UserRestrictionException();
    }

    public static void assertAuthority(User user, Authority authority) {
        if (user == null) {
            throw new UserRestrictionException();
        }

        if (user.isInstitutionAdmin() && Authority.INSTITUTION_ADMIN.hasEqualOrHigherRights(authority)) {
            return;
        }
        if (!user.isSuperUser() && user.getUserRoles().stream()
                .noneMatch(userRole -> userRole.getAuthority().hasEqualOrHigherRights(authority)))
            throw new UserRestrictionException();
    }

    public static void assertValidInvitation(User user, Authority intendedAuthority, List<Role> roles) {
        if (user == null) {
            throw new UserRestrictionException();
        }

        if (user.isSuperUser()) {
            return;
        }
        if (intendedAuthority.equals(Authority.SUPER_USER)) {
            throw new UserRestrictionException();
        }
        //For all roles verify that the user has a higher authority then the one requested for all off the roles
        Set<UserRole> userRoles = user.getUserRoles();
        if (user.isInstitutionAdmin() && roles.stream()
                .allMatch(role -> mayInviteByInstitutionAdmin(user.getApplications(), role.applicationIdentifiers()))) {
            return;
        }
        boolean allowed = roles.stream()
                .allMatch(role -> mayInviteByApplication(userRoles, intendedAuthority, role) ||
                        mayInviteByAuthority(userRoles, intendedAuthority, role));
        if (!allowed) {
            throw new UserRestrictionException();
        }
    }

    public static void assertManagerRole(List<String> manageIdentifiers, User user) {
        if (user == null) {
            throw new UserRestrictionException();
        }
        if (user.isSuperUser()) {
            return;
        }
        if (user.isInstitutionAdmin() && mayInviteByInstitutionAdmin(user.getApplications(), manageIdentifiers)) {
            return;
        }
        user.getUserRoles().stream()
                .filter(userRole -> userRole.getAuthority().hasEqualOrHigherRights(Authority.MANAGER)
                        && userRole.getRole().applicationIdentifiers().stream().anyMatch(manageIdentifiers::contains))
                .findFirst()
                .orElseThrow(UserRestrictionException::new);
    }

    public static void assertRoleAccess(User user, Role accessRole) {
        assertRoleAccess(user, accessRole, Authority.MANAGER);
    }

    public static void assertRoleAccess(User user, Role accessRole, Authority authority) {
        if (user == null) {
            throw new UserRestrictionException();
        }
        if (user.isSuperUser()) {
            return;
        }
        if (user.isInstitutionAdmin() && mayInviteByInstitutionAdmin(user.getApplications(), accessRole.applicationIdentifiers())) {
            return;
        }
        user.getUserRoles().stream()
                .filter(userRole -> (userRole.getRole().getId().equals(accessRole.getId()) &&
                        userRole.getAuthority().hasEqualOrHigherRights(authority)) ||
                        (userRole.hasAccessToApplication(accessRole) &&
                                userRole.getAuthority().hasEqualOrHigherRights(Authority.INSTITUTION_ADMIN)))
                .findFirst()
                .orElseThrow(UserRestrictionException::new);
    }

    //Does one off the userRoles has Authority.MANAGE and has the same application as the role
    private static boolean mayInviteByApplication(Set<UserRole> userRoles, Authority intendedAuthority, Role role) {
        return userRoles.stream()
                .anyMatch(userRole -> userRole.hasAccessToApplication(role) &&
                        userRole.getAuthority().hasEqualOrHigherRights(Authority.MANAGER) &&
                        userRole.getAuthority().hasEqualOrHigherRights(intendedAuthority));
    }

    //Does the one off the applications has the same application as the role
    private static boolean mayInviteByInstitutionAdmin(List<Map<String, Object>> applications,
                                                       List<String> manageIdentifiers) {
        return applications.stream()
                .anyMatch(application -> manageIdentifiers.contains(application.get("id")));
    }

    //Does one the userRoles has at least the Authority higher than the intendedAuthority and NOT Guest
    private static boolean mayInviteByAuthority(Set<UserRole> userRoles, Authority intendedAuthority, Role role) {
        return userRoles.stream()
                .anyMatch(userRole -> userRole.getAuthority().hasHigherRights(intendedAuthority) &&
                        userRole.getAuthority().hasEqualOrHigherRights(Authority.INVITER) &&
                        userRole.getRole().getId().equals(role.getId()));
    }

}
