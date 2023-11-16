package access.api;

import access.AbstractTest;
import access.AccessCookieFilter;
import access.config.HashGenerator;
import access.model.APIToken;
import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static access.Seed.*;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class APITokenControllerTest extends AbstractTest {

    @Test
    void apiTokensByInstitution() throws Exception {
        super.stubForManageProviderByOrganisationGUID(ORGANISATION_GUID);
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/me", INSTITUTION_ADMIN, institutionalAdminEntitlementOperator);

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
    void create() throws Exception {
        super.stubForManageProviderByOrganisationGUID(ORGANISATION_GUID);
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/me", INSTITUTION_ADMIN, institutionalAdminEntitlementOperator);
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
                .body(Map.of("description","test"))
                .post("/api/v1/tokens")
                .as(new TypeRef<>() {
                });
        assertNull(apiToken.getHashedValue());
        assertNull(apiToken.getOrganizationGUID());
        assertEquals("test", apiToken.getDescription());

        APIToken apiTokenFromDB = apiTokenRepository.findById(apiToken.getId()).get();
        assertEquals(HashGenerator.hashToken(token), apiTokenFromDB.getHashedValue());
    }

    @Test
    void deleteToken() throws Exception {
        super.stubForManageProviderByOrganisationGUID(ORGANISATION_GUID);
        APIToken apiToken = apiTokenRepository.findByHashedValue(HashGenerator.hashToken(API_TOKEN_HASH)).get();
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/me", INSTITUTION_ADMIN, institutionalAdminEntitlementOperator);

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
        assertEquals(0, apiTokenRepository.count());
    }
}