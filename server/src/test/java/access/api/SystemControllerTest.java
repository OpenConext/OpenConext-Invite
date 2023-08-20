package access.api;

import access.AbstractTest;
import access.AccessCookieFilter;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static access.Seed.SUPER_SUB;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

class SystemControllerTest extends AbstractTest {

    @Test
    void cron() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", SUPER_SUB);
        given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .contentType(ContentType.JSON)
                .get("/api/v1/system/cron")
                .then()
                .statusCode(200);

    }
}