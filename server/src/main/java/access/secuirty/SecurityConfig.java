package access.secuirty;

import access.config.UserHandlerMethodArgumentResolver;
import access.model.Invitation;
import access.repository.InvitationRepository;
import access.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.savedrequest.DefaultSavedRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;

@EnableWebSecurity
@EnableScheduling
@Configuration
public class SecurityConfig {

    private final String eduidEntityId;
    private final String introspectionUri;
    private final String clientId;
    private final String secret;
    private final ClientRegistrationRepository clientRegistrationRepository;
    private final InvitationRepository invitationRepository;

    @Autowired
    public SecurityConfig(ClientRegistrationRepository clientRegistrationRepository,
                          InvitationRepository invitationRepository,
                          @Value("${config.eduid-entity-id}") String eduidEntityId,
                          @Value("${oidcng.introspect-url}") String introspectionUri,
                          @Value("${oidcng.resource-server-id}") String clientId,
                          @Value("${oidcng.resource-server-secret}") String secret) {
        this.clientRegistrationRepository = clientRegistrationRepository;
        this.invitationRepository = invitationRepository;
        this.eduidEntityId = eduidEntityId;
        this.introspectionUri = introspectionUri;
        this.clientId = clientId;
        this.secret = secret;
    }

    @Configuration
    @EnableConfigurationProperties(SuperAdmin.class)
    public static class MvcConfig implements WebMvcConfigurer {

        private final UserRepository userRepository;
        private final SuperAdmin superAdmin;

        @Autowired
        public MvcConfig(UserRepository userRepository, SuperAdmin superAdmin) {
            this.userRepository = userRepository;
            this.superAdmin = superAdmin;
        }

        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
            argumentResolvers.add(new UserHandlerMethodArgumentResolver(userRepository, superAdmin));
        }

        @Override
        public void addCorsMappings(CorsRegistry registry) {
            registry.addMapping("/**")
                    .allowedOrigins("*");
        }
    }

    @Bean
    @Order(1)
    SecurityFilterChain sessionSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(c -> c.ignoringRequestMatchers("/login/oauth2/code/oidcng"))
                .securityMatcher("/login/oauth2/**", "/oauth2/authorization/**", "/api/v1/**")
                .authorizeHttpRequests(c -> c
                        .requestMatchers(
                                "/api/v1/csrf",
                                "/api/v1/users/config",
                                "/api/v1/users/logout",
                                "/api/v1/invitations/public",
                                "/ui/**",
                                "internal/**")
                        .permitAll()
                        .anyRequest()
                        .authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(authorization -> authorization
                                .authorizationRequestResolver(
                                        authorizationRequestResolver(this.clientRegistrationRepository)
                                )
                        )
                );

        return http.build();
    }

    private OAuth2AuthorizationRequestResolver authorizationRequestResolver(
            ClientRegistrationRepository clientRegistrationRepository) {
        DefaultOAuth2AuthorizationRequestResolver authorizationRequestResolver =
                new DefaultOAuth2AuthorizationRequestResolver(
                        clientRegistrationRepository, "/oauth2/authorization");
        authorizationRequestResolver.setAuthorizationRequestCustomizer(
                authorizationRequestCustomizer());

        return authorizationRequestResolver;
    }

    private Consumer<OAuth2AuthorizationRequest.Builder> authorizationRequestCustomizer() {
        return customizer -> customizer
                .additionalParameters(params -> {
                    RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
                    DefaultSavedRequest savedRequest = (DefaultSavedRequest) ((ServletRequestAttributes) requestAttributes)
                            .getRequest().getSession(false).getAttribute("SPRING_SECURITY_SAVED_REQUEST");
                    String[] force = savedRequest.getParameterValues("force");
                    if (force != null && force.length == 1) {
                        params.put("prompt", "login");
                    }
                    String[] hash = savedRequest.getParameterValues("hash");
                    if (hash != null && hash.length == 1) {
                        Optional<Invitation> optionalInvitation = invitationRepository.findByHash(hash[0]);
                        optionalInvitation.ifPresent(invitation -> {
                            if (invitation.isEduIDOnly()) {
                                params.put("login_hint", eduidEntityId);
                            }
                        });
                    }
                });
    }

    @Bean
    @Order(2)
    SecurityFilterChain jwtSecurityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(c -> c.disable())
                .securityMatcher("/api/external/v1/**")
                .authorizeHttpRequests(c -> c
                        .anyRequest()
                        .authenticated()
                )
                .sessionManagement(c -> c
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .oauth2ResourceServer(c -> c
                        .opaqueToken(c1 -> c1
                                .introspectionUri(introspectionUri)
                                .introspectionClientCredentials(clientId, secret)
                        ));
        return http.build();
    }

    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver slr = new AcceptHeaderLocaleResolver();
        slr.setDefaultLocale(Locale.ENGLISH);
        return slr;
    }
}