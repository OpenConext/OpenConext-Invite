package invite.security;

import invite.exception.UserRestrictionException;
import invite.model.Application;
import invite.model.Role;
import org.apache.commons.lang3.stream.Streams;

import java.util.Arrays;
import java.util.Collection;
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

    public static void assertApplicationAccess(RemoteUser remoteUser, Role role) {
        assertApplicationAccess(remoteUser, List.of(role));
    }

    public static void assertApplicationAccess(RemoteUser remoteUser, List<Role> roles) {
        if (remoteUser == null) {
            throw new UserRestrictionException("Remote user is NULL");
        }
        if (remoteUser.isLocalDevMode()) {
            return;
        }
        List<Application> remoteUserApplications = remoteUser.getApplications();
        boolean hasApplicationAccess = roles.stream().map(role -> role.applicationsUsed())
                .flatMap(Collection::stream)
                .allMatch(application -> remoteUserApplications.stream()
                        .anyMatch(remoteUserApplication -> remoteUserApplication.getManageId().equals(application.getManageId())
                                && remoteUserApplication.getManageType().equals(application.getManageType())));
        if (!hasApplicationAccess) {
            throw new UserRestrictionException(String.format("RemoteUser %s has no access to roles %s",
                    remoteUser.getName(), roles.stream().map(role -> role.getName()).collect(Collectors.joining(", "))));
        }
    }

}

