package access.cron;


import access.model.User;
import access.model.UserRole;
import access.repository.UserRepository;
import access.repository.UserRoleRepository;
import access.provision.scim.OperationType;
import access.provision.ProvisioningService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.Period;
import java.util.List;
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
        cleanNonActiveUsers();
        cleanOrphanedUser();
        cleanUserRoles();
    }

    private void cleanNonActiveUsers() {
        Instant past = Instant.now().minus(Period.ofDays(lastActivityDurationDays));
        List<User> users = userRepository.findByLastActivityBefore(past);

        LOG.info(String.format("Deleted %s users with no activity in the last %s days: %s ",
                users.size(),
                lastActivityDurationDays,
                users.stream().map(User::getEduPersonPrincipalName).collect(Collectors.joining(", "))));

        users.forEach(provisioningService::deleteUserRequest);
        userRepository.deleteAll(users);
    }

    private void cleanOrphanedUser() {
        List<User> orphans = userRepository.findNonSuperUserWithoutUserRoles();

        LOG.info(String.format("Deleted %s non-super users with no userRoles; %s",
                orphans.size(),
                orphans.stream().map(User::getEduPersonPrincipalName).collect(Collectors.joining(", "))));

        orphans.forEach(provisioningService::deleteUserRequest);
        userRepository.deleteAll(orphans);
    }

    private void cleanUserRoles() {
        List<UserRole> userRoles = userRoleRepository.findByEndDateBefore(Instant.now());

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


    }

}
