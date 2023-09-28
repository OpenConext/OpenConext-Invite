package access.api;

import access.AbstractTest;
import access.model.Authority;
import access.model.Invitation;
import io.restassured.http.ContentType;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

class EnvironmentControllerTest extends AbstractTest {

    @Test
    void disclaimer() throws IOException {
        InputStream inputStream = given()
                .when()
                .accept("text/css")
                .get("/api/v1/disclaimer")
                .asInputStream();
        String css = IOUtils.toString(inputStream, Charset.defaultCharset());
        assertEquals(css, "body::after {background: red;content: \"LOCAL\";}");
    }
}