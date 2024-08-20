package access.security;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
public class RemoteUser implements UserDetails, CredentialsContainer {

    private String username;
    private String password;
    private List<String> scopes;

    public RemoteUser(RemoteUser remoteUser) {
        this.username = remoteUser.username;
        this.password = remoteUser.password;
        this.scopes = remoteUser.scopes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        //Convention dictates upperCase Role names to be used in @PreAuthorize annotations
        return scopes.stream()
                .map(scope -> new SimpleGrantedAuthority("ROLE_" + scope.toUpperCase()))
                .collect(Collectors.toList());
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public void eraseCredentials() {
        this.password = null;
    }
}
