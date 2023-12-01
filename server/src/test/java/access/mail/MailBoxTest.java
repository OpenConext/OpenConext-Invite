package access.mail;

import access.AbstractMailTest;
import access.Seed;
import access.manage.EntityType;
import access.model.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MailBoxTest extends AbstractMailTest {

    @Autowired
    private MailBox mailBox;

    @Test
    void sendInviteMail() {
        User user = new User(false, "eppn", "sub", "example.com", "John", "Doe", "jdoe@example.com");
        Invitation invitation = new Invitation(Authority.GUEST,
                "hash",
                "nope@ex.com",
                false,
                false,
                false,
                "Please join..",
                user,
                Instant.now().plus(30, ChronoUnit.DAYS),
                Instant.now().plus(365, ChronoUnit.DAYS),
                Set.of(new InvitationRole(new Role("name", "desc", "https://landingpage.com", Seed.application("1", EntityType.SAML20_SP), 365, false, false)),
                        new InvitationRole(new Role("name", "desc", "https://landingpage.com",Seed.application("1", EntityType.SAML20_SP), 365, false, false))));
        mailBox.sendInviteMail(user, invitation, List.of(
                new GroupedProviders(
                        localManage.providerById(EntityType.SAML20_SP, "1"),
                        invitation.getRoles().stream().map(InvitationRole::getRole).toList(),
                        UUID.randomUUID().toString()),
                new GroupedProviders(
                        localManage.providerById(EntityType.SAML20_SP, "2"),
                        invitation.getRoles().stream().map(InvitationRole::getRole).toList(),
                        UUID.randomUUID().toString())
        ));
        MimeMessageParser mimeMessageParser = super.mailMessage();
        String htmlContent = mimeMessageParser.getHtmlContent();

        assertTrue(htmlContent.contains("Wiki EN"));
        assertTrue(htmlContent.contains("SURF bv"));
    }


}