package access.api;

import access.AbstractMailTest;
import access.AccessCookieFilter;
import access.manage.EntityType;
import access.model.AcceptInvitation;
import access.model.Authority;
import access.model.Invitation;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static access.Seed.INVITER_SUB;
import static access.Seed.MANAGE_SUB;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InvitationMailControllerTest extends AbstractMailTest {

    @Test
    void resendInviteMail() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", INVITER_SUB);
        Invitation invitation = invitationRepository.findByHash(Authority.GUEST.name()).get();
        super.stubForManageProviderById(EntityType.OIDC10_RP, "5");
        given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .contentType(ContentType.JSON)
                .pathParam("id", invitation.getId())
                .put("/api/v1/invitations/{id}")
                .then()
                .statusCode(201);
        String htmlContent = super.mailMessage().getHtmlContent();
        assertTrue(htmlContent.contains("Calendar EN"));
        assertTrue(htmlContent.contains("SURF bv"));
        assertTrue(htmlContent.contains("Mail"));
    }
}
