package access;

import io.restassured.RestAssured;
import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

class AccessApplicationTest {

    @Test
    void main() {
        AccessServerApplication.main(new String[]{"--server.port=8088"});
        RestAssured.port = 8088;

        given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .get("/internal/health")
                .then()
                .body("status", equalTo("UP"));
    }
}