package invite.security;

import invite.exception.UserRestrictionException;
import invite.model.Application;
import invite.model.Authority;
import invite.model.Role;
import invite.model.User;
import invite.model.UserRole;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class UserPermissions {
    private static final Log LOG = LogFactory.getLog(UserPermissions.class);

    private UserPermissions() {
    }

    public static void assertSuperUser(User user) {
        if (user == null) {
            throw new UserRestrictionException("User is NULL");
        }

        if (!user.isSuperUser()) {
            throw new UserRestrictionException("User is no super user: " + user.getEmail());
        }
    }

    public static void assertInstitutionAdmin(User user) {
        if (user == null) {
            throw new UserRestrictionException("User is NULL");
        }

        if (user.isSuperUser() || (user.isInstitutionAdmin() && StringUtils.hasText(user.getOrganizationGUID()))) {
            return;
        }
        throw new UserRestrictionException("User is no institution admin: " + user.getEmail());
    }

    public static void assertApplicationManager(User user) {
        if (user == null) {
            throw new UserRestrictionException("User is NULL");
        }
        if (user.isSuperUser() ||
                (user.isInstitutionAdmin() && StringUtils.hasText(user.getOrganizationGUID())) ||
                !CollectionUtils.isEmpty(user.getUserApplications())) {
            return;
        }
        throw new UserRestrictionException(
                String.format("User %s is not a super user, institution admin or application manager", user.getEmail()));
    }

    public static void assertApplicationManager(User user, List<Role> roles) {
        if (user == null) {
            throw new UserRestrictionException("User is NULL");
        }
        if (user.isSuperUser()) {
            return;
        }
        if (CollectionUtils.isEmpty(roles)) {
            throw new UserRestrictionException("Roles are empty");
        }
        if (user.isInstitutionAdmin()) {
            if (!StringUtils.hasText(user.getOrganizationGUID())) {
                throw new UserRestrictionException(String.format("User %s is institutionAdmin, but has no organizationGUID", user.getEmail()));
            }
            if (roles.stream().allMatch(role -> Objects.equals(role.getOrganizationGUID(), user.getOrganizationGUID()))) {
                return;
            }
            throw new UserRestrictionException(String.format("User %s is institutionAdmin of %s, but does not own all roles %s",
                    user.getEmail(),
                    user.getOrganizationGUID(),
                    roles.stream().map(role -> role.getOrganizationGUID()).toList()));
        }
        // Ensure that through the Applications of the User, all Roles are owned
        Set<Long> roleApplicationIdentifiers = roles.stream().map(role -> role.getApplicationUsages())
                .flatMap(applicationUsages -> applicationUsages.stream()
                        .map(applicationUsage -> applicationUsage.getApplication().getId()))
                .collect(Collectors.toSet());
        Set<Long> userApplicationIdentifiers = user.getUserApplications().stream()
                .map(userApplication -> userApplication.getApplication().getId())
                .collect(Collectors.toSet());
        if (!userApplicationIdentifiers.containsAll(roleApplicationIdentifiers)) {
            throw new UserRestrictionException(
                    String.format("User %s does not own all Applications for the requested roles", user.getEmail()));
        }
    }

    public static void assertAuthority(User user, Authority authority) {
        if (user == null) {
            throw new UserRestrictionException("User is NULL");
        }

        if (user.isSuperUser()) {
            LOG.debug(String.format("user %s is superuser", user.getEduPersonPrincipalName()));
            return;
        }

        if (user.isInstitutionAdmin() && Authority.INSTITUTION_ADMIN.hasEqualOrHigherRights(authority)) {
            LOG.debug(String.format("user %s is InstitutionAdmin", user.getEduPersonPrincipalName()));
            return;
        }
        if (!user.getUserApplications().isEmpty() && Authority.APPLICATION_MANAGER.hasEqualOrHigherRights(authority)) {
            LOG.debug(String.format("user %s is ApplicationManager", user.getEduPersonPrincipalName()));
            return;
        }
        if (user.getUserRoles().stream()
                .noneMatch(userRole -> userRole.getAuthority().hasEqualOrHigherRights(authority)))
            throw new UserRestrictionException(String.format("User %s is not an Authority %s", user.getEmail(), authority));
    }

    public static void assertValidInvitation(User user, Authority intendedAuthority, List<Role> roles) {
        if (user == null) {
            throw new UserRestrictionException("User is NULL");
        }
        LOG.debug(String.format("assertValidInvitation for user %s", user.getEduPersonPrincipalName()));

        if (user.isSuperUser()) {
            return;
        }
        if (intendedAuthority.equals(Authority.SUPER_USER)) {
            throw new UserRestrictionException("Invalid invitation for super-user by " + user.getEmail());
        }
        if (!user.isInstitutionAdmin() && intendedAuthority.equals(Authority.INSTITUTION_ADMIN)) {
            throw new UserRestrictionException("Invalid institution admin invitation by " + user.getEmail());
        }
        //Institution admin needs to own all roles or be a member of the role for at least the authority of invitationo
        if (user.isInstitutionAdmin() && roles.stream()
                .allMatch(role -> user.getOrganizationGUID().equals(role.getOrganizationGUID()))) {
            return;
        }
        if (!user.getUserApplications().isEmpty()) {
            Set<Long> roleIdentifiers = roles.stream().map(role -> role.getId()).collect(Collectors.toSet());
            Set<Long> applicationManagerRoles = user.getUserApplications().stream().flatMap(userApplication ->
                            userApplication.getApplication().getApplicationUsages().stream()
                                    .map(applicationUsage -> applicationUsage.getRole().getId()))
                    .collect(Collectors.toSet());
            if (applicationManagerRoles.containsAll(roleIdentifiers)) {
                return;
            } else {
                //All roles not owned as application manager are assesed as normal userRoles
                roles = roles.stream().filter(role -> !applicationManagerRoles.contains(role.getId())).toList();
            }
        }
        //For all roles verify that the user has a higher authority then the one requested for all off the roles
        Set<UserRole> userRoles = user.getUserRoles();
        boolean allowed = roles.stream()
                .allMatch(role -> {
                    boolean mayInviteByInstitutionAdmin = user.isInstitutionAdmin() && user.getOrganizationGUID().equals(role.getOrganizationGUID());
                    boolean mayInviteByApplication = mayInviteByApplication(userRoles, intendedAuthority, role);
                    boolean mayInviteByAuthority = mayInviteByAuthority(userRoles, intendedAuthority, role);
                    return mayInviteByInstitutionAdmin || mayInviteByApplication || mayInviteByAuthority;
                });
        if (!allowed) {
            throw new UserRestrictionException(String.format("Invalid invation by %s for roles %s",
                    user.getEmail(), roles.stream().map(role -> role.getName()).collect(Collectors.joining(", "))));
        }
    }

    public static void assertValidApplicationManagerInvitation(User user, Authority intendedAuthority, List<Application> applications) {
        if (!Authority.APPLICATION_MANAGER.equals(intendedAuthority)) {
            return;
        }
        if (user == null) {
            throw new UserRestrictionException("User is NULL");
        }
        LOG.debug(String.format("assertValidInvitation for user %s", user.getEduPersonPrincipalName()));
        if (user.isSuperUser()) {
            return;
        }
        if (!user.isInstitutionAdmin()) {
            throw new UserRestrictionException(String.format("Invalid invation by %s for application %s",
                    user.getEmail(), applications.stream().map(application -> application.getManageId()).collect(Collectors.joining(", "))));
        }
    }

    public static void assertRoleAccess(User user, Role accessRole, Authority authority) {
        if (user == null) {
            throw new UserRestrictionException("USer is NULL");
        }
        LOG.debug(String.format("assertRoleAccess for user %s", user.getEduPersonPrincipalName()));

        if (user.isSuperUser()) {
            return;
        }
        if (accessRole == null) {
            throw new UserRestrictionException("Role is NULL");
        }
        if (user.isInstitutionAdmin() && user.getOrganizationGUID().equals(accessRole.getOrganizationGUID())) {
            return;
        }
        if (!user.getUserApplications().isEmpty()) {
            //Application Manager
            if (user.getUserApplications().stream()
                    .anyMatch(userApplication ->
                            userApplication.getApplication().getApplicationUsages().stream()
                                    .anyMatch(applicationUsage -> Objects.equals(applicationUsage.getRole().getId(), accessRole.getId())))) {
                return;
            }
        }
        user.getUserRoles().stream()
                .filter(userRole -> (userRole.getRole().getId().equals(accessRole.getId()) &&
                        userRole.getAuthority().hasEqualOrHigherRights(authority)) ||
                        (userRole.hasAccessToApplication(accessRole) &&
                                userRole.getAuthority().hasEqualOrHigherRights(Authority.INSTITUTION_ADMIN)))
                .findFirst()
                .orElseThrow(() -> new UserRestrictionException(String.format("User %s has no access to role %s",
                        user.getEmail(), accessRole.getName())));
    }

    //Does one of the userRoles has Authority.MANAGE and has the same application as the role
    private static boolean mayInviteByApplication(Set<UserRole> userRoles, Authority intendedAuthority, Role role) {
        return userRoles.stream()
                .anyMatch(userRole -> userRole.hasAccessToApplication(role) &&
                        userRole.getAuthority().hasEqualOrHigherRights(Authority.MANAGER) &&
                        userRole.getAuthority().hasEqualOrHigherRights(intendedAuthority));
    }

    //Does one of the userRoles have at least the Authority higher than the intendedAuthority and NOT Guest
    private static boolean mayInviteByAuthority(Set<UserRole> userRoles, Authority intendedAuthority, Role role) {
        return userRoles.stream()
                .anyMatch(userRole -> userRole.getAuthority().hasHigherRights(intendedAuthority) &&
                        userRole.getAuthority().hasEqualOrHigherRights(Authority.INVITER) &&
                        userRole.getRole().getId().equals(role.getId()));
    }

}
