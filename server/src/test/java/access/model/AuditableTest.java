package access.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AuditableTest {

    @BeforeEach
    protected void beforeEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCreatedByNoAuthentication() {
        Auditable auditable = new Auditable();
        auditable.prePersist();
        auditable.preUpdate();

        assertEquals("ResourceCleaner", auditable.getCreatedBy());
        assertNotNull(auditable.getCreatedAt());
        assertEquals("ResourceCleaner", auditable.getUpdatedBy());
        assertNotNull(auditable.getUpdatedAt());
    }

    @Test
    void getCreatedByBearerTokenAuthentication() {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(1, ChronoUnit.HOURS);
        OidcIdToken idToken = new OidcIdToken("value", now, expiresAt,
                Map.of("sub", "sub", "eduperson_principal_name", "eppn"));
        OAuth2AuthenticatedPrincipal principal = new DefaultOidcUser(List.of(new SimpleGrantedAuthority("USER")), idToken);
        OAuth2AccessToken credentials = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "token", now, expiresAt);
        BearerTokenAuthentication authentication = new BearerTokenAuthentication(principal, credentials, emptyList());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        Auditable auditable = new Auditable();
        auditable.prePersist();

        assertEquals("eppn", auditable.getCreatedBy());
    }

    @Test
    void getCreatedByOAuth2AuthenticationToken() {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(1, ChronoUnit.HOURS);
        OidcIdToken idToken = new OidcIdToken("value", now, expiresAt,
                Map.of("sub", "sub", "eduperson_principal_name", "eppn"));
        OAuth2User principal = new DefaultOidcUser(List.of(new SimpleGrantedAuthority("USER")), idToken);
        OAuth2AuthenticationToken authentication = new OAuth2AuthenticationToken(principal, emptyList(), "registration-id");

        SecurityContextHolder.getContext().setAuthentication(authentication);

        Auditable auditable = new Auditable();
        auditable.prePersist();

        assertEquals("eppn", auditable.getCreatedBy());
    }
}