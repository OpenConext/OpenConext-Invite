package provisioning.api;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SCIMControllerTest extends AbstractTest {

    @Test
    void user() {
        Map result = given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body(Map.of("user", "test"))
                .post("/api/scim/v2/Users")
                .as(Map.class);
        String id = (String) result.get("id");
        assertNotNull(id);

        result = given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body(Map.of("user", "test"))
                .pathParams("id", id)
                .put("/api/scim/v2/Users/{id}")
                .as(Map.class);
        assertNotNull(result.get("id"));

        given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .pathParams("id", id)
                .delete("/api/scim/v2/Users/{id}")
                .then()
                .statusCode(201);

        assertEquals(3, provisioningRepository.count());
    }

    @Test
    void patchGroup() {
        Map result = given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body(Map.of("user", "test"))
                .pathParams("id", UUID.randomUUID().toString())
                .patch("api/scim/v2/Groups/{id}")
                .as(Map.class);
        assertNotNull(result.get("id"));
        assertEquals(1, provisioningRepository.count());
    }

    @Test
    void putGroup() {
        Map result = given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body(Map.of("user", "test"))
                .pathParams("id", UUID.randomUUID().toString())
                .put("api/scim/v2/Groups/{id}")
                .as(Map.class);
        assertNotNull(result.get("id"));
        assertEquals(1, provisioningRepository.count());
    }
}
