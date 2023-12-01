package access.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserRoleProvisioningTest {

    @Test
    void resolveSubWithEmail() {
        User user = new User(new UserRoleProvisioning(
                List.of(1L),
                Authority.GUEST,
                null,
                "jdoe@example.com",
                null,
                null,
                null,
                null,
                null,
                false
        ));
        assertEquals("urn:collab:person:example.com:jdoe", user.getSub());
        assertEquals("example.com", user.getSchacHomeOrganization());
    }

    @Test
    void resolveSubWithEPPN() {
        User user = new User(new UserRoleProvisioning(
                List.of(1L),
                Authority.GUEST,
                null,
                null,
                "jdoe@example.com",
                null,
                null,
                null,
                null,
                false
        ));
        assertEquals("urn:collab:person:example.com:jdoe", user.getSub());
        assertEquals("example.com", user.getSchacHomeOrganization());
    }

    @Test
    void resolveSubWithSub() {
        User user = new User(new UserRoleProvisioning(
                List.of(1L),
                Authority.GUEST,
                "urn:collab:person:example.com:jdoe",
                "mary@domain.com",
                null,
                null,
                null,
                null,
                "nice.org",
                false
        ));
        assertEquals("urn:collab:person:example.com:jdoe", user.getSub());
        assertEquals("nice.org", user.getSchacHomeOrganization());
    }

    @Test
    void resolveSubWithInvalidUserRoleProvisioning() {
        assertThrows(IllegalArgumentException.class, () -> new User(new UserRoleProvisioning(
                List.of(1L),
                Authority.GUEST,
                null,
                null,
                "nope",
                null,
                null,
                null,
                null,
                false
        )));
        assertThrows(IllegalArgumentException.class, () -> new User(new UserRoleProvisioning(
                List.of(1L),
                Authority.GUEST,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false
        )));
    }

}