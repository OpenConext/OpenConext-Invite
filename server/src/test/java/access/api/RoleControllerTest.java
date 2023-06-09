package access.api;

import access.AbstractTest;
import access.AccessCookieFilter;
import access.manage.EntityType;
import access.model.Role;
import access.model.RoleExists;
import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

class RoleControllerTest extends AbstractTest {

    @Test
    void create() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", "manager@example.com");
        Role role = new Role("New", "New desc", "1", EntityType.SAML20_SP);

        String body = objectMapper.writeValueAsString(localManage.providerById(EntityType.SAML20_SP, "1"));
        stubFor(get(urlPathMatching("/manage/api/internal/metadata/saml20_sp/1")).willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(body)));

        Map result = given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .contentType(ContentType.JSON)
                .body(role)
                .post("/api/v1/roles")
                .as(Map.class);
        assertNotNull(result.get("id"));
    }

    @Test
    void nameExistsTransientRole() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", "manager@example.com");
        Map result = given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .contentType(ContentType.JSON)
                .body(new RoleExists("WIKI", "1", null))
                .post("/api/v1/roles/validation/short_name")
                .as(Map.class);
        assertTrue((Boolean) result.get("exists"));
    }

    @Test
    void nameNotExistsTransientRole() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", "manager@example.com");
        Map result = given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .contentType(ContentType.JSON)
                .body(new RoleExists("unique", "1", null))
                .post("/api/v1/roles/validation/short_name")
                .as(Map.class);
        assertFalse((Boolean) result.get("exists"));
    }

    @Test
    void nameExists() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", "manager@example.com");
        Role role = roleRepository.findByManageIdAndShortNameIgnoreCase("1", "WIKI").get();
        Map result = given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .contentType(ContentType.JSON)
                .body(new RoleExists(role.getName(), role.getManageId(), role.getId()))
                .post("/api/v1/roles/validation/short_name")
                .as(Map.class);
        assertTrue((Boolean) result.get("exists"));
    }

    @Test
    void rolesByApplication() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", "manager@example.com");
        List<Role> roles = given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .contentType(ContentType.JSON)
                .get("/api/v1/roles")
                .as(new TypeRef<>() {
                });
        assertEquals(1, roles.size());
        assertEquals("Wiki", roles.get(0).getName());
    }
}