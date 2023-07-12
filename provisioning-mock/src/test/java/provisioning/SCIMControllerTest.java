package provisioning;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import provisioning.repository.ProvisioningRepository;

import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SCIMControllerTest {

    @LocalServerPort
    protected int port;

    @Autowired
    protected ProvisioningRepository provisioningRepository;

    @BeforeEach
    protected void beforeEach() {
        RestAssured.port = port;
        provisioningRepository.deleteAllInBatch();
    }

    @Test
    void user() {
        Map result = given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body(Map.of("user", "test"))
                .post("/api/scim/v2/users")
                .as(Map.class);
        String id = (String) result.get("id");
        assertNotNull(id);

        result = given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body(Map.of("user", "test"))
                .pathParams("id", id)
                .put("/api/scim/v2/users/{id}")
                .as(Map.class);
        assertNotNull(result.get("id"));

        given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .pathParams("id", id)
                .delete("/api/scim/v2/users/{id}")
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
                .patch("api/scim/v2/groups/{id}")
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
                .put("api/scim/v2/groups/{id}")
                .as(Map.class);
        assertNotNull(result.get("id"));
        assertEquals(1, provisioningRepository.count());
    }
}
