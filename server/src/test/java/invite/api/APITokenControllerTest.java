package invite.api;

import invite.AbstractTest;
import invite.AccessCookieFilter;
import invite.config.HashGenerator;
import invite.model.APIToken;
import invite.model.User;
import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class APITokenControllerTest extends AbstractTest {

    @Test
    void apiTokensByInstitution() throws Exception {
        super.stubForManageProvidersAllowedByIdP(ORGANISATION_GUID);
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/me", INSTITUTION_ADMIN_SUB,
                institutionalAdminEntitlementOperator(ORGANISATION_GUID));

        List<Map<String, Object>> tokens = given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .contentType(ContentType.JSON)
                .get("/api/v1/tokens")
                .as(new TypeRef<>() {
                });
        assertEquals(2, tokens.size());
    }

    @Test
    void apiTokensByUser() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/me", INVITER_SUB);

        List<Map<String, Object>> tokens = given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .contentType(ContentType.JSON)
                .get("/api/v1/tokens")
                .as(new TypeRef<>() {
                });
        assertEquals(1, tokens.size());
        assertEquals("John Doe", tokens.getFirst().get("owner"));
    }

    @Test
    void create() throws Exception {
        super.stubForManageProvidersAllowedByIdP(ORGANISATION_GUID);
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/me", INSTITUTION_ADMIN_SUB,
                institutionalAdminEntitlementOperator(ORGANISATION_GUID));
        //First get the value, otherwise the creation will fail
        Map<String, String> res = given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .contentType(ContentType.JSON)
                .get("/api/v1/tokens/generate-token")
                .as(new TypeRef<>() {
                });
        String token = res.get("token");

        Map<String, Object> apiToken = given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .contentType(ContentType.JSON)
                .body(Map.of("description", "test"))
                .post("/api/v1/tokens")
                .as(new TypeRef<>() {
                });
        assertNull(apiToken.get("hashedValue"));
        assertEquals(ORGANISATION_GUID, apiToken.get("organizationGUID"));
        assertEquals("test", apiToken.get("description"));

        APIToken apiTokenFromDB = apiTokenRepository.findById(Long.valueOf(apiToken.get("id").toString())).get();
        assertEquals(HashGenerator.hashToken(token), apiTokenFromDB.getHashedValue());
    }

    @Test
    void createSuperUserToken() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/me", SUPER_SUB);
        //First get the value, otherwise the creation will fail
        given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .contentType(ContentType.JSON)
                .get("/api/v1/tokens/generate-token");

        Map<String, Object> apiToken = given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .contentType(ContentType.JSON)
                .body(Map.of("description", "test", "organizationGUID", "super-users-rule"))
                .post("/api/v1/tokens")
                .as(new TypeRef<>() {
                });
        assertEquals("super-users-rule", apiToken.get("organizationGUID"));
    }

    @Test
    void createSuperUserTokenWithoutOrganizationGUID() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/me", SUPER_SUB);
        //First get the value, otherwise the creation will fail
        given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .get("/api/v1/tokens/generate-token");

        given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .contentType(ContentType.JSON)
                .body(Map.of("description", "test", "organizationGUID", ""))
                .post("/api/v1/tokens")
                .then()
                .statusCode(HttpStatus.FORBIDDEN.value());
    }

    @Test
    void createWithFaultyToken() throws Exception {
        super.stubForManageProvidersAllowedByIdP(ORGANISATION_GUID);
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/me", INSTITUTION_ADMIN_SUB,
                institutionalAdminEntitlementOperator(ORGANISATION_GUID));
        User user = userRepository.findBySubIgnoreCase(SUPER_SUB).get();
        given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "organizationGUID", "wrong",
                        "hashedValue", "wrong",
                        "superUserToken", false,
                        "description", "test"))
                .post("/api/v1/tokens")
                .then()
                .statusCode(403);
    }

    @Test
    void deleteToken() throws Exception {
        super.stubForManageProvidersAllowedByIdP(ORGANISATION_GUID);
        APIToken apiToken = apiTokenRepository.findByHashedValue(HashGenerator.hashToken(API_TOKEN_HASH)).get();
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/me", INSTITUTION_ADMIN_SUB,
                institutionalAdminEntitlementOperator(ORGANISATION_GUID));

        long preCount = apiTokenRepository.count();

        given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .contentType(ContentType.JSON)
                .pathParams("id", apiToken.getId())
                .delete("/api/v1/tokens/{id}")
                .then()
                .statusCode(204);
        long postCount = apiTokenRepository.count();
        assertEquals(preCount - 1, postCount);
    }

    @Test
    void deleteOtherToken() throws Exception {
        String organisationGUID = "test_institution_guid";
        super.stubForManageProvidersAllowedByIdP(organisationGUID);
        APIToken apiToken = apiTokenRepository.findByHashedValue(HashGenerator.hashToken(API_TOKEN_HASH)).get();
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/me", INSTITUTION_ADMIN_SUB,
                institutionalAdminEntitlementOperator(organisationGUID));

        given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .contentType(ContentType.JSON)
                .pathParams("id", apiToken.getId())
                .delete("/api/v1/tokens/{id}")
                .then()
                .statusCode(403);
    }

    @Test
    void deleteSuperUserTokenNotAllowed() throws Exception {
        super.stubForManageProvidersAllowedByIdP(ORGANISATION_GUID);
        APIToken apiToken = apiTokenRepository.findByHashedValue(HashGenerator.hashToken(API_TOKEN_SUPER_USER_HASH)).get();
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/me", INSTITUTION_ADMIN_SUB,
                institutionalAdminEntitlementOperator(ORGANISATION_GUID));

        given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .contentType(ContentType.JSON)
                .pathParams("id", apiToken.getId())
                .delete("/api/v1/tokens/{id}")
                .then()
                .statusCode(403);
    }

    @Test
    void apiTokensBySuperAdmin() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", SUPER_SUB);

        List<Map<String, Object>> tokens = given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .contentType(ContentType.JSON)
                .get("/api/v1/tokens")
                .as(new TypeRef<>() {
                });

        assertEquals(4, tokens.size());
        assertEquals(1L, tokens.stream().filter(token -> (boolean) token.get("superUserToken")).count());
        assertEquals(2L, tokens.stream().filter(token -> token.get("organizationGUID") != null).count());
    }

    @Test
    void deleteSuperUserToken() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", SUPER_SUB);
        APIToken apiToken = apiTokenRepository.findByHashedValue(HashGenerator.hashToken(API_TOKEN_SUPER_USER_HASH)).get();

        long preCount = apiTokenRepository.count();

        given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .contentType(ContentType.JSON)
                .pathParams("id", apiToken.getId())
                .delete("/api/v1/tokens/{id}")
                .then()
                .statusCode(204);

        long postCount = apiTokenRepository.count();
        assertEquals(preCount - 1, postCount);
    }

    @Test
    void deleteUserToken() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", INVITER_SUB);
        APIToken apiToken = apiTokenRepository.findByHashedValue(HashGenerator.hashToken(API_TOKEN_INVITER_USER_HASH)).get();

        long preCount = apiTokenRepository.count();

        given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .contentType(ContentType.JSON)
                .pathParams("id", apiToken.getId())
                .delete("/api/v1/tokens/{id}")
                .then()
                .statusCode(204);

        long postCount = apiTokenRepository.count();
        assertEquals(preCount - 1, postCount);
    }
}