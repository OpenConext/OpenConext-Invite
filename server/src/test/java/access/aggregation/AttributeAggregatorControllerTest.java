package access.aggregation;

import access.AbstractTest;
import access.manage.EntityType;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static access.AbstractTest.GUEST_SUB;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AttributeAggregatorControllerTest extends AbstractTest {

    @Test
    void getGroupMemberships() throws JsonProcessingException {
        stubForManageProviderByEntityID(EntityType.SAML20_SP, "https://research");
        List<Map<String, String>> roles = given()
                .when()
                .auth().basic("aa", "secret")
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .pathParam("sub", GUEST_SUB)
                .queryParam("SPentityID", "")
                .get("/api/aa/{sub}")
                .as(new TypeRef<>() {
                });
        assertEquals(1, roles.size());
        assertTrue(roles.get(0).get("id").startsWith("urn:mace:surf.nl:test.surfaccess.nl:"));
    }

    @Test
    void getGroupMembershipsNonExistingUser() throws JsonProcessingException {
        stubForManageProviderByEntityID(EntityType.SAML20_SP, "https://research");
        List<Map<String, String>> roles = given()
                .when()
                .auth().basic("aa", "secret")
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .pathParam("sub", "nope")
                .queryParam("SPentityID", "")
                .get("/api/aa/{sub}")
                .as(new TypeRef<>() {
                });
        assertEquals(0, roles.size());
    }

    @Test
    void getGroupMembershipsNonExistingProvider() throws JsonProcessingException {
        stubForManageProviderByEntityID(EntityType.SAML20_SP, "nope");
        stubForManageProviderByEntityID(EntityType.OIDC10_RP, "nope");
        List<Map<String, String>> roles = given()
                .when()
                .auth().basic("aa", "secret")
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .pathParam("sub", GUEST_SUB)
                .queryParam("SPentityID", "")
                .get("/api/aa/{sub}")
                .as(new TypeRef<>() {
                });
        assertEquals(0, roles.size());
    }
}