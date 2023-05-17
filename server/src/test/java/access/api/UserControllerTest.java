package access.api;

import access.AbstractTest;
import com.fasterxml.jackson.core.type.TypeReference;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.SignedJWT;
import io.restassured.filter.cookie.CookieFilter;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URLDecoder;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

class UserControllerTest extends AbstractTest {

    @Test
    void meWithOauth2Login() throws Exception {
        CookieFilter cookieFilter = new CookieFilter();
        String location = given()
                .redirects()
                .follow(false)
                .when()
                .filter(cookieFilter)
                .get("/api/v1/users/me")
                .header("Location");
        assertEquals("http://localhost:" + this.port + "/oauth2/authorization/oidcng", location);

        location = given()
                .redirects()
                .follow(false)
                .when()
                .filter(cookieFilter)
                .get(location)
                .header("Location");
        MultiValueMap<String, String> queryParams = UriComponentsBuilder.fromUriString(location).build().getQueryParams();
        String redirectUri = queryParams.getFirst("redirect_uri");
        //The state is url encoded
        String state = URLDecoder.decode(queryParams.getFirst("state"), "UTF-8");
        String nonce = URLDecoder.decode(queryParams.getFirst("nonce"), "UTF-8");

        String keyId = String.format("key_%s", new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSS").format(new Date()));
        RSAKey rsaKey = generateRsaKey(keyId);
        String publicKeysJson = new JWKSet(rsaKey.toPublicJWK()).toJSONObject().toString();
        stubFor(get(urlPathMatching("/jwk-set")).willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(publicKeysJson)));

        Map<String, Object> tokenResult = objectMapper.readValue(new ClassPathResource("token-response.json").getInputStream(), new TypeReference<>() {
        });
        String sub = "inviter@utrecht.nl";
        SignedJWT signedJWT = getSignedJWT(keyId, redirectUri, rsaKey, sub, nonce, state);
        String serialized = signedJWT.serialize();
        tokenResult.put("access_token", serialized);
        tokenResult.put("id_token", serialized);
        tokenResult.put("refresh_token", serialized);
        String validTokenResult = objectMapper.writeValueAsString(tokenResult);
        stubFor(post(urlPathMatching("/token")).willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(validTokenResult)));

        Map<String, Object> userInfo = objectMapper.readValue(new ClassPathResource("user-info.json").getInputStream(), new TypeReference<>() {
        });
        userInfo.put("sub", sub);
        userInfo.put("eduperson_principal_name", sub);
        String userInfoResult = objectMapper.writeValueAsString(userInfo);
        stubFor(get(urlPathMatching("/user-info")).willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(userInfoResult)));

        Map<String, String> body = new HashMap<>();
        body.put("code", UUID.randomUUID().toString());
        body.put("state", state);
        given()
                .redirects()
                .follow(false)
                .filter(cookieFilter)
                .when()
                .contentType(ContentType.URLENC)
                .formParams(body)
                .post(redirectUri);

        Map res = given()
                .when()
                .filter(cookieFilter)
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .get("/api/v1/users/me")
                .as(Map.class);
        assertEquals("me", res.get("res"));
    }

    @Test
    void meWithAccessToken() throws IOException {
        Map res = given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .auth()
                .oauth2(opaqueAccessToken("inviter@utrecht.nl", "introspect.json"))
                .get("/api/external/v1/users/me")
                .as(Map.class);
        assertEquals("me", res.get("res"));
    }

}