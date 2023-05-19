package access.api;

import access.AbstractTest;
import access.AccessCookieFilter;
import access.model.User;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

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
        assertEquals("jdoe@example.com", user.getEmail());

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
    void loginWithOauth2Login() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", "urn:collab:person:example.com:admin");

        String location = given()
                .redirects()
                .follow(false)
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .get(accessCookieFilter.apiURL())
                .header("Location");
        assertEquals("http://localhost:" + super.port + "/client", location);
    }

    @Test
    void meWithAccessToken() throws IOException {
        User user = given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .auth()
                .oauth2(opaqueAccessToken("urn:collab:person:example.com:admin", "introspect.json"))
                .get("/api/external/v1/users/me")
                .as(User.class);
        assertEquals("jdoe@example.com", user.getEmail());
    }

}