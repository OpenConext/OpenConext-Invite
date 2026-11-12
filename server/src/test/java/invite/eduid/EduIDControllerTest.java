package invite.eduid;

import invite.AbstractTest;
import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EduIDControllerTest extends AbstractTest {

    private static final String PATH = "/api/external/v1/eduid/guests/{organizationGUID}";

    @Test
    void guestsByOrganizationGUID() {
        List<Map<String, Object>> guests = given()
                .when()
                .auth().preemptive().basic("eduid", "secret")
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .pathParam("organizationGUID", ORGANISATION_GUID)
                .get(PATH)
                .then()
                .statusCode(200)
                .extract()
                .as(new TypeRef<>() {
                });
        //Only the GUEST user roles of the roles of this organization: the manager of Wiki is excluded
        assertEquals(2, guests.size());

        Map<String, Object> ann = guests.stream()
                .filter(m -> m.get("eduperson_principal_name").equals(GUEST_SUB)).findFirst().orElseThrow();
        @SuppressWarnings("unchecked")
        List<String> annRoles = (List<String>) ann.get("roles");
        assertEquals(2, annRoles.size());
        assertTrue(annRoles.containsAll(List.of("Wiki", "Research")));

        Map<String, Object> george = guests.stream()
                .filter(m -> m.get("eduperson_principal_name").equals(KB_USER_SUB)).findFirst().orElseThrow();
        assertEquals(List.of("Research"), george.get("roles"));
    }

    @Test
    void guestsByOrganizationGUIDUnknown() {
        List<Map<String, Object>> guests = given()
                .when()
                .auth().preemptive().basic("eduid", "secret")
                .accept(ContentType.JSON)
                .pathParam("organizationGUID", "nope")
                .get(PATH)
                .as(new TypeRef<>() {
                });
        assertEquals(0, guests.size());
    }

    @Test
    void guestsByOrganizationGUIDForbidden() {
        given()
                .when()
                .auth().preemptive().basic("profile", "secret")
                .accept(ContentType.JSON)
                .pathParam("organizationGUID", ORGANISATION_GUID)
                .get(PATH)
                .then()
                .statusCode(403);
    }
}
