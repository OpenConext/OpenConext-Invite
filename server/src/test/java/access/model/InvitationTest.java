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
        Role role = new Role("mail", "description", "https://landingpage.com", "1", EntityType.SAML20_SP, 30, false, false);

        Invitation invitation = new Invitation(Authority.GUEST, "hash", "john@example.com", false, false, "Please join..", new User(),
                null, Instant.now().plus(30, ChronoUnit.DAYS),
                Set.of(new InvitationRole(role)));

        assertEquals(13, Instant.now().until(invitation.getExpiryDate(), ChronoUnit.DAYS));
        assertEquals(29, Instant.now().until(invitation.getRoleExpiryDate(), ChronoUnit.DAYS));
    }

    @Test
    void constructorWithDefaults() {
        Role role = new Role("mail", "description", "https://landingpage.com", "1", EntityType.SAML20_SP, null, false, false);

        Invitation invitation = new Invitation(Authority.MANAGER, "hash", "john@example.com", false, false, "Please join..", new User(),
                null, null, Set.of(new InvitationRole(role)));
        assertEquals(13, Instant.now().until(invitation.getExpiryDate(), ChronoUnit.DAYS));
        assertNull(invitation.getRoleExpiryDate());
    }

    @Test
    void roleExpiryDate() {
        Role role = new Role("mail", "description", "https://landingpage.com", "1", EntityType.SAML20_SP, 30, false, false);

        Invitation invitation = new Invitation(Authority.GUEST, "hash", "john@example.com", false, false, "Please join..", new User(),
                null, null,
                Set.of(new InvitationRole(role)));

        assertEquals(29, Instant.now().until(invitation.getRoleExpiryDate(), ChronoUnit.DAYS));
    }

    @Test
    void inviterEmail() {
        Invitation invitation = new Invitation();
        assertEquals(0, invitation.getInviterEmail().size());
        User inviter = new User();
        inviter.setEmail("jdoe@example.com");
        invitation.setInviter(inviter);
        assertEquals(inviter.getEmail(), invitation.getInviterEmail().get("name"));
        inviter.setName("John Doe");
        assertEquals(inviter.getName(), invitation.getInviterEmail().get("name"));
    }

}