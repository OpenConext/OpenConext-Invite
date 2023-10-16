package provisioning.api;

import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GraphControllerTest extends AbstractTest {

    @Test
    void createUser() {
        Map<String, Object> result = given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body(Map.of("inviteRedirectUrl", "http://localhost/api/v1/invitations/ms-accept-return/1/2"))
                .post("/graph/users")
                .as(new TypeRef<>() {
                });
        assertNotNull(result.get("id"));
        assertNotNull(((Map) result.get("invitedUser")).get("id"));
        assertEquals("http://localhost:8081/graph/accept/1/2", result.get("inviteRedeemUrl"));

        assertEquals(1, provisioningRepository.count());
    }

    @Test
    void getUser() {
        Map<String, Object> result = given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .get("/graph/users")
                .as(new TypeRef<>() {
                });
        assertNotNull(result.get("id"));
        assertEquals(1, provisioningRepository.count());
    }

    @Test
    void updateUser() {
        Map<String, Object> result = given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body(Map.of("id", UUID.randomUUID().toString()))
                .patch("/graph/users")
                .as(new TypeRef<>() {
                });
        assertNotNull(result.get("id"));

        assertEquals(1, provisioningRepository.count());
    }

    @Test
    void deleteUser() {
        given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .delete("/graph/users")
                .then()
                .statusCode(201);

        assertEquals(1, provisioningRepository.count());
    }

    @Test
    void accept() {
        given()
                .redirects()
                .follow(false)
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .get("/graph/accept/1/2")
                .then()
                .header("Location", "http://localhost:8080/api/v1/users/ms-accept-return/1/2");
    }
}