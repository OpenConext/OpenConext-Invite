package access;

import access.manage.EntityType;
import access.manage.LocalManage;
import access.model.RemoteProvisionedUser;
import access.model.User;
import access.repository.*;
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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ClassPathResource;
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

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "oidcng.introspect-url=http://localhost:8081/introspect",
                "spring.security.oauth2.client.provider.oidcng.authorization-uri=http://localhost:8081/authorization",
                "spring.security.oauth2.client.provider.oidcng.token-uri=http://localhost:8081/token",
                "spring.security.oauth2.client.provider.oidcng.user-info-uri=http://localhost:8081/user-info",
                "spring.security.oauth2.client.provider.oidcng.jwk-set-uri=http://localhost:8081/jwk-set",
                "manage.url: http://localhost:8081",
                "manage.enabled: true"
        })
@SuppressWarnings("unchecked")
public abstract class AbstractTest {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected RoleRepository roleRepository;

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
            new Seed(
                    invitationRepository,
                    remoteProvisionedGroupRepository,
                    remoteProvisionedUserRepository,
                    roleRepository,
                    userRepository,
                    userRoleRepository,
                    apiTokenRepository).doSeed();
        }
        if (this.localManage == null) {
            this.localManage = new LocalManage(objectMapper, false);
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

        SignedJWT signedJWT = getSignedJWT(keyId, redirectUri, rsaKey, sub, nonce, state);
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

        Map<String, Object> userInfo = objectMapper.readValue(new ClassPathResource("user-info.json").getInputStream(), new TypeReference<>() {
        });
        userInfo.put("sub", StringUtils.hasText(sub) ? sub : "sub");
        userInfo.put("email", sub);
        userInfo.put("eduperson_principal_name", sub);
        userInfo = userInfoEnhancer.apply(userInfo);
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

    protected SignedJWT getSignedJWT(String keyID, String redirectURI, RSAKey rsaKey, String sub, String nonce, String state) throws Exception {
        JWTClaimsSet claimsSet = getJwtClaimsSet("http://localhost:8081", sub, redirectURI, nonce, state);
        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256).type(JOSEObjectType.JWT).keyID(keyID).build();
        SignedJWT signedJWT = new SignedJWT(header, claimsSet);
        JWSSigner jswsSigner = new RSASSASigner(rsaKey);
        signedJWT.sign(jswsSigner);
        return signedJWT;
    }

    protected JWTClaimsSet getJwtClaimsSet(String clientId, String sub, String redirectURI, String nonce, String state) {
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
                .claim("given_name", "John")
                .claim("family_name", "Doe")
                .claim("nonce", nonce)
                .claim("state", state);
        return builder.build();
    }

    protected void stubForManageProvisioning(List<String> identifiers) throws JsonProcessingException {
        List<Map<String, Object>> providers = localManage.provisioning(identifiers);
        String body = objectMapper.writeValueAsString(providers);
        stubFor(post(urlPathMatching("/manage/api/internal/provisioning"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json")
                        .withBody(body)
                        .withStatus(200)));

    }

    protected void stubForManageProviderByOrganisationGUID(String organisationGUID) throws JsonProcessingException {
        String path = "/manage/api/internal/search/%s";
        List<Map<String, Object>> providers = localManage.providersByInstitutionalGUID(organisationGUID);
        Optional<Map<String, Object>> identityProvider = localManage.identityProviderByInstitutionalGUID(organisationGUID);
        Map<EntityType, Object> providersMap = Map.of(
                EntityType.SAML20_SP,
                providers.stream().filter(m -> m.get("type").equals(EntityType.SAML20_SP.collectionName())).toList(),
                EntityType.SAML20_IDP,
                List.of(identityProvider.get()),
                EntityType.OIDC10_RP,
                providers.stream().filter(m -> m.get("type").equals(EntityType.OIDC10_RP.collectionName())).toList()
        );
        //Lambda can't do exception handling
        for (Map.Entry<EntityType, Object> entry : providersMap.entrySet()) {
            EntityType entityType = entry.getKey();
            stubFor(post(urlPathMatching(String.format(path, entityType.collectionName()))).willReturn(aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody(objectMapper.writeValueAsString(entry.getValue()))));
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
    protected void stubForManagerProvidersByIdIn(EntityType entityType, List<String> identifiers) {
        String path = String.format("/manage/api/internal/rawSearch/%s\\\\?.*", entityType.name().toLowerCase());
        List<Map<String, Object>> providers = localManage.providersByIdIn(entityType, identifiers);
        String body = objectMapper.writeValueAsString(providers);
        stubFor(get(urlPathMatching(path)).willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(body)));
    }

    protected void stubForManageProviderByEntityID(EntityType entityType, String entityId) throws JsonProcessingException {
        String path = String.format("/manage/api/internal/rawSearch/%s\\\\?.*", entityType.name().toLowerCase());
        Optional<Map<String, Object>> provider = localManage.providerByEntityID(entityType, entityId);
        String body = objectMapper.writeValueAsString(provider.isPresent() ? List.of(provider.get()) : Collections.emptyList());
        stubFor(get(urlPathMatching(path)).willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(body)));
    }

    protected void stubForDeleteScimUser() {
        stubFor(delete(urlPathMatching("/api/scim/v2/users/(.*)"))
                .willReturn(aResponse()
                        .withStatus(201)));
    }

    protected void stubForDeleteEvaUser() {
        stubFor(post(urlPathMatching(String.format("/eva/api/v1/guest/disable/(.*)")))
                .willReturn(aResponse()
                        .withStatus(201)));
    }

    protected void stubForDeleteGraphUser() {
        stubFor(delete(urlPathMatching(String.format("/graph/users")))
                .willReturn(aResponse()
                        .withStatus(201)));
    }

    protected void stubForDeleteScimRole() {
        stubFor(delete(urlPathMatching("/api/scim/v2/groups/(.*)"))
                .willReturn(aResponse()
                        .withStatus(201)));
    }

    protected String stubForCreateScimRole() throws JsonProcessingException {
        String value = UUID.randomUUID().toString();
        String body = objectMapper.writeValueAsString(Map.of("id", value));
        stubFor(post(urlPathMatching(String.format("/api/scim/v2/groups")))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(body)));
        return value;
    }

    protected String stubForCreateScimUser() throws JsonProcessingException {
        String value = UUID.randomUUID().toString();
        String body = objectMapper.writeValueAsString(Map.of("id", value));
        stubFor(post(urlPathMatching(String.format("/api/scim/v2/users")))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(body)));
        return value;
    }

    protected String stubForCreateEvaUser() throws JsonProcessingException {
        String value = UUID.randomUUID().toString();
        String body = objectMapper.writeValueAsString(Map.of("id", value));
        stubFor(post(urlPathMatching(String.format("/eva/api/v1/guest/create")))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(body)));
        return value;
    }

    protected String stubForCreateGraphUser() throws JsonProcessingException {
        String value = UUID.randomUUID().toString();
        String body = objectMapper.writeValueAsString(Map.of(
                "id", value,
                "invitedUser", Map.of("id", value),
                "inviteRedeemUrl",
                "https://www.google.com"
        ));
        stubFor(post(urlPathMatching(String.format("/graph/users")))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(body)));
        return value;
    }

    protected void stubForUpdateGraphUser(String sub) throws JsonProcessingException {
        User user = userRepository.findBySubIgnoreCase(sub).get();
        String remoteIdentifier = UUID.randomUUID().toString();
        remoteProvisionedUserRepository.save(new RemoteProvisionedUser(user, remoteIdentifier, "9"));
        String body = objectMapper.writeValueAsString(Map.of(
                "id", remoteIdentifier
        ));
        stubFor(get(urlPathMatching(String.format("/graph/users")))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(body)));
        stubFor(patch(urlPathMatching(String.format("/graph/users")))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(body)));
    }

    protected void stubForUpdateScimRole() throws JsonProcessingException {
        stubFor(put(urlPathMatching(String.format("/api/scim/v2/groups/(.*)")))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                ));
    }

    protected void stubForUpdateScimRolePatch() throws JsonProcessingException {
        stubFor(patch(urlPathMatching(String.format("/api/scim/v2/groups/(.*)")))
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


}
