package invite.security;

import invite.manage.EntityType;
import invite.model.Application;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExtendedInMemoryUserDetailsManagerTest {

    private final RemoteUser remoteUser = new RemoteUser(
            "user",
            "password",
            "SP Dashboard",
            "ad93daef-0911-e511-80d0-005056956c1a",
            List.of(Scope.profile),
            List.of(new Application("4", EntityType.SAML20_SP)),
            false);

    private final ExtendedInMemoryUserDetailsManager userDetailsManager =
            new ExtendedInMemoryUserDetailsManager(List.of(remoteUser));

    @Test
    void loadUserByUsername() {
        assertThrows(UsernameNotFoundException.class, () -> userDetailsManager.loadUserByUsername("nope"));

        RemoteUser user = (RemoteUser) userDetailsManager.loadUserByUsername("user");
        assertNotEquals(remoteUser, user);
        assertEquals("SP Dashboard", user.getName());
    }
}