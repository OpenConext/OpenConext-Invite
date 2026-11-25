package invite.security;

import invite.exception.UserRestrictionException;
import invite.manage.ManageIdentifier;
import invite.model.Application;
import invite.model.Authority;
import invite.model.Invitation;
import invite.model.InvitationApplication;
import invite.model.InvitationRole;
import invite.model.Role;
import org.apache.commons.lang3.stream.Streams;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

public class RemoteUserPermissions {

    private RemoteUserPermissions() {
    }

    public static void assertScopeAccess(RemoteUser remoteUser, Scope... scopes) {
        if (remoteUser == null) {
            throw new UserRestrictionException("Remote user is NULL");
        }
        if (scopes == null || scopes.length == 0) {
            return;
        }
        if (!Streams.of(scopes).allMatch(scope -> remoteUser.getScopes().contains(scope))) {
            throw new UserRestrictionException(String.format("Scope %s not allowd for remoteUsser %s",
                    Arrays.toString(scopes), remoteUser.getName()));
        }
    }

    /**
     * Remote users without the access scope are only allowed to invite for the applications they own.
     * Invitations that grant (institution) wide authority are reserved for the access scope.
     */
    public static void assertInvitationAuthority(RemoteUser remoteUser, Authority intendedAuthority, List<ManageIdentifier> manageIdentifiers) {
        if (remoteUser == null) {
            throw new UserRestrictionException("Remote user is NULL");
        }
        if (hasUnrestrictedAccess(remoteUser)) {
            return;
        }
        if (Authority.SUPER_USER.equals(intendedAuthority) || Authority.INSTITUTION_ADMIN.equals(intendedAuthority)) {
            throw new UserRestrictionException(String.format("RemoteUser %s is not allowed to invite for authority %s",
                    remoteUser.getName(), intendedAuthority));
        }
        //An empty collection matches vacuously
        boolean missingApplications = Authority.APPLICATION_MANAGER.equals(intendedAuthority) &&
                CollectionUtils.isEmpty(manageIdentifiers);
        //Regardless of the authority, as Applications are created run-time for the manageIdentifiers
        if (missingApplications || !ownsAllApplications(remoteUser, manageIdentifiers)) {
            throw new UserRestrictionException(String.format("RemoteUser %s has no access to all requested applications",
                    remoteUser.getName()));
        }
    }

    /**
     * Access to the roles of an invitation. Only invitations for application managers are allowed to have no roles, access
     * to the applications of those invitations is asserted in {@link #assertInvitationAuthority}
     */
    public static void assertInvitationRolesAccess(RemoteUser remoteUser, Authority intendedAuthority, List<Role> roles) {
        if (Authority.APPLICATION_MANAGER.equals(intendedAuthority) && CollectionUtils.isEmpty(roles)) {
            return;
        }
        assertApplicationAccess(remoteUser, roles);
    }

    /**
     * Access to an existing invitation
     */
    public static void assertInvitationAccess(RemoteUser remoteUser, Invitation invitation) {
        List<ManageIdentifier> manageIdentifiers = invitation.getApplications().stream()
                .map(InvitationApplication::getApplication)
                .map(application -> new ManageIdentifier(application.getManageId(), application.getManageType()))
                .toList();
        List<Role> roles = invitation.getRoles().stream().map(InvitationRole::getRole).toList();
        assertInvitationAuthority(remoteUser, invitation.getIntendedAuthority(), manageIdentifiers);
        assertInvitationRolesAccess(remoteUser, invitation.getIntendedAuthority(), roles);
    }

    /**
     * Remote users without the access scope may only use their own organization. The organizationGUID is optional.
     */
    public static void assertOrganizationAccess(RemoteUser remoteUser, String organizationGUID) {
        if (remoteUser == null) {
            throw new UserRestrictionException("Remote user is NULL");
        }
        if (hasUnrestrictedAccess(remoteUser) || !StringUtils.hasText(organizationGUID)) {
            return;
        }
        if (!organizationGUID.equals(remoteUser.getOrganizationGUIDFallback())) {
            throw new UserRestrictionException(String.format("RemoteUser %s has no access to organization %s",
                    remoteUser.getName(), organizationGUID));
        }
    }

    public static void assertApplicationAccess(RemoteUser remoteUser, Role role) {
        assertApplicationAccess(remoteUser, List.of(role));
    }

    public static void assertApplicationAccess(RemoteUser remoteUser, List<Role> roles) {
        if (remoteUser == null) {
            throw new UserRestrictionException("Remote user is NULL");
        }
        if (hasUnrestrictedAccess(remoteUser)) {
            return;
        }
        //Empty collections match vacuously, so both the roles and the applications of each role must be present
        boolean hasApplicationAccess = !roles.isEmpty() && roles.stream()
                .map(Role::applicationsUsed)
                .allMatch(applications -> !applications.isEmpty() &&
                        new HashSet<>(remoteUser.getApplications()).containsAll(applications));
        if (!hasApplicationAccess) {
            throw new UserRestrictionException(String.format("RemoteUser %s has no access to roles %s",
                    remoteUser.getName(), roles.stream().map(role -> role.getName()).collect(Collectors.joining(", "))));
        }
    }

    private static boolean hasUnrestrictedAccess(RemoteUser remoteUser) {
        return remoteUser.isLocalDevMode() || remoteUser.getScopes().contains(Scope.access);
    }

    private static boolean ownsAllApplications(RemoteUser remoteUser, List<ManageIdentifier> manageIdentifiers) {
        return CollectionUtils.isEmpty(manageIdentifiers) || manageIdentifiers.stream()
                .map(identifier -> new Application(identifier.manageId(), identifier.manageType()))
                .allMatch(remoteUser.getApplications()::contains);
    }

}

