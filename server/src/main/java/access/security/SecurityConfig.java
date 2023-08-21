package access.security;

import access.config.UserHandlerMethodArgumentResolver;
import access.exception.ExtendedErrorAttributes;
import access.model.Invitation;
import access.repository.InvitationRepository;
import access.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
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
@EnableMethodSecurity
public class SecurityConfig {

    private final String eduidEntityId;
    private final String introspectionUri;
    private final String clientId;
    private final String secret;
    private final ClientRegistrationRepository clientRegistrationRepository;
    private final InvitationRepository invitationRepository;
    private final String vootUser;
    private final String vootPassword;
    private final String attributeAggregationUser;
    private final String attributeAggregationPassword;
    private final String lifeCycleUser;
    private final String lifeCyclePassword;

    @Autowired
    public SecurityConfig(ClientRegistrationRepository clientRegistrationRepository,
                          InvitationRepository invitationRepository,
                          @Value("${config.eduid-entity-id}") String eduidEntityId,
                          @Value("${oidcng.introspect-url}") String introspectionUri,
                          @Value("${oidcng.resource-server-id}") String clientId,
                          @Value("${oidcng.resource-server-secret}") String secret,
                          @Value("${voot.user}") String vootUser,
                          @Value("${voot.password}") String vootPassword,
                          @Value("${lifecyle.user}") String lifeCycleUser,
                          @Value("${lifecyle.password}") String lifeCyclePassword,
                          @Value("${attribute-aggregation.user}") String attributeAggregationUser,
                          @Value("${attribute-aggregation.password}") String attributeAggregationPassword) {
        this.clientRegistrationRepository = clientRegistrationRepository;
        this.invitationRepository = invitationRepository;
        this.eduidEntityId = eduidEntityId;
        this.introspectionUri = introspectionUri;
        this.clientId = clientId;
        this.secret = secret;
        this.vootUser = vootUser;
        this.vootPassword = vootPassword;
        this.lifeCycleUser = lifeCycleUser;
        this.lifeCyclePassword = lifeCyclePassword;
        this.attributeAggregationUser = attributeAggregationUser;
        this.attributeAggregationPassword = attributeAggregationPassword;
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
                    .allowedMethods("*")
                    .allowedOrigins("*");
        }
    }

    @Bean
    @Order(1)
    SecurityFilterChain sessionSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(c -> c
                        .ignoringRequestMatchers("/login/oauth2/code/oidcng")
                        .ignoringRequestMatchers("/api/v1/validations/**"))
                .securityMatcher("/login/oauth2/**", "/oauth2/authorization/**", "/api/v1/**")
                .authorizeHttpRequests(c -> c
                        .requestMatchers(
                                "/api/v1/csrf",
                                "/api/v1/users/config",
                                "/api/v1/users/logout",
                                "/api/v1/invitations/public",
                                "/api/v1/validations/**",
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
                        ).userInfoEndpoint(userInfo -> userInfo.oidcUserService(this.oidcUserService()))
                );

        return http.build();
    }

    private OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService() {
        final OidcUserService delegate = new OidcUserService();

        return (userRequest) -> {
            // Delegate to the default implementation for loading a user
            return delegate.loadUser(userRequest);
        };
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
                    HttpSession session = ((ServletRequestAttributes) requestAttributes)
                            .getRequest().getSession(false);
                    if (session == null) {
                        return;
                    }
                    DefaultSavedRequest savedRequest = (DefaultSavedRequest) session.getAttribute("SPRING_SECURITY_SAVED_REQUEST");
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
    SecurityFilterChain basicAuthenticationSecurityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(c -> c.disable())
                .securityMatcher("/api/voot/**",
                        "/api/external/v1/voot/**",
                        "/api/aa/**",
                        "/api/external/v1/aa/**",
                        "/api/deprovisioning/**",
                        "/api/external/v1/deprovisioning/**")
                .sessionManagement(c -> c
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(c -> c
                        .anyRequest()
                        .authenticated()
                )
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    @Order(3)
    SecurityFilterChain jwtSecurityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(c -> c.disable())
                .securityMatcher("/api/external/v1/**")
                .authorizeHttpRequests(c -> c
                        .requestMatchers("/api/external/v1/validations/**")
                        .permitAll()
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
    public InMemoryUserDetailsManager userDetailsService() {
        UserDetails vootUserDetails = User
                .withUsername(vootUser)
                .password("{noop}" + vootPassword)
                .roles("VOOT")
                .build();
        UserDetails attributeAggregationUserDetails = User
                .withUsername(attributeAggregationUser)
                .password("{noop}" + attributeAggregationPassword)
                .roles("ATTRIBUTE_AGGREGATION")
                .build();
        UserDetails lifeCyleUserDetails = User
                .withUsername(lifeCycleUser)
                .password("{noop}" + lifeCyclePassword)
                .roles("LIFECYCLE")
                .build();
        return new InMemoryUserDetailsManager(vootUserDetails, attributeAggregationUserDetails, lifeCyleUserDetails);
    }

    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver slr = new AcceptHeaderLocaleResolver();
        slr.setDefaultLocale(Locale.ENGLISH);
        return slr;
    }

    @Bean
    ErrorAttributes errorAttributes() {
        return new ExtendedErrorAttributes();
    }

}
