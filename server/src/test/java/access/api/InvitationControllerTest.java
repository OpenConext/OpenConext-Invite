package access.api;

import access.AbstractTest;
import access.AccessCookieFilter;
import access.manage.EntityType;
import access.model.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@SuppressWarnings("unchecked")
class InvitationControllerTest extends AbstractTest {

    @Test
    void getInvitation() throws JsonProcessingException {
        String body = objectMapper.writeValueAsString(localManage.providerById(EntityType.OIDC10_RP, "5"));
        stubFor(get(urlPathMatching("/manage/api/internal/metadata/oidc10_rp/5")).willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(body)));

        MetaInvitation metaInvitation = given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .queryParam("hash", Authority.GUEST.name())
                .get("/api/v1/invitations/public")
                .as(MetaInvitation.class);

        assertEquals("Mail", metaInvitation.invitation().getRoles().iterator().next().getRole().getName());
        assertEquals(1, metaInvitation.providers().size());
        assertEquals("Calendar EN", ((Map<String, Object>) ((Map<String, Object>) metaInvitation.providers()
                .get(0).get("data")).get("metaDataFields")).get("name:en"));
    }

    @Test
    void getInvitationWrongStatus() {
        updateInvitationStatus(Authority.GUEST.name(), Status.ACCEPTED);

        given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .queryParam("hash", Authority.GUEST.name())
                .get("/api/v1/invitations/public")
                .then()
                .statusCode(409);

    }

    @Test
    void newInvitation() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", "manager@example.com");

        String body = objectMapper.writeValueAsString(localManage.providerById(EntityType.SAML20_SP, "1"));
        stubFor(get(urlPathMatching("/manage/api/internal/metadata/saml20_sp/1")).willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(body)));

        List<Long> roleIdentifiers = roleRepository.findByManageIdIn(Set.of("1")).stream()
                .map(Role::getId)
                .toList();
        InvitationRequest invitationRequest = new InvitationRequest(
                Authority.INVITER,
                "Message",
                true,
                List.of("new@new.nl"),
                roleIdentifiers,
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
    }

    @Test
    void accept() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", "inviter@new.com");
        String hash = Authority.INVITER.name();
        Invitation invitation = invitationRepository.findByHash(hash).get();

        stubForProvisioning(List.of("5"));
        stubForCreateScimUser();
        stubForCreateScimRole();
        stubForUpdateScimRole();

        AcceptInvitation acceptInvitation = new AcceptInvitation(hash, invitation.getId());
        given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .contentType(ContentType.JSON)
                .body(acceptInvitation)
                .post("/api/v1/invitations/accept")
                .then()
                .statusCode(201);
        User user = userRepository.findBySubIgnoreCase("inviter@new.com").get();
        assertEquals(2, user.getUserRoles().size());
        //two roles provisioned to 1 remote SCIM
        assertEquals(2, remoteProvisionedGroupRepository.count());
        //two users provisioned to 1 remote SCIM - the inviter and one existing user with the userRole
        assertEquals(2, remoteProvisionedUserRepository.count());
    }

    @Test
    void acceptEmailInequality() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", "does@not.match");
        String hash = Authority.INVITER.name();
        Invitation invitation = invitationRepository.findByHash(hash).get();

        AcceptInvitation acceptInvitation = new AcceptInvitation(hash, invitation.getId());
        given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .contentType(ContentType.JSON)
                .body(acceptInvitation)
                .post("/api/v1/invitations/accept")
                .then()
                .statusCode(412);
    }

    @Test
    void acceptInvitationAlreadyAccepted() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", "does@not.match");
        String hash = Authority.INVITER.name();
        Invitation invitation = updateInvitationStatus(hash, Status.ACCEPTED);

        AcceptInvitation acceptInvitation = new AcceptInvitation(hash, invitation.getId());
        given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .contentType(ContentType.JSON)
                .body(acceptInvitation)
                .post("/api/v1/invitations/accept")
                .then()
                .statusCode(409);
    }

    private Invitation updateInvitationStatus(String hash, Status status) {
        Invitation invitation = invitationRepository.findByHash(hash).get();
        invitation.setStatus(status);
        invitationRepository.save(invitation);
        return invitation;
    }

    @Test
    void acceptInvitationIdMatch() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", "does@not.match");
        String hash = Authority.INVITER.name();
        AcceptInvitation acceptInvitation = new AcceptInvitation(hash, 0L);
        given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .contentType(ContentType.JSON)
                .body(acceptInvitation)
                .post("/api/v1/invitations/accept")
                .then()
                .statusCode(404);
    }

    @Test
    void acceptInvitationExpired() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", "does@not.match");
        String hash = Authority.INVITER.name();
        Invitation invitation = invitationRepository.findByHash(hash).get();
        invitation.setExpiryDate(Instant.now().minus(5, ChronoUnit.DAYS));
        invitationRepository.save(invitation);

        AcceptInvitation acceptInvitation = new AcceptInvitation(hash, invitation.getId());
        given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .contentType(ContentType.JSON)
                .body(acceptInvitation)
                .post("/api/v1/invitations/accept")
                .then()
                .statusCode(410);
    }

    @Test
    void acceptPatch() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", "new@prov.com");
        Map res = given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .get("/api/v1/users/config")
                .as(Map.class);
        assertFalse((Boolean) res.get("authenticated"));

        Invitation invitation = invitationRepository.findByHash(Authority.MANAGER.name()).get();

        stubForProvisioning(List.of("4"));
        stubForCreateScimUser();
        stubForCreateScimRole();
        stubForUpdateScimRolePatch();

        AcceptInvitation acceptInvitation = new AcceptInvitation(Authority.MANAGER.name(), invitation.getId());
        given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .contentType(ContentType.JSON)
                .body(acceptInvitation)
                .post("/api/v1/invitations/accept")
                .then()
                .statusCode(201);
        User user = userRepository.findBySubIgnoreCase("new@prov.com").get();
        assertEquals(1, user.getUserRoles().size());
        //two roles provisioned to 1 remote SCIM
        assertEquals(1, remoteProvisionedGroupRepository.count());
        //two users provisioned to 1 remote SCIM - the inviter and one existing user with the userRole
        assertEquals(1, remoteProvisionedUserRepository.count());
    }
}