package invite.voot;

import invite.AbstractTest;
import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VootControllerTest extends AbstractTest {

    @Test
    void getGroupMemberships() {
        List<Map<String, String>> groups = given()
                .when()
                .auth().preemptive().basic("voot", "secret")
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .pathParam("sub", GUEST_SUB)
                .get("/api/voot/{sub}")
                .as(new TypeRef<>() {
                });
        List<String> urns = groups.stream().map(m -> m.get("urn")).sorted().toList();
        assertEquals(3, urns.size());
        assertTrue(urns.get(0).startsWith("urn:mace:surf.nl:test.surfaccess.nl:"));
    }

    @Test
    void getGroupMembershipsExternal() {
        List<Map<String, String>> groups = given()
                .when()
                .auth().preemptive().basic("voot", "secret")
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .pathParam("sub", GUEST_SUB)
                .get("/api/external/v1/voot/{sub}")
                .as(new TypeRef<>() {
                });
        List<String> urns = groups.stream().map(m -> m.get("urn")).sorted().toList();
        assertEquals(3, urns.size());
        assertTrue(urns.get(0).startsWith("urn:mace:surf.nl:test.surfaccess.nl:"));
    }

    @Test
    void getGroupMembershipsUnknownUser() {
        List<Map<String, String>> groups = given()
                .when()
                .auth().preemptive().basic("voot", "secret")
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .pathParam("sub", "nope")
                .get("/api/external/v1/voot/{sub}")
                .as(new TypeRef<>() {
                });
        assertEquals(0, groups.size());
    }

    @Test
    void getGroupMembershipsGuestIncluded() {
        List<Map<String, String>> groups = given()
                .when()
                .auth().preemptive().basic("voot", "secret")
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .pathParam("sub", MANAGE_SUB)
                .get("/api/external/v1/voot/{sub}")
                .as(new TypeRef<>() {
                });
        List<String> urns = groups.stream().map(m -> m.get("urn")).sorted().toList();
        assertEquals(1, urns.size());
        assertTrue(urns.get(0).startsWith("urn:mace:surf.nl:test.surfaccess.nl:"));
    }

    @Test
    void getGroupMembershipsOnlyGuest() {
        List<Map<String, String>> groups = given()
                .when()
                .auth().preemptive().basic("voot", "secret")
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .pathParam("sub", INVITER_SUB)
                .get("/api/external/v1/voot/{sub}")
                .as(new TypeRef<>() {
                });
        assertEquals(0, groups.size());
    }
}