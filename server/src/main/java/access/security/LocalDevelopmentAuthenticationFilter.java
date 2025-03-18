package access.security;

import jakarta.servlet.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class LocalDevelopmentAuthenticationFilter implements Filter {

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            this.populateSecurityContext();
        }
        filterChain.doFilter(servletRequest, servletResponse);
    }

    private void populateSecurityContext() {
        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("OPENID"));
        Map<String, Object> claims = Map.of(
                "eduperson_principal_name", "urn:collab:person:example.com:super",
                "email", "email",
                "family_name", "Doe",
                "given_name", "John",
                "name", "John Doe",
                "schac_home_organization", "example.com",
                "scope", "openid",
                "sub", "urn:collab:person:example.com:super",
                "uids", List.of("super"));
        OidcIdToken idtoken = new OidcIdToken(
                UUID.randomUUID().toString(),
                Instant.now(),
                Instant.now().plus(1, ChronoUnit.HOURS),
                claims
        );
        OidcUserInfo userInfo = new OidcUserInfo(claims);
        DefaultOidcUser oidcUser = new DefaultOidcUser(authorities, idtoken, userInfo);
        OAuth2AuthenticationToken authenticationToken = new OAuth2AuthenticationToken(
                oidcUser,
                authorities,
                "oidcng"
        );
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
    }
}
