package invite.cron;

import invite.AbstractMailTest;
import invite.mail.MimeMessageParser;
import invite.manage.EntityType;
import invite.model.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoleExpirationNotifierTest extends AbstractMailTest {

    @Autowired
    private RoleExpirationNotifier roleExpirationNotifier;

    @Test
    void sweep() {
        UserRole userRole = userRoleRepository.findByRoleName("Mail").get(0);
        userRole.setEndDate(Instant.now().minus(7, ChronoUnit.DAYS));
        super.stubForManageProviderById(EntityType.OIDC10_RP, "5");

        userRoleRepository.save(userRole);

        roleExpirationNotifier.sweep();

        MimeMessageParser messageParser = super.mailMessage();
        String htmlContent = messageParser.getHtmlContent();
        //Due to HTML formatting, we can't be sure of the line breaks
        Stream.of("Your Inviter role Mail at the application Calendar EN will expire in 5 days".split(" "))
                .forEach(s -> assertTrue(htmlContent.contains(s)));

        userRole = userRoleRepository.findByRoleName("Mail").get(0);
        assertEquals(1, userRole.getExpiryNotifications());

        Instant instant = Instant.now().plus(100, ChronoUnit.DAYS);
        List<UserRole> userRoles = userRoleRepository.findByEndDateBeforeAndExpiryNotifications(instant, 0);
        assertEquals(0, userRoles.size());
    }

    @Test
    void sweepNonExistentGroupProvider() {
        UserRole userRole = userRoleRepository.findByRoleName("Mail").get(0);
        userRole.setEndDate(Instant.now().minus(7, ChronoUnit.DAYS));
        //Mock 404 from Manage
        super.stubForManageProviderByIdNotFound(EntityType.OIDC10_RP, "5");

        userRoleRepository.save(userRole);

        roleExpirationNotifier.sweep();

        MimeMessageParser messageParser = super.mailMessage();
        String htmlContent = messageParser.getHtmlContent();
        // Assert that an NULL groupedProvider is handled correctly
        assertTrue(htmlContent.contains("Your Inviter role Mail will expire"));
    }

    @Test
    void noCronJobResponsible() {
        RoleExpirationNotifier subject = new RoleExpirationNotifier(null, null, null, false, 5);
        subject.sweep();
        subject = new RoleExpirationNotifier(null, null, null, true, -1);
        subject.sweep();
    }
}