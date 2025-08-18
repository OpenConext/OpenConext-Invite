package invite.api;

import invite.AbstractTest;
import invite.model.Validation;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.core.IsEqual.equalTo;

class ValidationControllerTest extends AbstractTest {

    @Test
    void validEmail() {
        given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body(new Validation("email", "john@doe.com"))
                .post("/api/v1/validations/validate")
                .then()
                .body("valid", equalTo(true));
    }

    @Test
    void invalidEmail() {
        given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body(new Validation("email", "nope"))
                .post("/api/v1/validations/validate")
                .then()
                .body("valid", equalTo(false));
    }

    @Test
    void validUrl() {
        given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body(new Validation("url", "https://landingpage"))
                .post("/api/v1/validations/validate")
                .then()
                .body("valid", equalTo(true));
    }

    @Test
    void emptyUrl() {
        given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body(new Validation("url", ""))
                .post("/api/v1/validations/validate")
                .then()
                .statusCode(400);
    }

    @Test
    void invalidUrl() {
        given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body(new Validation("url", "nope"))
                .post("/api/v1/validations/validate")
                .then()
                .body("valid", equalTo(false));
    }

    @Test
    void invalidType() {
        given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body(new Validation("nope", "john@doe.com"))
                .post("/api/v1/validations/validate")
                .then()
                .statusCode(400);
    }

    @Override
    protected boolean seedDatabase() {
        return false;
    }
}