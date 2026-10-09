package invite.api;

import invite.AbstractTest;
import invite.AccessCookieFilter;
import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestPropertySource(properties = "config.access-menu-enabled=false")
class MenuDisabledControllerTest extends AbstractTest {

    @Test
    void menuDisabled() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/menu", "urn:collab:person:example.com:admin");

        Map<String, Object> menu = given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .get(accessCookieFilter.apiURL())
                .as(new TypeRef<>() {
                });
        assertEquals(List.of(), menu.get("menuItems"));
        assertTrue((Boolean) menu.get("disabled"));
        //Access is never called
        verify(0, getRequestedFor(urlPathMatching("/access/api/external/v1/menu")));
    }

    @Test
    void configExposesAccessMenuDisabled() {
        Map<String, Object> res = given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .get("/api/v1/users/config")
                .as(new TypeRef<>() {
                });
        assertFalse((Boolean) res.get("accessMenuEnabled"));
    }
}
