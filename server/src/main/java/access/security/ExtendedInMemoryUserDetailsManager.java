package access.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;

public class ExtendedInMemoryUserDetailsManager implements UserDetailsService {

    private final Map<String, RemoteUser> users;

    public ExtendedInMemoryUserDetailsManager(List<RemoteUser> users) {
        users.forEach(this::fixPassword);
        this.users = users.stream()
                .collect(Collectors.toMap(UserDetails::getUsername, identity()));
    }

    private void fixPassword(RemoteUser remoteUser) {
        String password = remoteUser.getPassword();
        String noopPrefix = "{noop}";
        boolean hasNoopPrefix = password.startsWith(noopPrefix);
        if (!hasNoopPrefix) {
            remoteUser.setPassword(noopPrefix.concat(password));
        }
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        RemoteUser remoteUser = users.get(username);
        return remoteUser != null ? new RemoteUser(remoteUser) : null;
    }
}
