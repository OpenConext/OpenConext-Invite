package invite.security;

import invite.model.Application;
import invite.model.Provisionable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;

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
    private String displayName;
    private String organizationGUIDFallback;
    private List<Scope> scopes = new ArrayList<>();
    private List<Application> applications = new ArrayList<>();
    private boolean localDevMode;

    public RemoteUser(RemoteUser remoteUser) {
        this.username = remoteUser.username;
        this.password = remoteUser.password;
        this.displayName = remoteUser.displayName;
        this.organizationGUIDFallback = remoteUser.organizationGUIDFallback;
        this.scopes = remoteUser.scopes;
        this.applications = remoteUser.applications;
        this.localDevMode = remoteUser.localDevMode;
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
        return StringUtils.hasText(displayName) ? displayName : username;
    }

    @Override
    public void eraseCredentials() {
        this.password = null;
    }
}
