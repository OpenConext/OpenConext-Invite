package access.cron;

import access.AbstractMailTest;
import access.mail.MimeMessageParser;
import access.manage.EntityType;
import access.model.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

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
        assertTrue(htmlContent.contains("Your Inviter role Mail at the application Calendar EN will expire"));

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