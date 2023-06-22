package access.api;

import access.AbstractTest;
import access.model.Authority;
import access.model.MetaInvitation;
import access.model.Validation;
import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.*;

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