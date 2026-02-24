package invite.aggregation;

import com.fasterxml.jackson.core.JsonProcessingException;
import invite.AbstractTest;
import invite.crm.CRMContact;
import invite.crm.CRMRole;
import invite.manage.EntityType;
import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static invite.manage.EntityType.SAML20_SP;
import static invite.security.SecurityConfig.API_KEY_HEADER;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AttributeAggregatorControllerTest extends AbstractTest {

    @Test
    void getGroupMemberships() throws JsonProcessingException {
        stubForManageProviderByEntityID(SAML20_SP, "https://research");
        List<Map<String, String>> roles = given()
                .when()
                .auth().preemptive().basic("aa", "secret")
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .pathParam("sub", GUEST_SUB)
                .queryParam("SPentityID", "")
                .get("/api/external/v1/aa/{sub}")
                .as(new TypeRef<>() {
                });
        assertEquals(1, roles.size());
        assertTrue(roles.get(0).get("id").startsWith("urn:mace:surf.nl:test.surfaccess.nl:"));
    }

    @Test
    void getGroupMembershipsManageUnavailable() {
        List<Map<String, String>> roles = given()
                .when()
                .auth().preemptive().basic("aa", "secret")
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .pathParam("sub", GUEST_SUB)
                .queryParam("SPentityID", "")
                .get("/api/external/v1/aa/{sub}")
                .as(new TypeRef<>() {
                });
        assertEquals(0, roles.size());
    }

    @Test
    void getGroupMembershipsGuestIncluded() throws JsonProcessingException {
        stubForManageProviderByEntityID(SAML20_SP, "https://wiki");
        List<Map<String, String>> roles = given()
                .when()
                .auth().preemptive().basic("aa", "secret")
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .pathParam("sub", MANAGE_SUB)
                .queryParam("SPentityID", "")
                .get("/api/external/v1/aa/{sub}")
                .as(new TypeRef<>() {
                });
        assertEquals(1, roles.size());
        assertTrue(roles.get(0).get("id").startsWith("urn:mace:surf.nl:test.surfaccess.nl:"));
    }

    @Test
    void getGroupMembershipsNonExistingUser() throws JsonProcessingException {
        stubForManageProviderByEntityID(SAML20_SP, "https://research");
        List<Map<String, String>> roles = given()
                .when()
                .auth().preemptive().basic("aa", "secret")
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .pathParam("sub", "nope")
                .queryParam("SPentityID", "")
                .get("/api/external/v1/aa/{sub}")
                .as(new TypeRef<>() {
                });
        assertEquals(0, roles.size());
    }

    @Test
    void getGroupMembershipsNonExistingProvider() throws JsonProcessingException {
        stubForManageProviderByEntityID(SAML20_SP, "nope");
        stubForManageProviderByEntityID(EntityType.OIDC10_RP, "nope");
        List<Map<String, String>> roles = given()
                .when()
                .auth().preemptive().basic("aa", "secret")
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .pathParam("sub", GUEST_SUB)
                .queryParam("SPentityID", "")
                .get("/api/external/v1/aa/{sub}")
                .as(new TypeRef<>() {
                });
        assertEquals(0, roles.size());
    }

    @Test
    void manageDown() {
        List<Map<String, String>> roles = given()
                .when()
                .auth().preemptive().basic("aa", "secret")
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .pathParam("sub", GUEST_SUB)
                .queryParam("SPentityID", "")
                .get("/api/external/v1/aa/{sub}")
                .as(new TypeRef<>() {
                });
        assertEquals(0, roles.size());
    }

    @Test
    void attributeAggregationCRMRole() throws JsonProcessingException {
        CRMRole crmRoleResearch = new CRMRole("5e17b508-08e4-e811-8100-005056956c1a", "CONBEH", "SURFconextbeheerder");
        CRMRole crmRoleCloud = new CRMRole("cf652619-08e4-e811-8100-005056956c1a", "CONVER", "SURFconextverantwoordelijke");
        CRMContact crmContact = getCrmContact(crmRoleResearch, "guest", "example.com", true);
        crmContact.setRoles(List.of(crmRoleCloud, crmRoleResearch));
        //This application is linked to the 'CONBEH' CRM role
        String researchEntityId = "https://research";
        super.stubForManageProviderByEntityID(EntityType.SAML20_SP, researchEntityId);
        super.stubForManageProviderByEntityID(EntityType.OIDC10_RP, "https://cloud");
        //Ignore the SCIM provisioning
        super.stubForManageProvisioning(List.of());

        String response = given()
                .when()
                .accept(ContentType.JSON)
                .header(API_KEY_HEADER, "secret")
                .contentType(ContentType.JSON)
                .body(crmContact)
                .post("/api/internal/v1/crm")
                .then()
                .extract()
                .asString();
        assertEquals("updated", response);

        stubForManageProviderByEntityID(SAML20_SP, researchEntityId);
        List<Map<String, String>> roles = given()
                .when()
                .auth().preemptive().basic("aa", "secret")
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .pathParam("sub", GUEST_SUB)
                .queryParam("SPentityID", researchEntityId)
                .get("/api/external/v1/aa/{sub}")
                .as(new TypeRef<>() {
                });
        assertEquals(4, roles.size());
        assertEquals(1, roles.stream().filter(m -> m.containsKey("id")).count());
        List<Map<String, String>> autorisatie = roles.stream().filter(m -> m.containsKey("autorisatie")).toList();
        assertEquals(3, autorisatie.size());
        List<String> autorizations = autorisatie.stream().map(m -> m.get("autorisatie")).sorted().toList();
        List<String> expected = Stream.of(
                "urn:mace:surfnet.nl:surfnet.nl:sab:organizationCode:abbrec",
                "urn:mace:surfnet.nl:surfnet.nl:sab:organizationGUID:organisationId",
                "urn:mace:surfnet.nl:surfnet.nl:sab:role:SURFconextbeheerder").sorted().toList();
        assertEquals(expected, autorizations);
    }
}