package invite;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

class AccessApplicationTest {

    @Test
    void mainApp() {
        ConfigurableApplicationContext applicationContext = SpringApplication.run(AccessServerApplication.class, new String[]{"--server.port=8098"});
        RestAssured.port = 8098;

        given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .get("/internal/health")
                .then()
                .body("status", equalTo("UP"));
        SpringApplication.exit(applicationContext);
    }
}