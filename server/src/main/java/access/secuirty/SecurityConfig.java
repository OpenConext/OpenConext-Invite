package access.secuirty;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

@EnableWebSecurity
@EnableScheduling
@Configuration
public class SecurityConfig {

    private final String introspectionUri;
    private final String clientId;
    private final String secret;
    private final ProvisioningOidcUserService provisioningOidcUserService;

    public SecurityConfig(ProvisioningOidcUserService provisioningOidcUserService,
                          @Value("${oidcng.introspect-url}") String introspectionUri,
                          @Value("${oidcng.resource-server-id}") String clientId,
                          @Value("${oidcng.resource-server-secret}") String secret) {
        this.provisioningOidcUserService = provisioningOidcUserService;
        this.introspectionUri = introspectionUri;
        this.clientId = clientId;
        this.secret = secret;
    }

    @Bean
    @Order(1)
    SecurityFilterChain sessionSecurityFilterChain(HttpSecurity http) throws Exception {
        http.csrf().disable()
                .securityMatcher("/login/oauth2/**", "/oauth2/authorization/**", "/api/v1/**")
                .authorizeHttpRequests()
                .requestMatchers("/api/v1/users/config", "/ui/**")
                .permitAll()
                .requestMatchers("/api/v1/**")
                .authenticated()
                .and()
                .oauth2Login()
                .userInfoEndpoint()
                .oidcUserService(provisioningOidcUserService);
        return http.build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain jwtSecurityFilterChain(HttpSecurity http) throws Exception {
        http.csrf().disable()
                .securityMatcher("/api/internal/v1/**")
                .authorizeHttpRequests()
                .anyRequest()
                .authenticated()
                .and()
                .oauth2ResourceServer()
                .opaqueToken()
                .introspectionUri(introspectionUri)
                .introspectionClientCredentials(clientId, secret);
        return http.build();
    }
}
