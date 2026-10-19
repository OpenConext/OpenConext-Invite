package invite.api;

import invite.AbstractTest;
import invite.AccessCookieFilter;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.Base64;
import java.util.Map;

import static io.restassured.RestAssured.given;

class FeedbackControllerTest extends AbstractTest {

    @Test
    void submitFeedbackWithoutScreenshot() throws Exception {
        post(Map.of(
                "message", "This is feedback",
                "url", "http://localhost/home",
                "includeScreenshot", false
        ), HttpStatus.OK);
    }

    @Test
    void submitFeedbackWithScreenshot() throws Exception {
        String base64 = Base64.getEncoder().encodeToString(new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47});
        post(Map.of(
                "message", "Feedback with screenshot",
                "url", "http://localhost/app",
                "includeScreenshot", true,
                "screenshotBase64", base64,
                "screenshotContentType", "image/png"
        ), HttpStatus.OK);
    }

    @Test
    void submitFeedbackRejectsEmptyMessage() throws Exception {
        post(Map.of(
                "message", "",
                "url", "http://localhost/home",
                "includeScreenshot", false
        ), HttpStatus.BAD_REQUEST);
    }

    @Test
    void submitFeedbackRejectsNonPngScreenshot() throws Exception {
        String base64 = Base64.getEncoder().encodeToString("not-a-png".getBytes());
        post(Map.of(
                "message", "Feedback with invalid screenshot",
                "url", "http://localhost/app",
                "includeScreenshot", true,
                "screenshotBase64", base64,
                "screenshotContentType", "text/plain"
        ), HttpStatus.BAD_REQUEST);
    }

    private void post(Map<String, Object> body, HttpStatus expected) throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/me", INVITER_SUB);
        RequestSpecification request = given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .contentType(ContentType.JSON)
                .body(body);
        request.post("/api/v1/feedback")
                .then()
                .statusCode(expected.value());
    }
}
