package access.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
class ExtendedInMemoryUserDetailsManagerTest {

    private final RemoteUser remoteUser = new RemoteUser("user", "password", List.of(Scope.profile))    ;
    private final ExtendedInMemoryUserDetailsManager userDetailsManager =
            new ExtendedInMemoryUserDetailsManager(List.of(remoteUser));

    @Test
    void loadUserByUsername() {
        assertThrows(UsernameNotFoundException.class, () ->  userDetailsManager.loadUserByUsername("nope"));

        RemoteUser user = (RemoteUser) userDetailsManager.loadUserByUsername("user");
        assertNotEquals(remoteUser, user);
    }
}