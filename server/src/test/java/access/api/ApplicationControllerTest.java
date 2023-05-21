package access.api;

import access.AbstractTest;
import access.AccessCookieFilter;
import access.model.Application;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ApplicationControllerTest extends AbstractTest {

    @Test
    void create() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", "urn:collab:person:example.com:admin");
        Application body = new Application("manage-id", "test-application");
        Application application = given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .contentType(ContentType.JSON)
                .body(body)
                .post("/api/v1/applications")
                .as(Application.class);
        assertNotNull(application.getId());

    }
}