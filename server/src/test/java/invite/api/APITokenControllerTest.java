package invite.api;

import invite.AbstractTest;
import invite.AccessCookieFilter;
import invite.config.HashGenerator;
import invite.model.APIToken;
import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

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

        List<APIToken> tokens = given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .contentType(ContentType.JSON)
                .get("/api/v1/tokens")
                .as(new TypeRef<>() {
                });
        assertEquals(1, tokens.size());
    }

    @Test
    void apiTokensByUser() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/me", INVITER_SUB);

        List<APIToken> tokens = given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .contentType(ContentType.JSON)
                .get("/api/v1/tokens")
                .as(new TypeRef<>() {
                });
        assertEquals(1, tokens.size());
        assertEquals(INVITER_SUB, tokens.getFirst().getOwner().getSub());
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

        APIToken apiToken = given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .contentType(ContentType.JSON)
                .body(Map.of("description", "test"))
                .post("/api/v1/tokens")
                .as(new TypeRef<>() {
                });
        assertNull(apiToken.getHashedValue());
        assertEquals(ORGANISATION_GUID, apiToken.getOrganizationGUID());
        assertEquals("test", apiToken.getDescription());

        APIToken apiTokenFromDB = apiTokenRepository.findById(apiToken.getId()).get();
        assertEquals(HashGenerator.hashToken(token), apiTokenFromDB.getHashedValue());
    }

    @Test
    void createWithFaultyToken() throws Exception {
        super.stubForManageProvidersAllowedByIdP(ORGANISATION_GUID);
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/me", INSTITUTION_ADMIN_SUB,
                institutionalAdminEntitlementOperator(ORGANISATION_GUID));
        given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .contentType(ContentType.JSON)
                .body(new APIToken("wrong", "wrong", false, "wrong"))
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

        assertEquals(3, apiTokenRepository.count());

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
        assertEquals(2, apiTokenRepository.count());
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

        List<APIToken> tokens = given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .contentType(ContentType.JSON)
                .get("/api/v1/tokens")
                .as(new TypeRef<>() {
                });

        assertEquals(3, tokens.size());
        assertEquals(1L, tokens.stream().filter(token -> token.isSuperUserToken()).count());
        assertEquals(1L, tokens.stream().filter(token -> token.getOrganizationGUID() != null).count());
    }

    @Test
    void deleteSuperUserToken() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", SUPER_SUB);
        APIToken apiToken = apiTokenRepository.findByHashedValue(HashGenerator.hashToken(API_TOKEN_SUPER_USER_HASH)).get();

        assertEquals(3, apiTokenRepository.count());

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

        assertEquals(2, apiTokenRepository.count());
    }

    @Test
    void deleteUserToken() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", INVITER_SUB);
        APIToken apiToken = apiTokenRepository.findByHashedValue(HashGenerator.hashToken(API_TOKEN_INVITER_USER_HASH)).get();

        assertEquals(3, apiTokenRepository.count());

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

        assertEquals(2, apiTokenRepository.count());
    }
}