package access.security;

import access.manage.EntityType;
import access.model.Application;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
class ExtendedInMemoryUserDetailsManagerTest {

    private final RemoteUser remoteUser = new RemoteUser(
            "user",
            "password",
            List.of(Scope.profile),
            List.of(new Application("4", EntityType.SAML20_SP)));

    private final ExtendedInMemoryUserDetailsManager userDetailsManager =
            new ExtendedInMemoryUserDetailsManager(List.of(remoteUser));

    @Test
    void loadUserByUsername() {
        assertThrows(UsernameNotFoundException.class, () ->  userDetailsManager.loadUserByUsername("nope"));

        RemoteUser user = (RemoteUser) userDetailsManager.loadUserByUsername("user");
        assertNotEquals(remoteUser, user);
    }
}