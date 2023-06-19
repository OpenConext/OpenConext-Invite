package provisioning;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SCIMControllerTest {

    @LocalServerPort
    protected int port;

    @BeforeEach
    protected void beforeEach() throws Exception {
        RestAssured.port = port;
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
                .pathParams("id", id)
                .get("/api/scim/v2/users/{id}")
                .as(Map.class);
        String user = (String) result.get("user");
        assertEquals("test", user);

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

    }
}
