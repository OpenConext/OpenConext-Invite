package access.logging;

import access.model.*;
import org.apache.commons.logging.Log;
import org.slf4j.MDC;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AccessLogger {

    private AccessLogger() {
    }

    public static void role(Log log, Event event, User user, Role role) {
        MDC.setContextMap(Map.of(
                "type", String.format("%s Role", event),
                "userId", user.getSub(),
                "applications", applications(role),
                "roleId", role.getId().toString()
        ));
        log.info(String.format("%s role %s", event, role.getName()));
    }

    private static String applications(Role role) {
        return role.applicationsUsed().stream()
                .map(application -> String.format("%s %s", application.getManageType(), application.getManageId()))
                .collect(Collectors.joining(", "));
    }

    public static void userRole(Log log, Event event, User user, UserRole userRole) {
        Role role = userRole.getRole();
        MDC.setContextMap(Map.of(
                "type", String.format("%s UserRole", event),
                "userId", user.getSub(),
                "applications", applications(role),
                "roleId", role.getId().toString()
        ));
        log.info(String.format("%s userRole %s", event, role.getName()));
    }

    public static void invitation(Log log, Event event, Invitation invitation) {
        List<Role> roles = invitation.getRoles().stream().map(InvitationRole::getRole).toList();
        ;
        MDC.setContextMap(Map.of(
                "type", String.format("%s Invitation", event),
                "userId", invitation.getInviter().getSub(),
                "applications", roles.stream().map(AccessLogger::applications).collect(Collectors.joining(", ")),
                "roles", String.join(",", roles.stream().map(Role::getName).toList())
        ));
        log.info(String.format("%s invitation for %s", event, invitation.getEmail()));
    }

    public static void user(Log log, Event event, User user) {
        MDC.setContextMap(Map.of(
                "type", String.format("%s User", event),
                "userId", user.getSub()
        ));
        log.info(String.format("%s user with sub %s", event, user.getSub()));
    }
}
