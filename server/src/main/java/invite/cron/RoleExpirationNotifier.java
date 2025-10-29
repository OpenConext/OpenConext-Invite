package invite.cron;

import invite.mail.MailBox;
import invite.manage.Manage;
import invite.model.GroupedProviders;
import invite.model.UserRole;
import invite.repository.UserRoleRepository;
import lombok.Getter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class RoleExpirationNotifier {

    private static final Log LOG = LogFactory.getLog(RoleExpirationNotifier.class);

    private final UserRoleRepository userRoleRepository;
    @Getter
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
        this.doSweep();
    }

    public List<String> doSweep() {
        Instant instant = Instant.now().plus(roleExpirationNotificationDays, ChronoUnit.DAYS);
        List<UserRole> userRoles = userRoleRepository.findByEndDateBeforeAndExpiryNotifications(instant, 0);
        return userRoles.stream().map(userRole -> {
            List<GroupedProviders> groupedProviders = manage.getGroupedProviders(List.of(userRole.getRole()));
            GroupedProviders groupedProvider = groupedProviders.isEmpty() ? null : groupedProviders.get(0);
            Instant endDate = userRole.getEndDate();
            long daysBetween = ChronoUnit.DAYS.between(endDate, instant);
            String mail = mailBox.sendUserRoleExpirationNotificationMail(userRole, groupedProvider, roleExpirationNotificationDays);
            //https://stackoverflow.com/a/75121707
            userRoleRepository.updateExpiryNotifications(1, userRole.getId());

            LOG.info("Send expiration notification mail to " + userRole.getUser().getEmail());

            return mail;
        }).toList();
    }

}
