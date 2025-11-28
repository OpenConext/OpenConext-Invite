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

import javax.sql.DataSource;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class RoleExpirationNotifier extends AbstractNodeLeader {

    public static final String LOCK_NAME = "role_expiration_notifier_user_level_lock";
    private static final Log LOG = LogFactory.getLog(RoleExpirationNotifier.class);

    private final UserRoleRepository userRoleRepository;
    @Getter
    private final Manage manage;
    private final MailBox mailBox;

    private final int roleExpirationNotificationDays;

    public RoleExpirationNotifier(UserRoleRepository userRoleRepository,
                                  Manage manage,
                                  MailBox mailBox,
                                  DataSource dataSource,
                                  @Value("${cron.role-expiration-notifier-duration-days}") int roleExpirationNotificationDays) {
        super(LOCK_NAME, dataSource);
        this.userRoleRepository = userRoleRepository;
        this.manage = manage;
        this.mailBox = mailBox;
        this.roleExpirationNotificationDays = roleExpirationNotificationDays;
    }

    @Scheduled(cron = "${cron.role-expiration-notifier-expression}")
    @Transactional
    public void sweep() {
        if (roleExpirationNotificationDays == -1) {
            return;
        }
        super.perform("RoleExpirationNotifier#sweep", () -> this.doSweep());
    }

    public List<String> doSweep() {
        Instant now = Instant.now();
        Instant futureDate = now.plus(roleExpirationNotificationDays, ChronoUnit.DAYS);
        List<UserRole> userRoles = userRoleRepository.findByEndDateBeforeAndExpiryNotifications(futureDate, 0);
        return userRoles.stream().map(userRole -> {
            List<GroupedProviders> groupedProviders = manage.getGroupedProviders(List.of(userRole.getRole()));
            GroupedProviders groupedProvider = groupedProviders.isEmpty() ? null : groupedProviders.get(0);
            Instant endDate = userRole.getEndDate();
            long daysBetween = ChronoUnit.DAYS.between(now, endDate) + 1;
            String mail = mailBox.sendUserRoleExpirationNotificationMail(userRole, groupedProvider, (int) daysBetween);
            //https://stackoverflow.com/a/75121707
            userRoleRepository.updateExpiryNotifications(1, userRole.getId());

            LOG.info("Send expiration notification mail to " + userRole.getUser().getEmail());

            return mail;
        }).toList();
    }

}
