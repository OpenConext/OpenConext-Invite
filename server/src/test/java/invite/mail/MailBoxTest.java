package invite.mail;

import invite.AbstractMailTest;
import invite.manage.EntityType;
import invite.model.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MailBoxTest extends AbstractMailTest {

    @Autowired
    private MailBox mailBox;

    @Test
    void sendInviteMail() {
        String htmlContent = doSendInviteMail(true, Authority.INVITER);

        assertTrue(htmlContent.contains("Wiki EN"));
        assertTrue(htmlContent.contains("SURF bv"));
        assertTrue(htmlContent.contains("For access to these applications we use SURFconext"));
        assertFalse(htmlContent.contains("For access to these applications eduID is used"));
    }

    @Test
    void sendInviteMailForEduIDOnly() {
        String htmlContent = doSendInviteMail(true, Authority.GUEST);

        assertTrue(htmlContent.contains("Wiki EN"));
        assertTrue(htmlContent.contains("SURF bv"));
        assertFalse(htmlContent.contains("For access to these applications we use SURFconext"));
        assertTrue(htmlContent.contains("For access to these applications eduID is used"));
    }

    private String doSendInviteMail(boolean eduIDOnly, Authority intendedAuthority) {
        User user = new User(false, "eppn", "sub", "example.com", "John", "Doe", "jdoe@example.com");
        Invitation invitation = new Invitation(intendedAuthority,
                "hash",
                "nope@ex.com",
                false,
                eduIDOnly,
                false,
                "Please join..",
                Language.en,
                user,
                Instant.now().plus(30, ChronoUnit.DAYS),
                Instant.now().plus(365, ChronoUnit.DAYS),
                Set.of(new InvitationRole(new Role("name", "desc", application("1", EntityType.SAML20_SP), 365, false, false)),
                        new InvitationRole(new Role("name", "desc", application("1", EntityType.SAML20_SP), 365, false, false))));
        mailBox.sendInviteMail(user, invitation, List.of(
                new GroupedProviders(
                        localManage.providerById(EntityType.SAML20_SP, "1"),
                        invitation.getRoles().stream().map(InvitationRole::getRole).toList(),
                        UUID.randomUUID().toString()),
                new GroupedProviders(
                        localManage.providerById(EntityType.SAML20_SP, "2"),
                        invitation.getRoles().stream().map(InvitationRole::getRole).toList(),
                        UUID.randomUUID().toString())
        ), Language.en, Optional.empty());
        MimeMessageParser mimeMessageParser = super.mailMessage();
        return mimeMessageParser.getHtmlContent();
    }


}