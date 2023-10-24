package access.api;

import access.AbstractTest;
import access.AccessCookieFilter;
import access.exception.NotFoundException;
import access.manage.EntityType;
import access.model.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static access.Seed.*;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@SuppressWarnings("unchecked")
class InvitationControllerTest extends AbstractTest {

    @Test
    void getInvitation() throws JsonProcessingException {
        super.stubForManageProviderByEntityID(EntityType.OIDC10_RP, "https://calendar");

        Invitation invitation = given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .queryParam("hash", Authority.GUEST.name())
                .get("/api/v1/invitations/public")
                .as(Invitation.class);

        Role role = invitation.getRoles().iterator().next().getRole();
        assertEquals("Mail", role.getName());
        Map<String, Object> application = role.getApplication();
        assertEquals("Calendar EN", application.get("name:en"));
    }

    @Test
    void getInvitationAlreadyAccepted() {
        Invitation invitation = invitationRepository.findByHash(Authority.GUEST.name()).orElseThrow(NotFoundException::new);
        invitation.setStatus(Status.ACCEPTED);
        invitationRepository.save(invitation);
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
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", MANAGE_SUB);

        stubForManageProviderById(EntityType.SAML20_SP, "1");

        List<Long> roleIdentifiers = roleRepository.findByManageIdIn(Set.of("1")).stream()
                .map(Role::getId)
                .toList();
        InvitationRequest invitationRequest = new InvitationRequest(
                Authority.GUEST,
                "Message",
                true,
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
    }

    @Test
    void newInvitationEmptyRoles() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", MANAGE_SUB);

        InvitationRequest invitationRequest = new InvitationRequest(
                Authority.INVITER,
                "Message",
                true,
                false,
                List.of("new@new.nl"),
                Collections.emptyList(),
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
                .statusCode(409);
    }

    @Test
    void accept() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", "user@new.com");
        String hash = Authority.GUEST.name();
        Invitation invitation = invitationRepository.findByHash(hash).get();

        stubForManageProvisioning(List.of("5"));
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
        User user = userRepository.findBySubIgnoreCase("user@new.com").get();
        assertEquals(1, user.getUserRoles().size());
        //one roles provisioned to 1 remote SCIM
        assertEquals(1, remoteProvisionedGroupRepository.count());
        //two users provisioned to 1 remote SCIM - the inviter and one existing user with the userRole
        assertEquals(2, remoteProvisionedUserRepository.count());
    }

    @Test
    void acceptGraph() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", "graph@new.com");
        Invitation invitation = invitationRepository.findByHash(GRAPH_INVITATION_HASH).get();

        stubForManageProvisioning(List.of("2"));
        stubForCreateGraphUser();

        AcceptInvitation acceptInvitation = new AcceptInvitation(GRAPH_INVITATION_HASH, invitation.getId());
        Map<String, String> results = given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .contentType(ContentType.JSON)
                .body(acceptInvitation)
                .post("/api/v1/invitations/accept")
                .as(new TypeRef<>() {
                });
        assertEquals("https://www.google.com", results.get("inviteRedeemUrl"));

        User user = userRepository.findBySubIgnoreCase("graph@new.com").get();
        assertEquals(1, user.getUserRoles().size());
        //no roles provisioned to GRAPH
        assertEquals(0, remoteProvisionedGroupRepository.count());
        //one user provisioned to 1 remote GRAPH
        assertEquals(1, remoteProvisionedUserRepository.count());
    }

    @Test
    void acceptForUpgradingExistingUserRole() throws Exception {
        User beforeAcceptUser = userRepository.findBySubIgnoreCase(GUEST_SUB).get();
        Authority authority = beforeAcceptUser.getUserRoles().stream()
                .filter(userRole -> userRole.getRole().getName().equals("Research"))
                .findFirst().get().getAuthority();
        assertEquals(Authority.GUEST, authority);

        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", GUEST_SUB);
        String hash = Authority.MANAGER.name();
        Invitation invitation = invitationRepository.findByHash(hash).get();

        stubForManageProvisioning(List.of("5"));
        stubForCreateScimUser();

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
        User user = userRepository.findBySubIgnoreCase(GUEST_SUB).get();
        Authority upgradedAuthority = user.getUserRoles().stream()
                .filter(userRole -> userRole.getRole().getName().equals("Research"))
                .findFirst().get().getAuthority();
        assertEquals(Authority.MANAGER, upgradedAuthority);
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

    @Test
    void deleteInvitation() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", INVITER_SUB);
        Invitation invitation = invitationRepository.findByHash(Authority.GUEST.name()).get();
        given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .contentType(ContentType.JSON)
                .pathParam("id", invitation.getId())
                .delete("/api/v1/invitations/{id}")
                .then()
                .statusCode(204);

        assertFalse(invitationRepository.findByHash(Authority.GUEST.name()).isPresent());
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

        Invitation invitation = invitationRepository.findByHash(Authority.GUEST.name()).get();

        stubForManageProvisioning(List.of("4"));
        stubForCreateScimUser();
        stubForCreateScimRole();
        stubForUpdateScimRolePatch();

        AcceptInvitation acceptInvitation = new AcceptInvitation(Authority.GUEST.name(), invitation.getId());
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
        //One role provisioned to 1 remote SCIM
        assertEquals(1, remoteProvisionedGroupRepository.count());
        //One user provisioned to 1 remote SCIM
        assertEquals(1, remoteProvisionedUserRepository.count());
    }

    @Test
    void byRole() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", INVITER_SUB);
        Role mail = roleRepository.findByName("Mail").get(0);
        List<Invitation> invitations = given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .pathParam("roleId", mail.getId())
                .get("/api/v1/invitations/roles/{roleId}")
                .as(new TypeRef<>() {
                });
        assertEquals(2, invitations.size());
    }

    @Test
    void all() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", SUPER_SUB);
        List<Invitation> invitations = given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .get("/api/v1/invitations/all")
                .as(new TypeRef<>() {
                });
        assertEquals(5, invitations.size());
    }

    @Test
    void allByInviter() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", INVITER_SUB);
        List<Invitation> invitations = given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .get("/api/v1/invitations/all")
                .as(new TypeRef<>() {
                });
        assertEquals(2, invitations.size());
    }
}