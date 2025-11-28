package invite.cron;


import invite.model.Role;
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

import javax.sql.DataSource;
import java.io.Serializable;
import java.time.Instant;
import java.time.Period;
import java.util.List;
import java.util.Map;

@Component
public class ResourceCleaner extends AbstractNodeLeader {

    public static final String LOCK_NAME = "resource_cleaner_user_level_lock";
    private static final Log LOG = LogFactory.getLog(ResourceCleaner.class);

    private final UserRepository userRepository;
    private final ProvisioningService provisioningService;
    private final UserRoleRepository userRoleRepository;
    private final int lastActivityDurationDays;

    @Autowired
    public ResourceCleaner(UserRepository userRepository,
                           UserRoleRepository userRoleRepository,
                           ProvisioningService provisioningService,
                           DataSource dataSource,
                           @Value("${cron.last-activity-duration-days}") int lastActivityDurationDays) {
        super(LOCK_NAME, dataSource);
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.lastActivityDurationDays = lastActivityDurationDays;
        this.provisioningService = provisioningService;
    }

    @Scheduled(cron = "${cron.user-cleaner-expression}")
    @Transactional
    public void clean() {
        super.perform("ResourceCleaner#clean", () -> doClean());
    }

    public Map<String, List<? extends Serializable>> doClean() {
        List<User> users = cleanNonActiveUsers();
        List<User> orphans = cleanOrphanedUser();
        List<UserRole> userRoles = cleanUserRoles();
        return Map.of(
                "DeletedNonActiveUsers", users,
                "DeletedOrphanUsers", orphans,
                "DeletedExpiredUserRoles", userRoles
        );
    }

    private List<User> cleanNonActiveUsers() {
        Instant past = Instant.now().minus(Period.ofDays(lastActivityDurationDays));
        List<User> users = userRepository.findByLastActivityBefore(past);
        this.doDeleteUsers(users, "Deleted user %s with no recent activity");
        return users;
    }

    private List<User> cleanOrphanedUser() {
        List<User> orphans = userRepository.findNonSuperUserWithoutUserRoles();
        this.doDeleteUsers(orphans, "Deleted non-super user %s with no roles");
        return orphans;
    }

    private void doDeleteUsers(List<User> users, String logMessage) {
        users.forEach(user -> {
            try {
                provisioningService.deleteUserRequest(user);
                userRepository.delete(user);

                LOG.info(String.format(logMessage, user.getEmail()));
            } catch (RuntimeException e) {
                LOG.error(String.format("Error in provisioningService#deleteUserRequest for user %s", user.getEmail()), e);
            }
        });
    }

    private List<UserRole> cleanUserRoles() {
        List<UserRole> userRoles = userRoleRepository.findByEndDateBeforeAndExpiryNotifications(Instant.now(), 1);
        userRoles.forEach(userRole -> {
            User user = userRole.getUser();
            Role role = userRole.getRole();
            try {
                provisioningService.updateGroupRequest(userRole, OperationType.Remove);
                user.removeUserRole(userRole);
                userRepository.save(user);
                LOG.info(String.format("Deleted userRole for user %s and role %s with an endDate in the past",
                        user.getEmail(),
                        role.getName()));
            } catch (RuntimeException e) {
                LOG.error(String.format("Error in provisioningService#updateGroupRequest for user %s and userRole %s",
                        user.getEmail(), role.getName()), e);
            }
        });
        return userRoles;
    }

}
