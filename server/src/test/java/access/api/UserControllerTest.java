package access.api;

import access.AbstractTest;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.Headers;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import io.restassured.http.ContentType;

import static org.junit.jupiter.api.Assertions.*;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

class UserControllerTest extends AbstractTest {

    @Test
    void me() {
        Headers headers = given().redirects().follow(false)
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
//                .auth().oauth2(opaqueAccessToken("inviter@utrecht.nl", "introspect.json"))
                .get("/api/v1/users")
                .headers();
        System.out.println(headers);

    }
}