package access.model;

import access.manage.EntityType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class InvitationTest {

    @Test
    void constructorWithoutDefaults() {
        Role role = new Role("mail", "description", "https://landingpage.com", "1", EntityType.SAML20_SP, 30);
        InvitationRole invitationRole = new InvitationRole(role);
        assertNotNull(invitationRole.getEndDate());

        Invitation invitation = new Invitation(Authority.INVITER, "hash", "john@example.com", false, new User(), Set.of(invitationRole));
        assertEquals(13, Instant.now().until(invitation.getExpiryDate(), ChronoUnit.DAYS));
        assertEquals(29, Instant.now().until(invitation.getRoleExpiryDate(), ChronoUnit.DAYS));
    }

    @Test
    void constructorWithDefaults() {
        Role role = new Role("mail", "description", "https://landingpage.com", "1", EntityType.SAML20_SP, null);
        InvitationRole invitationRole = new InvitationRole(role);
        assertNotNull(invitationRole.getEndDate());

        Invitation invitation = new Invitation(Authority.INVITER, "hash", "john@example.com", false, new User(), Set.of(invitationRole));
        assertEquals(13, Instant.now().until(invitation.getExpiryDate(), ChronoUnit.DAYS));
        assertEquals(364, Instant.now().until(invitation.getRoleExpiryDate(), ChronoUnit.DAYS));
    }

    @Test
    void constructorWithMinimalExpirationDate() {
        Role mail = new Role("mail", "description", "https://landingpage.com", "1", EntityType.SAML20_SP, 100);
        Role wiki = new Role("wiki", "description", "https://landingpage.com", "1", EntityType.SAML20_SP, 50);
        InvitationRole mailInvitationRole = new InvitationRole(mail);
        InvitationRole wikiInvitationRole = new InvitationRole(wiki);

        Invitation invitation = new Invitation(Authority.INVITER, "hash", "john@example.com", false, new User(),
                Set.of(mailInvitationRole, wikiInvitationRole));
        assertEquals(13, Instant.now().until(invitation.getExpiryDate(), ChronoUnit.DAYS));
        assertEquals(49, Instant.now().until(invitation.getRoleExpiryDate(), ChronoUnit.DAYS));

        mailInvitationRole.setEndDate(null);
        wikiInvitationRole.setEndDate(null);

        invitation = new Invitation(Authority.INVITER, "hash", "john@example.com", false, new User(),
                Set.of(mailInvitationRole, wikiInvitationRole));
        assertEquals(49, Instant.now().until(invitation.getRoleExpiryDate(), ChronoUnit.DAYS));

        mail.setDefaultExpiryDays(null);
        wiki.setDefaultExpiryDays(null);

        invitation = new Invitation(Authority.INVITER, "hash", "john@example.com", false, new User(),
                Set.of(mailInvitationRole, wikiInvitationRole));
        assertEquals(364, Instant.now().until(invitation.getRoleExpiryDate(), ChronoUnit.DAYS));
    }

}