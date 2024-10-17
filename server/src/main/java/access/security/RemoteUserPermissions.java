package access.security;

import access.exception.UserRestrictionException;
import access.model.Application;
import access.model.Role;
import org.apache.commons.lang3.stream.Streams;

import java.util.List;

public class RemoteUserPermissions {

    private RemoteUserPermissions() {
    }

    public static void assertScopeAccess(RemoteUser remoteUser, Scope... scopes) {
        if (remoteUser == null) {
            throw new UserRestrictionException();
        }
        if (scopes == null || scopes.length == 0) {
            return;
        }
        if (!Streams.of(scopes).allMatch(scope -> remoteUser.getScopes().contains(scope))) {
            throw new UserRestrictionException();
        }
    }

    public static void assertApplicationAccess(RemoteUser remoteUser, Role role) {
        if (remoteUser == null) {
            throw new UserRestrictionException();
        }
        List<Application> remoteUserApplications = remoteUser.getApplications();
        boolean hasApplicationAccess = role.applicationsUsed().stream()
                .allMatch(application -> remoteUserApplications.stream()
                        .anyMatch(remoteUserApplication -> remoteUserApplication.getManageId().equals(application.getManageId())
                                && remoteUserApplication.getManageType().equals(application.getManageType())));
        if (!hasApplicationAccess) {
            throw new UserRestrictionException();
        }
    }
}
