package access.api;

import access.AbstractTest;
import access.AccessCookieFilter;
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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserControllerTest extends AbstractTest {

    @Test
    void meWithOauth2Login() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/me", "inviter@utrecht.nl");

        Map res = given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .get(accessCookieFilter.apiURL())
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