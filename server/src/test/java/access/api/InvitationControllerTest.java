package access.api;

import access.AbstractTest;
import access.AccessCookieFilter;
import access.exception.NotFoundException;
import access.manage.EntityType;
import access.model.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

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
        Map<String, Object> application = role.getApplicationMaps().get(0);
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
        //Because the user is changed and provisionings are queried
        stubForManageProvisioning(List.of());
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", MANAGE_SUB);

        stubForManageProviderById(EntityType.SAML20_SP, "1");

        List<Long> roleIdentifiers = roleRepository.findByApplicationUsagesApplicationManageId("1").stream()
                .map(Role::getId)
                .toList();
        InvitationRequest invitationRequest = new InvitationRequest(
                Authority.GUEST,
                "Message",
                Language.en,
                true,
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
    }

    @Test
    void newInvitationEmptyRoles() throws Exception {
        //Because the user is changed and provisionings are queried
        stubForManageProvisioning(List.of());
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", MANAGE_SUB);

        InvitationRequest invitationRequest = new InvitationRequest(
                Authority.INVITER,
                "Message",
                Language.en,
                true,
                false,
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
        //one role provisioned to 1 remote SCIM
        assertEquals(1, remoteProvisionedGroupRepository.count());
        //one user provisioned to 1 remote SCIM - the invitee. The one existing user is not provisioned because only Guests are provisioned
        assertEquals(1, remoteProvisionedUserRepository.count());
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
    void acceptGraphWithInvalidAliasMail() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", "graph+damn@new.com");
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
        assertEquals("true", results.get("errorResponse"));

        User user = userRepository.findBySubIgnoreCase("graph+damn@new.com").get();
        assertEquals(1, user.getUserRoles().size());
        //no roles provisioned to GRAPH
        assertEquals(0, remoteProvisionedGroupRepository.count());
        //no user provisioned to remote GRAPH because of MS error
        assertEquals(0, remoteProvisionedUserRepository.count());
    }

    @Test
    void acceptForUpgradingExistingUserRole() throws Exception {
        User beforeAcceptUser = userRepository.findBySubIgnoreCase(GUEST_SUB).get();
        Authority authority = beforeAcceptUser.getUserRoles().stream()
                .filter(userRole -> userRole.getRole().getName().equals("Research"))
                .findFirst().get().getAuthority();
        assertEquals(Authority.GUEST, authority);

        //Because the user is changed and provisionings are queried
        stubForManageProvisioning(List.of());
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
        UserRole researchRole = user.getUserRoles().stream()
                .filter(userRole -> userRole.getRole().getName().equals("Research"))
                .findFirst().get();
        Authority upgradedAuthority = researchRole.getAuthority();
        assertEquals(Authority.MANAGER, upgradedAuthority);
        assertTrue(researchRole.isGuestRoleIncluded());
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
    void acceptPatchForDifferentScimIdentifier() throws Exception {
        String externalId = "subject@example.com";
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", "new@prov.com",
                m -> {

                    m.put("subject_id", externalId);
                    return m;
                });
        Invitation invitation = invitationRepository.findByHash(Authority.GUEST.name()).get();
        //Provisioning with id=4 hs different scim_user_identifier
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
        //Now assert the correct scim_identifier was used
        List<LoggedRequest> requests = findAll(postRequestedFor(urlPathMatching("/api/scim/v2/users")));
        assertEquals(1, requests.size());

        Map<String, Object> request = objectMapper.readValue(requests.get(0).getBodyAsString(), new TypeReference<>() {
        }) ;
        assertEquals(externalId, request.get("externalId"));
    }

    @Test
    void acceptInstitutionAdmin() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", "user@new.com");
        Invitation invitation = invitationRepository.findByHash(INSTITUTION_ADMIN_INVITATION_HASH).get();

        super.stubForManageProviderByOrganisationGUID(ORGANISATION_GUID);
        super.stubForManagerProvidersByIdIn(EntityType.SAML20_SP, List.of("2"));

        AcceptInvitation acceptInvitation = new AcceptInvitation(INSTITUTION_ADMIN_INVITATION_HASH, invitation.getId());
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
        assertTrue(user.isInstitutionAdmin());
        assertEquals(ORGANISATION_GUID, user.getOrganizationGUID());

        User me = given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .get("/api/v1/users/me")
                .as(User.class);
        assertTrue(me.isInstitutionAdmin());
        assertEquals(ORGANISATION_GUID, me.getOrganizationGUID());
        assertEquals(3, me.getApplications().size());
        assertEquals("https://mock-idp", me.getInstitution().get("entityid"));
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
        assertEquals(6, invitations.size());
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

    @Test
    void eduIDRequiredLoginOnlyForGuests() throws Exception {
        Invitation invitation = invitationRepository.findByHash(Authority.INVITER.name()).get();
        invitation.setEduIDOnly(true);
        invitationRepository.save(invitation);
        openIDConnectFlow(
                "/api/v1/users/login?force=true&hash=" + Authority.INVITER.name(),
                "urn:collab:person:example.com:admin",
                authorizationUrl -> {
                    MultiValueMap<String, String> queryParams = UriComponentsBuilder.fromUriString(authorizationUrl).build().getQueryParams();
                    assertFalse(queryParams.containsKey("login_hint"));
                },
                m -> m);
    }

}