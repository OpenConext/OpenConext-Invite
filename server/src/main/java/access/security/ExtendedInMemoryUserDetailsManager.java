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
        this.users = users.stream().collect(Collectors.toMap(UserDetails::getUsername, identity()));
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        RemoteUser remoteUser = users.get(username);
        return remoteUser != null ? new RemoteUser(remoteUser) : null;
    }
}
