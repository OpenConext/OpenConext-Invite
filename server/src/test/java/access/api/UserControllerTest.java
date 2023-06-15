package access.api;

import access.AbstractTest;
import access.AccessCookieFilter;
import access.manage.EntityType;
import access.model.Authority;
import access.model.Role;
import access.model.User;
import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static access.Seed.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

class UserControllerTest extends AbstractTest {

    @Test
    void config() {
        Map res = given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .get("/api/v1/users/config")
                .as(Map.class);
        assertFalse((Boolean) res.get("authenticated"));
    }

    @Test
    void meWithOauth2Login() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/me", "urn:collab:person:example.com:admin");

        User user = given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .get(accessCookieFilter.apiURL())
                .as(User.class);
        assertEquals("urn:collab:person:example.com:admin", user.getEmail());

        Map res = given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .get("/api/v1/users/config")
                .as(Map.class);
        assertTrue((Boolean) res.get("authenticated"));
    }

    @Test
    void meWithRoles() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/me", "inviter@example.com");

        String body = objectMapper.writeValueAsString(localManage.providerById(EntityType.OIDC10_RP, "5"));
        stubFor(get(urlPathMatching("/manage/api/internal/metadata/oidc10_rp/5")).willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(body)));

        User user = given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .get(accessCookieFilter.apiURL())
                .as(User.class);
        List<String> roleNames = List.of("Mail", "Calendar").stream().sorted().toList();

        assertEquals(roleNames, user.getUserRoles().stream().map(userRole -> userRole.getRole().getName()).sorted().toList());
        assertEquals(1, user.getProviders().size());
        assertEquals("5", user.getProviders().get(0).get("id"));
        assertEquals("5", user.getProviders().get(0).get("_id"));
    }

    @Test
    void loginWithOauth2Login() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow(
                "/api/v1/users/login?force=true&hash=" + Authority.GUEST.name(),
                "urn:collab:person:example.com:admin",
                authorizationUrl -> {
                    MultiValueMap<String, String> queryParams = UriComponentsBuilder.fromUriString(authorizationUrl).build().getQueryParams();
                    String prompt = queryParams.getFirst("prompt");
                    assertEquals("login", prompt);
                    String loginHint = URLDecoder.decode(queryParams.getFirst("login_hint"), StandardCharsets.UTF_8);
                    assertEquals("https://login.test2.eduid.nl", loginHint);
                });

        String location = given()
                .redirects()
                .follow(false)
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .get(accessCookieFilter.apiURL())
                .header("Location");
        assertEquals("http://localhost:3000", location);
    }

    @Test
    void meWithAccessToken() throws IOException {
        User user = given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .auth()
                .oauth2(opaqueAccessToken(
                        "urn:collab:person:example.com:admin",
                        "introspect.json",
                        "jdoe@example.com"))
                .get("/api/external/v1/users/me")
                .as(User.class);
        assertEquals("jdoe@example.com", user.getEmail());
    }

    @Test
    void search() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", SUPER_SUB);

        List<User> users = given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .queryParam("query", "doe")
                .get("/api/v1/users/search")
                .as(new TypeRef<>() {
                });
        assertEquals(4, users.size());
    }

    @Test
    void usersByRole() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", MANAGE_SUB);
        Role role = roleRepository.findByManageIdAndShortNameIgnoreCase("1", "wiki").get();
        List<User> users = given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .pathParams("roleId", role.getId())
                .get("/api/v1/users/roles/{roleId}")
                .as(new TypeRef<>() {
                });
        assertEquals(2, users.size());
    }

    @Test
    void other() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", SUPER_SUB);
        Long id = userRepository.findBySubIgnoreCase(INVITER_SUB).get().getId();
        stubForManageProviderById(EntityType.OIDC10_RP, "5");
        User user = given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .pathParams("id", id)
                .get("/api/v1/users/other/{id}")
                .as(User.class);
        assertEquals(2, user.getUserRoles().size());
    }

    @Test
    void logout() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", SUPER_SUB);

        given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .get("/api/v1/users/logout")
                .then()
                .statusCode(200);

        String location = given()
                .redirects()
                .follow(false)
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .get("/api/v1/users/logout")
                .header("Location");
        assertEquals("http://localhost:" + port + "/oauth2/authorization/oidcng", location);
    }

    @Test
    void error() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", SUPER_SUB);

        given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .contentType(ContentType.JSON)
                .body(Map.of("error", "trouble") )
                .post("/api/v1/users/error")
                .then()
                .statusCode(201);
    }
}