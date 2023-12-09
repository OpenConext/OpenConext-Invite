package access.security;

import access.exception.ExtendedErrorAttributes;
import access.manage.Manage;
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
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
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
public class SecurityConfig {

    public static final String API_TOKEN_HEADER = "X-API-TOKEN";

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
    private final String teamsUser;
    private final String teamsPassword;

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
                          @Value("${teams.user}") String teamsUser,
                          @Value("${teams.password}") String teamsPassword,
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
        this.teamsUser = teamsUser;
        this.teamsPassword = teamsPassword;
        this.attributeAggregationUser = attributeAggregationUser;
        this.attributeAggregationPassword = attributeAggregationPassword;
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
                .csrf(c -> c
                        .ignoringRequestMatchers("/login/oauth2/code/oidcng")
                        .ignoringRequestMatchers("/api/v1/validations/**"))
                .securityMatcher("/login/oauth2/**", "/oauth2/authorization/**", "/api/v1/**")
                .authorizeHttpRequests(c -> c
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
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(authorization -> authorization
                                .authorizationRequestResolver(
                                        authorizationRequestResolver(this.clientRegistrationRepository)
                                )
                        ).userInfoEndpoint(userInfo -> userInfo.oidcUserService(
                                new CustomOidcUserService(manage, userRepository, entitlement, organizationGuidPrefix)))
                )
                //We need a reference to the securityContextRepository to update the authentication after an InstitutionAdmin invitation accept
                .securityContext(securityContext -> securityContext.securityContextRepository(this.securityContextRepository()));

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
        final RequestHeaderRequestMatcher apiTokenRequestMatcher = new RequestHeaderRequestMatcher(API_TOKEN_HEADER);
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
        UserDetails teamsUserDetails = User
                .withUsername(teamsUser)
                .password("{noop}" + teamsPassword)
                .roles("TEAMS")
                .build();
        UserDetails lifeCyleUserDetails = User
                .withUsername(lifeCycleUser)
                .password("{noop}" + lifeCyclePassword)
                .roles("LIFECYCLE")
                .build();
        return new InMemoryUserDetailsManager(
                vootUserDetails,
                attributeAggregationUserDetails,
                lifeCyleUserDetails,
                teamsUserDetails);
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
