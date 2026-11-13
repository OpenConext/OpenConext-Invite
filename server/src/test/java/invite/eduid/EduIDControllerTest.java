package invite.eduid;

import invite.AbstractTest;
import invite.model.User;
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
        //Only users with an eppn ending with @eduid.nl are returned
        String eppn = "ann@eduid.nl";
        User ann = userRepository.findBySubIgnoreCase(GUEST_SUB).orElseThrow();
        ann.setEduPersonPrincipalName(eppn);
        userRepository.save(ann);

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
        //The Wiki manager is excluded (not a GUEST) as is George (eppn does not end with @eduid.nl)
        assertEquals(1, guests.size());

        Map<String, Object> result = guests.get(0);
        assertEquals(eppn, result.get("eduperson_principal_name"));
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) result.get("roles");
        assertEquals(2, roles.size());
        assertTrue(roles.containsAll(List.of("Wiki", "Research")));
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
