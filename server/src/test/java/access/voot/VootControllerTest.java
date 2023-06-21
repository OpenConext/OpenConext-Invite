package access.voot;

import access.AbstractTest;
import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static access.Seed.GUEST_SUB;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

class VootControllerTest extends AbstractTest {

    @Test
    void getGroupMemberships() {
        List<Map<String, String>> groups = given()
                .when()
                .auth().basic("voot", "secret")
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .pathParam("sub", GUEST_SUB)
                .get("/api/voot/{sub}")
                .as(new TypeRef<>() {
                });
        List<String> urns = groups.stream().map(m -> m.get("urn")).sorted().toList();
        assertEquals(List.of(
                "urn:mace:surf.nl:test.surfaccess.nl:1:wiki",
                "urn:mace:surf.nl:test.surfaccess.nl:3:storage",
                "urn:mace:surf.nl:test.surfaccess.nl:4:research"), urns);
    }
}