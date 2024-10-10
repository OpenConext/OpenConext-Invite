package access.security;

import access.model.Provisionable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RemoteUser implements UserDetails, CredentialsContainer, Provisionable {

    private String username;
    private String password;
    private List<Scope> scopes = new ArrayList<>();

    public RemoteUser(RemoteUser remoteUser) {
        this.username = remoteUser.username;
        this.password = remoteUser.password;
        this.scopes = remoteUser.scopes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        //Convention dictates upperCase Role names to be used in @PreAuthorize annotations
        return scopes.stream()
                .map(scope -> new SimpleGrantedAuthority("ROLE_" + scope.name().toUpperCase()))
                .collect(Collectors.toList());
    }

    @Override
    public String getName() {
        return username;
    }

    @Override
    public void eraseCredentials() {
        this.password = null;
    }
}
