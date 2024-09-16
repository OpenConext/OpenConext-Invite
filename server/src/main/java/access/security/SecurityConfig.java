package access.security;

import access.exception.ExtendedErrorAttributes;
import access.manage.Manage;
import access.provision.ProvisioningService;
import access.repository.APITokenRepository;
import access.repository.InvitationRepository;
import access.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.context.DelegatingSecurityContextRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.util.matcher.RequestHeaderRequestMatcher;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.util.List;
import java.util.Locale;

@EnableWebSecurity
@EnableScheduling
@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties({ExternalApiConfiguration.class})
public class SecurityConfig {

    public static final String API_TOKEN_HEADER = "X-API-TOKEN";

    private final String eduidEntityId;
    private final String introspectionUri;
    private final String clientId;
    private final String secret;
    private final ClientRegistrationRepository clientRegistrationRepository;
    private final InvitationRepository invitationRepository;
    private final ProvisioningService provisioningService;
    private final ExternalApiConfiguration externalApiConfiguration;
    private final Environment environment;

    private final RequestHeaderRequestMatcher apiTokenRequestMatcher = new RequestHeaderRequestMatcher(API_TOKEN_HEADER);

    @Autowired
    public SecurityConfig(ClientRegistrationRepository clientRegistrationRepository,
                          InvitationRepository invitationRepository,
                          ProvisioningService provisioningService,
                          ExternalApiConfiguration externalApiConfiguration,
                          Environment environment,
                          @Value("${config.eduid-entity-id}") String eduidEntityId,
                          @Value("${oidcng.introspect-url}") String introspectionUri,
                          @Value("${oidcng.resource-server-id}") String clientId,
                          @Value("${oidcng.resource-server-secret}") String secret) {
        this.clientRegistrationRepository = clientRegistrationRepository;
        this.invitationRepository = invitationRepository;
        this.provisioningService = provisioningService;
        this.eduidEntityId = eduidEntityId;
        this.introspectionUri = introspectionUri;
        this.clientId = clientId;
        this.secret = secret;
        this.externalApiConfiguration = externalApiConfiguration;
        this.environment = environment;
    }

    @Configuration
    @EnableConfigurationProperties({SuperAdmin.class})
    public static class MvcConfig implements WebMvcConfigurer {

        private final UserRepository userRepository;
        private final APITokenRepository apiTokenRepository;
        private final SuperAdmin superAdmin;
        private final Manage manage;

        @Autowired
        public MvcConfig(UserRepository userRepository, APITokenRepository apiTokenRepository, SuperAdmin superAdmin, Manage manage) {
            this.userRepository = userRepository;
            this.apiTokenRepository = apiTokenRepository;
            this.superAdmin = superAdmin;
            this.manage = manage;
        }

        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
            argumentResolvers.add(new UserHandlerMethodArgumentResolver(userRepository, apiTokenRepository, superAdmin, manage));
        }

        @Override
        public void addCorsMappings(CorsRegistry registry) {
            registry.addMapping("/**")
                    .allowedMethods("*")
                    .allowedOrigins("*");
        }
    }

    @Bean
    public CookieSerializer cookieSerializer(@Value("${server.servlet.session.cookie.secure}") boolean secure) {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setUseSecureCookie(secure);
        return serializer;
    }

    @Bean
    @Order(1)
    SecurityFilterChain sessionSecurityFilterChain(HttpSecurity http,
                                                   Manage manage,
                                                   UserRepository userRepository,
                                                   @Value("${institution-admin.entitlement}") String entitlement,
                                                   @Value("${institution-admin.organization-guid-prefix}") String organizationGuidPrefix) throws Exception {
        http
                .csrf(csrfConfigurer -> csrfConfigurer
                        .ignoringRequestMatchers("/login/oauth2/code/oidcng")
                        .ignoringRequestMatchers("/api/v1/validations/**"))
                .securityMatcher("/login/oauth2/**", "/oauth2/authorization/**", "/api/v1/**")
                .authorizeHttpRequests(authorizeHttpRequestsConfigurer -> authorizeHttpRequestsConfigurer
                        .requestMatchers(
                                "/api/v1/csrf",
                                "/api/v1/disclaimer",
                                "/api/v1/users/config",
                                "/api/v1/users/logout",
                                "/api/v1/invitations/public",
                                "/api/v1/users/ms-accept-return/**",
                                "/api/v1/validations/**",
                                "/ui/**",
                                "internal/**")
                        .permitAll()
                        .anyRequest()
                        .authenticated()
                )
                .oauth2Login(oAuth2LoginConfigurer -> oAuth2LoginConfigurer
                        .authorizationEndpoint(authorization -> authorization
                                .authorizationRequestResolver(
                                        authorizationRequestResolver(this.clientRegistrationRepository)
                                )
                        ).userInfoEndpoint(userInfoEndpointConfigurer -> userInfoEndpointConfigurer.oidcUserService(
                                new CustomOidcUserService(manage, userRepository, provisioningService, entitlement, organizationGuidPrefix)))
                )
                //We need a reference to the securityContextRepository to update the authentication after an InstitutionAdmin invitation accept
                .securityContext(securityContextConfigurer ->
                        securityContextConfigurer.securityContextRepository(this.securityContextRepository()));
        if (environment.acceptsProfiles(Profiles.of("local"))) {
            //Thus avoiding an oauth2 login for local development
            http.addFilterBefore(new LocalDevelopmentAuthenticationFilter(), AnonymousAuthenticationFilter.class);
        }
        return http.build();
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new DelegatingSecurityContextRepository(
                new RequestAttributeSecurityContextRepository(),
                new HttpSessionSecurityContextRepository()
        );
    }

    private OAuth2AuthorizationRequestResolver authorizationRequestResolver(
            ClientRegistrationRepository clientRegistrationRepository) {
        DefaultOAuth2AuthorizationRequestResolver authorizationRequestResolver =
                new DefaultOAuth2AuthorizationRequestResolver(
                        clientRegistrationRepository, "/oauth2/authorization");
        authorizationRequestResolver.setAuthorizationRequestCustomizer(
                new AuthorizationRequestCustomizer(invitationRepository, eduidEntityId));
        return authorizationRequestResolver;
    }

    @Bean
    @Order(2)
    SecurityFilterChain basicAuthenticationSecurityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(c -> c.disable())
                .securityMatcher(
                        "/api/voot/**",
                        "/api/external/v1/voot/**",
                        "/api/teams/**",
                        "/api/external/v1/teams/**",
                        "/api/profile/**",
                        "/api/external/v1/profile/**",
                        "/api/aa/**",
                        "/api/external/v1/aa/**",
                        "/api/deprovision/**",
                        "/api/external/v1/deprovision/**",
                        "/api/external/v1/sp_dashboard/**"
                ).sessionManagement(c -> c
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(c -> c
                        //The API token is secured in the UserHandlerMethodArgumentResolver
                        .requestMatchers(apiTokenRequestMatcher)
                        .permitAll()
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
                        //The API token is secured in the UserHandlerMethodArgumentResolver
                        .requestMatchers(apiTokenRequestMatcher)
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
    public UserDetailsService userDetailsService() {
        return new ExtendedInMemoryUserDetailsManager(externalApiConfiguration.getRemoteUsers());
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
