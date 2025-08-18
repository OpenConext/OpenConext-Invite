package invite.security;

import invite.exception.UserRestrictionException;
import invite.model.Authority;
import invite.model.Role;
import invite.model.User;
import invite.model.UserRole;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;

public class UserPermissions {
    private static final Log LOG = LogFactory.getLog(UserPermissions.class);
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
        LOG.debug(String.format("assertAuthority for user %s", user.getEduPersonPrincipalName()));

        if (user.isSuperUser()) {
            LOG.debug(String.format("user %s is superuser", user.getEduPersonPrincipalName()));
            return;
        }

        if (user.isInstitutionAdmin() && Authority.INSTITUTION_ADMIN.hasEqualOrHigherRights(authority)) {
            LOG.debug(String.format("user %s is InstitutionAdmin", user.getEduPersonPrincipalName()));
            return;
        }
        if (user.getUserRoles().stream()
                        .noneMatch(userRole -> userRole.getAuthority().hasEqualOrHigherRights(authority)))
            throw new UserRestrictionException();
    }

    public static void assertValidInvitation(User user, Authority intendedAuthority, List<Role> roles) {
        if (user == null) {
            throw new UserRestrictionException();
        }
        LOG.debug(String.format("assertValidInvitation for user %s", user.getEduPersonPrincipalName()));

        if (user.isSuperUser()) {
            return;
        }
        if (intendedAuthority.equals(Authority.SUPER_USER)) {
            throw new UserRestrictionException();
        }
        Set<UserRole> userRoles = user.getUserRoles();
        //Institution admin needs to own all roles
        if (user.isInstitutionAdmin() && roles.stream()
                .allMatch(role -> user.getOrganizationGUID().equals(role.getOrganizationGUID()))) {
            return;
        }
        //For all roles verify that the user has a higher authority then the one requested for all off the roles
        boolean allowed = roles.stream()
                .allMatch(role -> mayInviteByApplication(userRoles, intendedAuthority, role) ||
                        mayInviteByAuthority(userRoles, intendedAuthority, role));
        if (!allowed) {
            throw new UserRestrictionException();
        }
    }

    public static void assertRoleAccess(User user, Role accessRole, Authority authority) {
        if (user == null) {
            throw new UserRestrictionException();
        }
        LOG.debug(String.format("assertRoleAccess for user %s", user.getEduPersonPrincipalName()));

        if (user.isSuperUser()) {
            return;
        }
        if (accessRole == null) {
            throw new UserRestrictionException();
        }
        if (user.isInstitutionAdmin() && user.getOrganizationGUID().equals(accessRole.getOrganizationGUID())) {
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

    //Does one the userRoles has at least the Authority higher than the intendedAuthority and NOT Guest
    private static boolean mayInviteByAuthority(Set<UserRole> userRoles, Authority intendedAuthority, Role role) {
        return userRoles.stream()
                .anyMatch(userRole -> userRole.getAuthority().hasHigherRights(intendedAuthority) &&
                        userRole.getAuthority().hasEqualOrHigherRights(Authority.INVITER) &&
                        userRole.getRole().getId().equals(role.getId()));
    }

}
