package invite;

import invite.config.HashGenerator;
import invite.manage.EntityType;
import invite.manage.LocalManage;
import invite.model.*;
import invite.repository.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.restassured.RestAssured;
import io.restassured.common.mapper.TypeRef;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.filter.cookie.CookieFilter;
import io.restassured.http.ContentType;
import io.restassured.http.Headers;
import lombok.SneakyThrows;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.text.SimpleDateFormat;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "oidcng.introspect-url=http://localhost:8081/introspect",
                "config.past-date-allowed=False",
                "spring.security.oauth2.client.provider.oidcng.authorization-uri=http://localhost:8081/authorization",
                "spring.security.oauth2.client.provider.oidcng.token-uri=http://localhost:8081/token",
                "spring.security.oauth2.client.provider.oidcng.user-info-uri=http://localhost:8081/user-info",
                "spring.security.oauth2.client.provider.oidcng.jwk-set-uri=http://localhost:8081/jwk-set",
                "manage.url: http://localhost:8081",
                "myconext.uri: http://localhost:8081/myconext/api/invite/provision-eduid",
                "manage.enabled: true",
                "spring.task.scheduling.enabled=false",
                "spring.jpa.properties.hibernate.format_sql=false",
                "spring.jpa.show-sql=false"
        })
@SuppressWarnings("unchecked")
public abstract class AbstractTest {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public static final String SUPER_SUB = "urn:collab:person:example.com:super";
    public static final String MANAGE_SUB = "urn:collab:person:example.com:manager";
    public static final String INSTITUTION_ADMIN_SUB = "urn:collab:person:example.com:institution_admin";
    public static final String INVITER_SUB = "urn:collab:person:example.com:inviter";
    public static final String INVITER_WIKI_SUB = "urn:collab:person:example.com:inviter_wiki_sub";
    public static final String GUEST_SUB = "urn:collab:person:example.com:guest";
    public static final String KB_USER_SUB = "urn:collab:person:kb.nl:guest";
    public static final String GRAPH_INVITATION_HASH = "graph_invitation_hash";
    public static final String INSTITUTION_ADMIN_INVITATION_HASH = "institution_admin_invitation_hash";
    public static final String ORGANISATION_GUID = "ad93daef-0911-e511-80d0-005056956c1a";
    public static final String API_TOKEN_HASH = HashGenerator.generateToken();
    public static final String API_TOKEN_SUPER_USER_HASH = HashGenerator.generateToken();
    public static final String API_TOKEN_INVITER_USER_HASH = HashGenerator.generateToken();
    public static final String API_TOKEN_LEGACY_HASH = HashGenerator.generateToken();

    @Value("${manage.staticManageDirectory}")
    private String staticManageDirectory;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected RoleRepository roleRepository;

    @Autowired
    protected ApplicationRepository applicationRepository;

    @Autowired
    protected UserRoleRepository userRoleRepository;

    @Autowired
    protected InvitationRepository invitationRepository;

    @Autowired
    protected RemoteProvisionedGroupRepository remoteProvisionedGroupRepository;

    @Autowired
    protected RemoteProvisionedUserRepository remoteProvisionedUserRepository;

    @Autowired
    protected APITokenRepository apiTokenRepository;

    @Autowired
    protected ApplicationUsageRepository applicationUsageRepository;

    protected LocalManage localManage;

    @RegisterExtension
    WireMockExtension mockServer = new WireMockExtension(8081);

    @LocalServerPort
    protected int port;

    @BeforeAll
    protected static void beforeAll() {
        RestAssured.config = RestAssuredConfig.config()
                .objectMapperConfig(new ObjectMapperConfig().jackson2ObjectMapperFactory(
                        new ConfigurableJackson2ObjectMapperFactory()));
    }

    @BeforeEach
    protected void beforeEach() throws Exception {
        RestAssured.port = port;
        if (seedDatabase()) {
            this.doSeed();
        }
        if (this.localManage == null) {
            this.localManage = new LocalManage(objectMapper, staticManageDirectory);
        }
    }

    protected boolean seedDatabase() {
        return true;
    }

    protected String opaqueAccessToken(String sub, String responseJsonFileName, String email, String... scopes) throws IOException {
        List<String> scopeList = new ArrayList<>(Arrays.asList(scopes));
        scopeList.add("openid");

        Map<String, Object> introspectResult = objectMapper.readValue(new ClassPathResource(responseJsonFileName).getInputStream(), new TypeReference<>() {
        });
        introspectResult.put("sub", sub);
        introspectResult.put("eduperson_principal_name", email);
        introspectResult.put("email", email);
        introspectResult.put("given_name", "John");
        introspectResult.put("family_name", "Doe");

        introspectResult.put("scope", String.join(" ", scopeList));
        introspectResult.put("exp", (System.currentTimeMillis() / 1000) + 60);
        introspectResult.put("updated_at", System.currentTimeMillis() / 1000);
        String introspectResultWithScope = objectMapper.writeValueAsString(introspectResult);

        stubFor(post(urlPathMatching("/introspect")).willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(introspectResultWithScope)));
        return UUID.randomUUID().toString();
    }

    protected AccessCookieFilter openIDConnectFlow(String path, String sub) throws Exception {
        return this.openIDConnectFlow(path, sub, s -> {
        }, m -> m);
    }

    protected AccessCookieFilter openIDConnectFlow(String path,
                                                   String sub,
                                                   UnaryOperator<Map<String, Object>> userInfoEnhancer) throws Exception {
        return this.openIDConnectFlow(path, sub, s -> {
        }, userInfoEnhancer);
    }

    protected AccessCookieFilter openIDConnectFlow(String path, String sub,
                                                   Consumer<String> authorizationConsumer,
                                                   UnaryOperator<Map<String, Object>> userInfoEnhancer) throws Exception {
        CookieFilter cookieFilter = new CookieFilter();
        Headers headers = given()
                .redirects()
                .follow(false)
                .when()
                .filter(cookieFilter)
                .get(path)
                .headers();
        String location = headers.getValue("Location");
        assertEquals("http://localhost:" + this.port + "/oauth2/authorization/oidcng", location);

        headers = given()
                .redirects()
                .follow(false)
                .when()
                .filter(cookieFilter)
                .get(location)
                .headers();
        location = headers.getValue("Location");
        assertTrue(location.startsWith("http://localhost:8081/authorization?"));

        authorizationConsumer.accept(location);

        MultiValueMap<String, String> queryParams = UriComponentsBuilder.fromUriString(location).build().getQueryParams();
        String redirectUri = queryParams.getFirst("redirect_uri");
        //The state and nonce are url encoded
        String state = URLDecoder.decode(queryParams.getFirst("state"), StandardCharsets.UTF_8);
        String nonce = URLDecoder.decode(queryParams.getFirst("nonce"), StandardCharsets.UTF_8);

        String keyId = String.format("key_%s", new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSS").format(new Date()));
        RSAKey rsaKey = generateRsaKey(keyId);
        String publicKeysJson = new JWKSet(rsaKey.toPublicJWK()).toJSONObject().toString();
        stubFor(get(urlPathMatching("/jwk-set")).willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(publicKeysJson)));

        Map<String, Object> userInfo = objectMapper.readValue(new ClassPathResource("user-info.json").getInputStream(), new TypeReference<>() {
        });
        userInfo.put("sub", StringUtils.hasText(sub) ? sub : "sub");
        userInfo.put("email", sub);
        userInfo.put("eduperson_principal_name", sub);
        userInfo = userInfoEnhancer.apply(userInfo);

        SignedJWT signedJWT = getSignedJWT(keyId, redirectUri, rsaKey, sub, nonce, state, userInfo);
        String serialized = signedJWT.serialize();
        Map<String, Object> tokenResult = Map.of(
                "access_token", serialized,
                "expires_in", 864000,
                "id_token", serialized,
                "refresh_token", serialized,
                "token_type", "Bearer"
        );
        String validTokenResult = objectMapper.writeValueAsString(tokenResult);
        stubFor(post(urlPathMatching("/token")).willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(validTokenResult)));

        String userInfoResult = objectMapper.writeValueAsString(userInfo);
        stubFor(get(urlPathMatching("/user-info")).willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(userInfoResult)));

        Map<String, String> body = new HashMap<>();
        body.put("code", UUID.randomUUID().toString());
        body.put("state", state);

        headers = given()
                .redirects()
                .follow(false)
                .filter(cookieFilter)
                .when()
                .contentType(ContentType.URLENC)
                .formParams(body)
                .post(redirectUri)
                .headers();
        location = headers.getValue("Location");
        //Refreshing the CSRF token after authentication success and logout success is required
        Map<String, String> map = given()
                .when()
                .filter(cookieFilter)
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .get("/api/v1/csrf")
                .then()
                .extract()
                .as(new TypeRef<>() {
                });

        CsrfToken csrfToken = new DefaultCsrfToken(map.get("headerName"), map.get("parameterName"), map.get("token"));
        return new AccessCookieFilter(cookieFilter, location, csrfToken);
    }


    protected RSAKey generateRsaKey(String keyID) throws NoSuchProviderException, NoSuchAlgorithmException {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA", "BC");
        kpg.initialize(2048);
        KeyPair keyPair = kpg.generateKeyPair();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();

        return new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .algorithm(JWSAlgorithm.RS256)
                .keyUse(KeyUse.SIGNATURE)
                .keyID(keyID)
                .build();
    }

    protected SignedJWT getSignedJWT(String keyID, String redirectURI, RSAKey rsaKey, String sub, String nonce, String state, Map<String, Object> userInfo) throws Exception {
        JWTClaimsSet claimsSet = getJwtClaimsSet("http://localhost:8081", sub, redirectURI, nonce, state, userInfo);
        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256).type(JOSEObjectType.JWT).keyID(keyID).build();
        SignedJWT signedJWT = new SignedJWT(header, claimsSet);
        JWSSigner jswsSigner = new RSASSASigner(rsaKey);
        signedJWT.sign(jswsSigner);
        return signedJWT;
    }

    protected JWTClaimsSet getJwtClaimsSet(String clientId, String sub, String redirectURI, String nonce, String state, Map<String, Object> userInfo) {
        Instant instant = Clock.systemDefaultZone().instant();

        JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
                .audience("playground_client")
                .expirationTime(Date.from(instant.plus(3600, ChronoUnit.SECONDS)))
                .jwtID(UUID.randomUUID().toString())
                .issuer(clientId)
                .issueTime(Date.from(instant))
                .subject(StringUtils.hasText(sub) ? sub : "sub")
                .notBeforeTime(new Date(System.currentTimeMillis()))
                .claim("redirect_uri", redirectURI)
                .claim("eduperson_principal_name", sub)
                .claim("email", sub)
                .claim("given_name", userInfo.get("given_name"))
                .claim("family_name", userInfo.get("family_name"))
                .claim("nonce", nonce)
                .claim("state", state);
        return builder.build();
    }

    protected void stubForManageProvisioning(List<String> applicationIdentifiers) throws JsonProcessingException {
        List<Map<String, Object>> providers = localManage.provisioning(applicationIdentifiers);
        String body = writeValueAsString(providers);
        stubFor(post(urlPathMatching("/manage/api/internal/provisioning"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json")
                        .withBody(body)
                        .withStatus(200)));

    }

    protected void stubForManageProvidersAllowedByIdP(String organisationGUID) throws JsonProcessingException {
        String postPath = "/manage/api/internal/search/%s";
        Map<String, Object> identityProvider = localManage.identityProvidersByInstitutionalGUID(organisationGUID).get(0);
        stubFor(post(urlPathMatching(String.format(postPath, EntityType.SAML20_IDP.collectionName()))).willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(objectMapper.writeValueAsString(List.of(identityProvider)))));
        Boolean allowedAll = (Boolean) identityProvider.getOrDefault("allowedall", Boolean.FALSE);
        List<Map<String, Object>> providers = localManage.providersAllowedByIdP(identityProvider);
        //Lambda can't do exception handling
        for (EntityType entityType : List.of(EntityType.SAML20_SP, EntityType.OIDC10_RP)) {
            List<Map<String, Object>> body = providers.stream().filter(m -> m.get("type").equals(entityType.collectionName())).toList();
            if (allowedAll) {
                String path = String.format("/manage/api/internal/search/%s", entityType.name().toLowerCase());
                stubFor(post(urlPathMatching(path)).willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(body))));
            } else {
                String path = String.format("/manage/api/internal/rawSearch/%s", entityType.name().toLowerCase());
                stubFor(post(urlPathMatching(path)).willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(body))));
            }
        }
    }

    @SneakyThrows
    protected void stubForManageProviderById(EntityType entityType, String id) {
        String path = String.format("/manage/api/internal/metadata/%s/%s", entityType.name().toLowerCase(), id);
        String body = objectMapper.writeValueAsString(localManage.providerById(entityType, id));
        stubFor(get(urlPathMatching(path)).willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(body)));
    }

    @SneakyThrows
    protected void stubForManageProviderByIdNotFound(EntityType entityType, String id) {
        String path = String.format("/manage/api/internal/metadata/%s/%s", entityType.name().toLowerCase(), id);
        stubFor(get(urlPathMatching(path)).willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withStatus(404)));
    }

    @SneakyThrows
    protected void stubForManagerProvidersByIdIn(EntityType entityType, List<String> identifiers) {
        String path = String.format("/manage/api/internal/rawSearch/%s", entityType.name().toLowerCase());
        List<Map<String, Object>> providers = localManage.providersByIdIn(entityType, identifiers);
        String body = writeValueAsString(providers);
        stubFor(post(urlPathMatching(path)).willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(body)));
    }

    @SneakyThrows
    protected void stubForManageAllProviders(EntityType... entityTypes) {
        Stream.of(entityTypes).forEach(entityType -> {
            String path = String.format("/manage/api/internal/search/%s", entityType.name().toLowerCase());
            List<Map<String, Object>> providers = localManage.providers(entityType);
            String s = writeValueAsString(providers);
            String body = s;
            stubFor(post(urlPathMatching(path)).willReturn(aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody(body)));

        });
    }

    @SneakyThrows
    private String writeValueAsString(List<Map<String, Object>> providers) {
        return objectMapper.writeValueAsString(providers);
    }

    protected void stubForManageProviderByEntityID(EntityType entityType, String entityId) throws JsonProcessingException {
        String path = String.format("/manage/api/internal/rawSearch/%s", entityType.name().toLowerCase());
        Optional<Map<String, Object>> provider = localManage.providerByEntityID(entityType, entityId);
        String body = objectMapper.writeValueAsString(provider.isPresent() ? List.of(provider.get()) : Collections.emptyList());
        stubFor(post(urlPathMatching(path)).willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(body)));
    }

    protected void stubForDeleteScimUser() {
        stubFor(delete(urlPathMatching("/api/scim/v2/Users/(.*)"))
                .willReturn(aResponse()
                        .withStatus(201)));
    }

    protected void stubForDeleteEvaUser() {
        stubFor(post(urlPathMatching("/eva/api/v1/guest/disable/(.*)"))
                .willReturn(aResponse()
                        .withStatus(201)));
    }

    protected void stubForDeleteGraphUser() {
        stubFor(delete(urlPathMatching("/graph/users"))
                .willReturn(aResponse()
                        .withStatus(201)));
    }

    protected void stubForDeleteScimRole() {
        stubFor(delete(urlPathMatching("/api/scim/v2/Groups/(.*)"))
                .willReturn(aResponse()
                        .withStatus(201)));
    }

    protected String stubForCreateScimRole() throws JsonProcessingException {
        String value = UUID.randomUUID().toString();
        String body = objectMapper.writeValueAsString(Map.of("id", value));
        stubFor(post(urlPathMatching("/api/scim/v2/Groups"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(body)));
        return value;
    }

    protected String stubForCreateScimUser() throws JsonProcessingException {
        String value = UUID.randomUUID().toString();
        return stubForCreateScimUser(value);
    }

    protected String stubForCreateScimUser(String idValue) throws JsonProcessingException {
        String body = objectMapper.writeValueAsString(Map.of("id", idValue));
        stubFor(post(urlPathMatching("/api/scim/v2/Users"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(body)));
        return idValue;
    }

    protected String stubForCreateEvaUser() throws JsonProcessingException {
        String value = UUID.randomUUID().toString();
        String body = objectMapper.writeValueAsString(Map.of("id", value));
        stubFor(post(urlPathMatching("/eva/api/v1/guest/create"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(body)));
        return value;
    }

    protected void stubForUpdateEvaUser() {
        stubFor(post(urlPathMatching("/eva/api/v1/guest/create"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")));
    }

    protected String stubForCreateGraphUser() throws JsonProcessingException {
        String value = UUID.randomUUID().toString();
        String body = objectMapper.writeValueAsString(Map.of(
                "id", value,
                "invitedUser", Map.of("id", value),
                "inviteRedeemUrl",
                "https://www.google.com"
        ));
        stubFor(post(urlPathMatching("/graph/users"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(body)));
        return value;
    }

    protected void stubForUpdateScimUser() {
        stubFor(put(urlPathMatching("/api/scim/v2/Users/(.*)"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                ));
    }

    protected void stubForUpdateGraphUser(String sub) throws JsonProcessingException {
        User user = userRepository.findBySubIgnoreCase(sub).get();
        String remoteIdentifier = UUID.randomUUID().toString();
        remoteProvisionedUserRepository.save(new RemoteProvisionedUser(user, remoteIdentifier, "9"));
        String body = objectMapper.writeValueAsString(Map.of(
                "id", remoteIdentifier
        ));
        stubFor(get(urlPathMatching("/graph/users"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(body)));
        stubFor(patch(urlPathMatching("/graph/users"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(body)));
    }

    protected void stubForUpdateScimRole() {
        stubFor(put(urlPathMatching("/api/scim/v2/Groups/(.*)"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                ));
    }

    protected void stubForUpdateScimRolePatch() {
        stubFor(patch(urlPathMatching(String.format("/api/scim/v2/Groups/(.*)")))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                ));
    }

    protected UnaryOperator<Map<String, Object>> institutionalAdminEntitlementOperator(String organisationGuid) {
        return m -> {
            m.put("eduperson_entitlement",
                    List.of(
                            "urn:mace:surfnet.nl:surfnet.nl:sab:role:SURFconextverantwoordelijke",
                            "urn:mace:surfnet.nl:surfnet.nl:sab:organizationGUID:" + organisationGuid
                    ));
            return m;
        };
    }

    protected Set<ApplicationUsage> application(String manageId, EntityType entityType) {
        Application application = applicationRepository.findByManageIdAndManageTypeOrderById(manageId, entityType).
                orElseGet(() -> applicationRepository.save(new Application(manageId, entityType)));
        return Set.of(new ApplicationUsage(application, "http://landingpage.com"));
    }

    private void doSeed() {
        this.invitationRepository.deleteAllInBatch();
        this.remoteProvisionedGroupRepository.deleteAllInBatch();
        this.remoteProvisionedUserRepository.deleteAllInBatch();
        this.roleRepository.deleteAllInBatch();
        this.applicationRepository.deleteAllInBatch();
        this.userRepository.deleteAllInBatch();
        this.userRoleRepository.deleteAllInBatch();
        this.apiTokenRepository.deleteAllInBatch();

        User superUser =
                new User(true, SUPER_SUB, SUPER_SUB, "example.com", "David", "Doe", "david.doe@example.com");
        User institutionAdmin =
                new User(false, INSTITUTION_ADMIN_SUB, INSTITUTION_ADMIN_SUB, "example.com", "Carl", "Doe", "carl.doe@example.com");
        institutionAdmin.setInstitutionAdmin(true);
        institutionAdmin.setInstitutionAdminByInvite(true);
        institutionAdmin.setOrganizationGUID(ORGANISATION_GUID);

        User manager =
                new User(false, MANAGE_SUB, MANAGE_SUB, "example.com", "Mary", "Doe", "mary.doe@example.com");
        User inviter =
                new User(false, INVITER_SUB, INVITER_SUB, "example.com", "Paul", "Doe", "paul.doe@example.com");
        User wikiInviter =
                new User(false, INVITER_WIKI_SUB, INVITER_WIKI_SUB, "example.com", "James", "Doe", "james.doe@example.com");
        User guest =
                new User(false, GUEST_SUB, GUEST_SUB, "example.com", "Ann", "Doe", "ann.doe@example.com");
        guest.setEduId(UUID.randomUUID().toString());
        User kbUser =
                new User(false, KB_USER_SUB, KB_USER_SUB, "kb.nl", "George", "Best", "gb@kb.nl");
        doSave(this.userRepository, superUser, institutionAdmin, manager, inviter, wikiInviter, guest, kbUser);

        Role wiki =
                new Role("Wiki", "Wiki desc",
                        application("1", EntityType.SAML20_SP), 365, false, false);
        wiki.setInviterDisplayName("wiki@university.com");
        wiki.setOverrideSettingsAllowed(true);

        Role network =
                new Role("Network", "Network desc",
                        application("2", EntityType.SAML20_SP), 365, false, false);
        network.setEduIDOnly(true);


        Set<Application> applications = Set.of(
                        new Application("3", EntityType.SAML20_SP),
                        new Application("6", EntityType.OIDC10_RP))
                .stream()
                .map(app -> applicationRepository.findByManageIdAndManageTypeOrderById(app.getManageId(), app.getManageType()).
                        orElseGet(() -> applicationRepository.save(new Application(app.getManageId(), app.getManageType()))))
                .collect(Collectors.toSet());
        Set<ApplicationUsage> applicationUsages = applications.stream().map(application -> new ApplicationUsage(application, "https://landingpage.com")).collect(Collectors.toSet());

        Role storage =
                new Role("Storage", "Storage desc",
                        applicationUsages, 365, false, false);
        storage.setEnforceEmailEquality(true);
        Role research =
                new Role("Research", "Research desc",
                        application("4", EntityType.SAML20_SP), 365, false, false);
        research.setDefaultExpiryDays(Integer.valueOf(900));
        Role calendar =
                new Role("Calendar", "Calendar desc",
                        application("5", EntityType.OIDC10_RP), 365, false, false);

        Role mail =
                new Role("Mail", "Mail desc",
                        application("5", EntityType.OIDC10_RP), 365, false, false);

        //These roles will be accessible for the institution admin based
        network.setOrganizationGUID(ORGANISATION_GUID);
        research.setOrganizationGUID(ORGANISATION_GUID);
        wiki.setOrganizationGUID(ORGANISATION_GUID);

        doSave(this.roleRepository, wiki, network, storage, research, calendar, mail);

        UserRole wikiManager =
                new UserRole("system", manager, wiki, Authority.MANAGER);
        wikiManager.setGuestRoleIncluded(true);
        UserRole wikiInviterUserRole =
                new UserRole("system", wikiInviter, wiki, Authority.INVITER);
        UserRole calendarInviter =
                new UserRole("system", inviter, calendar, Authority.INVITER);
        UserRole mailInviter =
                new UserRole("system", inviter, mail, Authority.INVITER);
        UserRole storageGuest =
                new UserRole("system", guest, storage, Authority.GUEST);
        UserRole wikiGuest =
                new UserRole("system", guest, wiki, Authority.GUEST);
        UserRole researchGuest =
                new UserRole("system", guest, research, Authority.GUEST);
        UserRole kbUserGuest =
                new UserRole("system", kbUser, research, Authority.GUEST);
        doSave(this.userRoleRepository, wikiManager, wikiInviterUserRole, calendarInviter, mailInviter, storageGuest,
                wikiGuest, researchGuest, kbUserGuest);

        String message = "Please join..";
        Instant roleExpiryDate = Instant.now().plus(365, ChronoUnit.DAYS);
        Instant expiryDate = Instant.now().plus(14, ChronoUnit.DAYS);

        Invitation superUserInvitation =
                new Invitation(Authority.SUPER_USER, Authority.SUPER_USER.name(), "super_user@new.com", false, false, null, false, message, Language.en,
                        inviter, expiryDate, roleExpiryDate, Set.of(), null);
        Invitation managerInvitation =
                new Invitation(Authority.MANAGER, Authority.MANAGER.name(), "manager@new.com", false, false, null, false, message, Language.en,
                        inviter, expiryDate, roleExpiryDate, Set.of(new InvitationRole(research)), null);
        Invitation inviterInvitation =
                new Invitation(Authority.INVITER, Authority.INVITER.name(), "inviter@new.com", false, false, RequestedAuthnContext.EduIDLinkedInstitution, true, message, Language.en,
                        inviter, expiryDate, roleExpiryDate, Set.of(new InvitationRole(calendar), new InvitationRole(mail)), null);
        inviterInvitation.setEnforceEmailEquality(true);
        Invitation guestInvitation =
                new Invitation(Authority.GUEST, Authority.GUEST.name(), "guest@new.com",
                        false, false, null, false, message, Language.en,
                        inviter, expiryDate, roleExpiryDate, Set.of(new InvitationRole(mail)), null);
        guestInvitation.setEduIDOnly(true);
        //To test graph callback
        guestInvitation.setSubInvitee(GUEST_SUB);

        Invitation institutionAdminInvitation =
                new Invitation(Authority.INSTITUTION_ADMIN, INSTITUTION_ADMIN_INVITATION_HASH, "institutionh@admin.com",
                        false, false, null, false, message, Language.en,
                        institutionAdmin, expiryDate, roleExpiryDate, Set.of(new InvitationRole(network)), null);

        Invitation graphInvitation =
                new Invitation(Authority.GUEST, GRAPH_INVITATION_HASH, "graph@new.com",
                        false, false, null, false, message, Language.en,
                        inviter, expiryDate, roleExpiryDate, Set.of(new InvitationRole(network)), null);
        doSave(invitationRepository, superUserInvitation, managerInvitation, inviterInvitation, guestInvitation,
                institutionAdminInvitation, graphInvitation);

        APIToken apiToken = new APIToken(ORGANISATION_GUID, HashGenerator.hashToken(API_TOKEN_HASH),
                false, "Test-token", institutionAdmin);
        APIToken superUserApiToken = new APIToken(null,
                HashGenerator.hashToken(API_TOKEN_SUPER_USER_HASH), true,
                "Test super-user token", superUser);
        APIToken legacyApiToken = new APIToken(ORGANISATION_GUID, HashGenerator.hashToken(API_TOKEN_LEGACY_HASH),
                false, "Legacy-test-token", institutionAdmin);
        // Legacy tokens do not have owners
        legacyApiToken.setOwner(null);
        APIToken userApiToken = new APIToken(HashGenerator.hashToken(API_TOKEN_INVITER_USER_HASH),
                "Test-user token", inviter);
        doSave(apiTokenRepository, apiToken, superUserApiToken, legacyApiToken, userApiToken);
    }

    @SafeVarargs
    private <M> void doSave(JpaRepository<M, Long> repository, M... entities) {
        repository.saveAll(Arrays.asList(entities));
    }


}
