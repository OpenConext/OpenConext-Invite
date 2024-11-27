package access.security;

import access.manage.EntityType;
import access.model.Application;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExtendedInMemoryUserDetailsManagerTest {

    private final RemoteUser remoteUser = new RemoteUser(
            "user",
            "password",
            "SP Dashboard",
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