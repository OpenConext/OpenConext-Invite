package invite.cron;


import invite.model.User;
import invite.model.UserRole;
import invite.provision.ProvisioningService;
import invite.provision.scim.OperationType;
import invite.repository.UserRepository;
import invite.repository.UserRoleRepository;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.time.Instant;
import java.time.Period;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ResourceCleaner {

    private static final Log LOG = LogFactory.getLog(ResourceCleaner.class);

    private final UserRepository userRepository;
    private final ProvisioningService provisioningService;
    private final UserRoleRepository userRoleRepository;
    private final boolean cronJobResponsible;
    private final int lastActivityDurationDays;

    @Autowired
    public ResourceCleaner(UserRepository userRepository,
                           UserRoleRepository userRoleRepository,
                           ProvisioningService provisioningService,
                           @Value("${cron.last-activity-duration-days}") int lastActivityDurationDays,
                           @Value("${cron.node-cron-job-responsible}") boolean cronJobResponsible) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.lastActivityDurationDays = lastActivityDurationDays;
        this.cronJobResponsible = cronJobResponsible;
        this.provisioningService = provisioningService;
    }

    @Scheduled(cron = "${cron.user-cleaner-expression}")
    @Transactional
    public void clean() {
        if (!cronJobResponsible) {
            return;
        }
        doClean();
    }

    public Map<String, List<? extends Serializable>> doClean() {
        List<User> users = cleanNonActiveUsers();
        List<User> orphans = cleanOrphanedUser();
        List<UserRole> userRoles = cleanUserRoles();
        return Map.of("DeletedNonActiveUsers", users,
                "DeletedOrphanUsers", orphans,
                "DeletedExpiredUserRoles", userRoles);
    }

    private List<User> cleanNonActiveUsers() {
        Instant past = Instant.now().minus(Period.ofDays(lastActivityDurationDays));
        List<User> users = userRepository.findByLastActivityBefore(past);

        LOG.info(String.format("Deleted %s users with no activity in the last %s days: %s ",
                users.size(),
                lastActivityDurationDays,
                users.stream().map(User::getEduPersonPrincipalName).collect(Collectors.joining(", "))));

        users.forEach(provisioningService::deleteUserRequest);
        userRepository.deleteAll(users);

        return users;
    }

    private List<User> cleanOrphanedUser() {
        List<User> orphans = userRepository.findNonSuperUserWithoutUserRoles();

        LOG.info(String.format("Deleted %s non-super users with no userRoles; %s",
                orphans.size(),
                orphans.stream().map(User::getEduPersonPrincipalName).collect(Collectors.joining(", "))));

        orphans.forEach(provisioningService::deleteUserRequest);
        userRepository.deleteAll(orphans);

        return orphans;
    }

    private List<UserRole> cleanUserRoles() {
        List<UserRole> userRoles = userRoleRepository.findByEndDateBeforeAndExpiryNotifications(Instant.now(), 1);

        LOG.info(String.format("Deleted %s userRoles with an endDate in the past: %s",
                userRoles.size(),
                userRoles.stream()
                        .map(userRole -> String.format("%s - %s", userRole.getUser().getEduPersonPrincipalName(), userRole.getRole().getName()))
                        .collect(Collectors.toList())));

        userRoles.forEach(userRole -> provisioningService.updateGroupRequest(userRole, OperationType.Remove));

        userRoles.forEach(userRole -> {
            User user = userRole.getUser();
            user.removeUserRole(userRole);
            userRepository.save(user);
        });
        return userRoles;

    }

}
