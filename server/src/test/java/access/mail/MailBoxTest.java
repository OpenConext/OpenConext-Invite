package access.mail;

import access.AbstractMailTest;
import access.manage.EntityType;
import access.model.*;
import jakarta.mail.BodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MailBoxTest extends AbstractMailTest {

    @Autowired
    private MailBox mailBox;

    @Test
    void sendInviteMail() throws Exception {
        User user = new User(false, "eppn", "sub", "John", "Doe", "jdoe@example.com");
        Invitation invitation = new Invitation(Authority.GUEST,
                "hash",
                "nope@ex.com",
                false, user,
                Set.of(new InvitationRole(new Role("name", "desc", "1", EntityType.SAML20_SP)),
                        new InvitationRole(new Role("name", "desc", "1", EntityType.SAML20_SP))));
        mailBox.sendInviteMail(user, invitation, List.of(
                new GroupedProviders(
                        localManage.providerById(EntityType.SAML20_SP, "1"),
                        invitation.getRoles().stream().map(role -> role.getRole()).toList(),
                        UUID.randomUUID().toString()),
                new GroupedProviders(
                        localManage.providerById(EntityType.SAML20_SP, "2"),
                        invitation.getRoles().stream().map(role -> role.getRole()).toList(),
                        UUID.randomUUID().toString())
        ));
        MimeMessageParser mimeMessageParser = super.mailMessage();
        String htmlContent = mimeMessageParser.getHtmlContent();

        System.out.println(htmlContent);
    }


}