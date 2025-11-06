package invite.model;

import invite.WithApplicationTest;
import invite.manage.EntityType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class InvitationTest extends WithApplicationTest {

    @Test
    void constructorWithoutDefaults() {
        Role role = new Role("mail", "description", application("1", EntityType.SAML20_SP), 30, false, false);

        Invitation invitation = new Invitation(Authority.GUEST, "hash", "john@example.com", false, false, false, "Please join..", Language.en, new User(),
                null, Instant.now().plus(30, ChronoUnit.DAYS),
                Set.of(new InvitationRole(role)), null);

        assertEquals(13, Instant.now().until(invitation.getExpiryDate(), ChronoUnit.DAYS));
        assertEquals(29, Instant.now().until(invitation.getRoleExpiryDate(), ChronoUnit.DAYS));
    }

    @Test
    void constructorWithDefaults() {
        Role role = new Role("mail", "description", application("1", EntityType.SAML20_SP), null, false, false);

        Invitation invitation = new Invitation(Authority.MANAGER, "hash", "john@example.com", false, false, false, "Please join..", Language.en, new User(),
                null, null, Set.of(new InvitationRole(role)), null);
        assertEquals(13, Instant.now().until(invitation.getExpiryDate(), ChronoUnit.DAYS));
        assertNull(invitation.getRoleExpiryDate());
    }

    @Test
    void roleExpiryDate() {
        Role role = new Role("mail", "description", application("1", EntityType.SAML20_SP), 30, false, false);

        Invitation invitation = new Invitation(Authority.GUEST, "hash", "john@example.com",
                false, false, false, "Please join..", Language.en, new User(),
                null, null,
                Set.of(new InvitationRole(role)),
                UUID.randomUUID().toString());

        assertEquals(29, Instant.now().until(invitation.getRoleExpiryDate(), ChronoUnit.DAYS));
    }

    @Test
    void inviterEmail() {
        Invitation invitation = new Invitation();
        assertEquals(0, invitation.getInviterEmail().size());
        User inviter = new User();
        inviter.setId(1L);
        inviter.setEmail("jdoe@example.com");
        invitation.setInviter(inviter);
        assertEquals(inviter.getEmail(), invitation.getInviterEmail().get("name"));
        inviter.setName("John Doe");
        assertEquals(inviter.getName(), invitation.getInviterEmail().get("name"));
    }

}