package invite.cron;


import invite.audit.UserRoleAuditService;
import invite.model.Role;
import invite.model.User;
import invite.model.UserRole;
import invite.model.UserRoleAudit;
import invite.provision.ProvisioningService;
import invite.repository.InvitationRepository;
import invite.repository.UserRepository;
import invite.repository.UserRoleAuditRepository;
import invite.repository.UserRoleRepository;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
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
import java.util.Map;

@Component
public class ResourceCleaner {

    public static final String LOCK_NAME = "resource_cleaner_user_level_lock";
    private static final Log LOG = LogFactory.getLog(ResourceCleaner.class);

    private final UserRepository userRepository;
    private final UserRoleAuditRepository userRoleAuditRepository;
    private final ProvisioningService provisioningService;
    private final UserRoleRepository userRoleRepository;
    private final UserRoleAuditService userRoleAuditService;
    private final int lastActivityDurationDays;
    private final int purgeAuditLogDays;
    private final int purgeExpiredInvitationDays;
    private final InvitationRepository invitationRepository;


    @Autowired
    public ResourceCleaner(UserRepository userRepository,
                           UserRoleRepository userRoleRepository,
                           ProvisioningService provisioningService,
                           UserRoleAuditRepository userRoleAuditRepository,
                           UserRoleAuditService userRoleAuditService,
                           InvitationRepository invitationRepository,
                           @Value("${cron.last-activity-duration-days}") int lastActivityDurationDays,
                           @Value("${cron.purge-audit-log-days}") int purgeAuditLogDays,
                           @Value("${cron.purge-expired-invitations-days}") int purgeExpiredInvitationDays) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.userRoleAuditRepository = userRoleAuditRepository;
        this.userRoleAuditService = userRoleAuditService;
        this.lastActivityDurationDays = lastActivityDurationDays;
        this.provisioningService = provisioningService;
        this.purgeAuditLogDays = purgeAuditLogDays;
        this.purgeExpiredInvitationDays = purgeExpiredInvitationDays;
        this.invitationRepository = invitationRepository;
    }

    @Scheduled(fixedDelayString = "${cron.user-cleaner-cron}", initialDelayString = "${cron.user-cleaner-cron-initiaal-delay}")
    @SchedulerLock(name = LOCK_NAME, lockAtLeastFor = "${cron.user-cleaner-lock-at-least-for}",
            lockAtMostFor = "${cron.user-cleaner-lock-at-most-for}")
    @Transactional
    public void clean() {
        LOG.info("CRON: Cleaning resources");
        this.doClean();
    }

    public Map<String, Object> doClean() {
        List<User> users = cleanNonActiveUsers();
        List<User> orphans = cleanOrphanedUser();
        List<UserRole> userRoles = cleanUserRoles();
        return Map.of(
                "DeletedNonActiveUsers", users,
                "DeletedOrphanUsers", orphans,
                "DeletedExpiredUserRoles", userRoles,
                "DeletedUserRoleAudits", cleanUserRoleAudit(),
                "DeletedExpiredInvitations", cleanExpiredInvitation()
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
                userRoleAuditService.logAction(userRole, UserRoleAudit.ActionType.DELETE);
                provisioningService.deleteUserRoleRequest(userRole);
                userRoleRepository.deleteUserRoleById(userRole.getId());

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

    private int cleanUserRoleAudit() {
        if (purgeAuditLogDays == 0L) {
            return 0;
        }
        Instant past = Instant.now().minus(Period.ofDays(purgeAuditLogDays));
        return userRoleAuditRepository.deleteByCreatedAtBefore(past);
    }

    private int cleanExpiredInvitation() {
        if (purgeExpiredInvitationDays == 0L) {
            return 0;
        }
        Instant past = Instant.now().minus(Period.ofDays(purgeExpiredInvitationDays));
        return invitationRepository.deleteByExpiryDateBefore(past);
    }
}
