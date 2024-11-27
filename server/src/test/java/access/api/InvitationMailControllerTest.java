package access.api;

import access.AbstractMailTest;
import access.AccessCookieFilter;
import access.mail.MimeMessageParser;
import access.manage.EntityType;
import access.model.*;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

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

    @Test
    void newInvitationCustomDisplayName() throws Exception {
        //Because the user is changed and provisionings are queried
        stubForManageProvisioning(List.of());
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", MANAGE_SUB);

        stubForManageProviderById(EntityType.SAML20_SP, "1");
        //Wiki role, see AbstractTest#seed
        List<Role> roles = roleRepository.findByApplicationUsagesApplicationManageId("1");
        List<Long> roleIdentifiers = roles.stream()
                .map(Role::getId)
                .toList();
        InvitationRequest invitationRequest = new InvitationRequest(
                Authority.GUEST,
                "Message",
                Language.en,
                true,
                false,
                false,
                false,
                List.of("new@new.nl"),
                roleIdentifiers,
                Instant.now().plus(365, ChronoUnit.DAYS),
                Instant.now().plus(12, ChronoUnit.DAYS));

        given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .contentType(ContentType.JSON)
                .body(invitationRequest)
                .post("/api/v1/invitations")
                .then()
                .statusCode(201);

        List<MimeMessageParser> mimeMessageParsers = super.allMailMessages(1);
        String htmlContent = mimeMessageParsers.getFirst().getHtmlContent();

        assertTrue(htmlContent.contains("wiki@university.com"));
    }

}
