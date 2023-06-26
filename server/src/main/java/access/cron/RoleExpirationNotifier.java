package access.cron;

import access.api.HasManage;
import access.mail.MailBox;
import access.manage.Manage;
import access.model.GroupedProviders;
import access.model.UserRole;
import access.repository.UserRoleRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class RoleExpirationNotifier implements HasManage {

    private static final Log LOG = LogFactory.getLog(ResourceCleaner.class);

    private final UserRoleRepository userRoleRepository;
    private final Manage manage;
    private final MailBox mailBox;

    private final boolean cronJobResponsible;
    private final int roleExpirationNotificationDays;

    public RoleExpirationNotifier(UserRoleRepository userRoleRepository,
                                  Manage manage,
                                  MailBox mailBox,
                                  @Value("${cron.node-cron-job-responsible}") boolean cronJobResponsible,
                                  @Value("${cron.role-expiration-notifier-duration-days}") int roleExpirationNotificationDays) {
        this.userRoleRepository = userRoleRepository;
        this.manage = manage;
        this.mailBox = mailBox;
        this.cronJobResponsible = cronJobResponsible;
        this.roleExpirationNotificationDays = roleExpirationNotificationDays;
    }

    @Scheduled(cron = "${cron.role-expiration-notifier-expression}")
    @Transactional
    public void sweep() {
        if (!cronJobResponsible || roleExpirationNotificationDays == -1) {
            return;
        }
        Instant instant = Instant.now().plus(roleExpirationNotificationDays, ChronoUnit.DAYS);
        List<UserRole> userRoles = userRoleRepository.findByEndDateBeforeAndExpiryNotifications(instant, 0);
        userRoles.forEach(userRole -> {
            List<GroupedProviders> groupedProviders = getGroupedProviders(List.of(userRole.getRole()));
            GroupedProviders groupedProvider = groupedProviders.isEmpty() ? null : groupedProviders.get(0);
            mailBox.sendUserRoleExpirationNotificationMail(userRole, groupedProvider, roleExpirationNotificationDays);
            userRole.setExpiryNotifications(1);
            userRoleRepository.save(userRole);

            LOG.info("Send expiration notification mail to " + userRole.getUser().getEmail());
        });

    }

    @Override
    public Manage getManage() {
        return manage;
    }
}
