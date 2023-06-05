package access.secuirty;

import access.exception.UserRestrictionException;
import access.model.Authority;
import access.model.Role;
import access.model.User;
import access.model.UserRole;

import java.util.List;
import java.util.Map;
import java.util.Set;

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

    public static void assertValidInvitation(User user, Authority intendedAuthority, List<Role> roles) {
        if (user.isSuperUser()) {
            return;
        }
        //For all roles verify that the user is an inviter or a manager of the application of the role
        Set<UserRole> userRoles = user.getUserRoles();
        boolean allowed = roles.stream()
                .allMatch(role -> mayInviteByApplication(userRoles, intendedAuthority, role) ||
                        mayInviteByAuthority(userRoles, intendedAuthority, role));
        if (!allowed) {
            throw new UserRestrictionException();
        }
    }

    //Does one the userRoles has Authority.MANAGE and has the same application as the role
    private static boolean mayInviteByApplication(Set<UserRole> userRoles, Authority intendedAuthority, Role role) {
        if (intendedAuthority.hasEqualOrHigherRights(Authority.MANAGER)) {
            //only superUsers can invite manager
            throw new UserRestrictionException();
        }
        return userRoles.stream()
                .anyMatch(userRole -> userRole.getRole().getManageId().equals(role.getManageId()) &&
                        userRole.getAuthority().hasEqualOrHigherRights(Authority.MANAGER));
    }

    //Does one the userRoles has at least the Authority.INVITER for the role requested
    private static boolean mayInviteByAuthority(Set<UserRole> userRoles, Authority intendedAuthority, Role role) {
        if (intendedAuthority.hasEqualOrHigherRights(Authority.INVITER)) {
            //only managers can invite inviters
            throw new UserRestrictionException();
        }
        return userRoles.stream()
                .anyMatch(userRole -> userRole.getAuthority().hasEqualOrHigherRights(Authority.INVITER) &&
                        userRole.getRole().getId().equals(role.getId()));
    }

    public static void assertManagerRole(Map<String, Object> provider, User user) {
        String manageId = (String) provider.get("id");
        if (!user.isSuperUser()) {
            user.getUserRoles().stream()
                    .filter(userRole -> userRole.getAuthority().hasEqualOrHigherRights(Authority.MANAGER)
                            && userRole.getRole().getManageId().equals(manageId))
                    .findFirst()
                    .orElseThrow(UserRestrictionException::new);
        }
    }

    public static void assertRoleAccess(User user, Role accessRole) {
        if (user.isSuperUser()) {
            return;
        }
        user.getUserRoles().stream()
                .filter(userRole -> userRole.getRole().getId().equals(accessRole.getId()) ||
                        (userRole.getRole().getManageId().equals(accessRole.getManageId()) &&
                                userRole.getAuthority().hasEqualOrHigherRights(Authority.MANAGER)))
                .findFirst()
                .orElseThrow(UserRestrictionException::new);
    }

}
