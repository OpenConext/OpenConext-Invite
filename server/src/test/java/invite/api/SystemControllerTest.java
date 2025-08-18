package invite.api;

import invite.AbstractTest;
import invite.AccessCookieFilter;
import invite.manage.EntityType;
import invite.model.Role;
import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SystemControllerTest extends AbstractTest {

    @Test
    void cronCleanup() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", SUPER_SUB);
        given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .contentType(ContentType.JSON)
                .get("/api/v1/system/cron/cleanup")
                .then()
                .statusCode(200);
    }

    @Test
    void expiryUserRoles() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", SUPER_SUB);
        given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .contentType(ContentType.JSON)
                .get("/api/v1/system/expiry-user-roles")
                .then()
                .statusCode(200);
    }

    @Test
    void expiryNotifications() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", SUPER_SUB);
        given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .contentType(ContentType.JSON)
                .get("/api/v1/system/cron/expiry-notifications")
                .then()
                .body("mails", Matchers.hasSize(0))
                .statusCode(200);

    }

    @Test
    void unknownRoles() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", SUPER_SUB);
        super.stubForManagerProvidersByIdIn(EntityType.SAML20_SP, List.of("1", "2", "3", "4"));
        super.stubForManagerProvidersByIdIn(EntityType.OIDC10_RP, List.of("5", "6"));
        List<Role> roles = given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .contentType(ContentType.JSON)
                .get("/api/v1/system/unknown-roles")
                .as(new TypeRef<>() {
                });
        assertTrue(roles.isEmpty());
    }

    @Test
    void performanceSeed() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", SUPER_SUB);

        super.stubForManageAllProviders(EntityType.SAML20_SP, EntityType.OIDC10_RP, EntityType.SAML20_IDP);

        Map<String, Object> results = given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .queryParam("numberOfRole", 1)
                .queryParam("numberOfUsers", 1)
                .contentType(ContentType.JSON)
                .put("/api/v1/system/performance-seed")
                .as(new TypeRef<>() {
                });
        assertEquals(1, results.get("users"));
        assertEquals(1, results.get("roles"));
        assertEquals(1, results.get("userRoles"));
    }
}