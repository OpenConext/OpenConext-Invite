package invite.crm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import invite.AbstractMailTest;
import invite.exception.NotFoundException;
import invite.mail.MimeMessageParser;
import invite.manage.EntityType;
import invite.model.Authority;
import invite.model.Invitation;
import invite.model.Organisation;
import invite.model.RemoteProvisionedGroup;
import invite.model.RemoteProvisionedUser;
import invite.model.Role;
import invite.model.User;
import invite.model.UserRole;
import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;
import jakarta.mail.Address;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.getAllServeEvents;
import static invite.security.SecurityConfig.API_KEY_HEADER;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

class CRMControllerTest extends AbstractMailTest {

    public static final String SUPER_ADMIN_NAME = "SUPER_ADMIN_NAME";

    @Test
    void contactProvisioningNewUser() throws JsonProcessingException {
        CRMRole crmRole = new CRMRole("roleId", "BVW", "Super");
        String crmContactID = UUID.randomUUID().toString();
        String crmOrganisationID = UUID.randomUUID().toString();
        CRMContact crmContact = createCrmContact(crmContactID, crmOrganisationID, crmRole, "new_user", "hardewijk.org", true);
        //These two applications are linked to the 'BVW' CRM role
        super.stubForManageProviderByEntityID(EntityType.OIDC10_RP, "https://calendar");
        super.stubForManageProviderByEntityID(EntityType.SAML20_SP, "https://storage");
        //This will return the SCIM provisioning
        super.stubForManageProvisioning(List.of("5"));
        //The actual SCIM provisioning
        super.stubForCreateScimRole();
        super.stubForCreateScimUser();
        super.stubForUpdateScimRole();

        String response = given()
                .when()
                .accept(ContentType.JSON)
                .header(API_KEY_HEADER, "secret")
                .contentType(ContentType.JSON)
                .body(crmContact)
                .post("/crm/profile")
                .then()
                .extract()
                .asString();
        assertEquals("created", response);
        Organisation organisation = organisationRepository.findByCrmOrganisationId(crmOrganisationID)
                .orElseThrow(() -> new NotFoundException("Organisation not found: " + crmOrganisationID));
        User user = userRepository.findByCrmContactIdAndOrganisation(crmContactID, organisation)
                .get();
        assertEquals(1, user.getUserRoles().size());

        UserRole userRole = user.getUserRoles().iterator().next();
        assertFalse(userRole.isGuestRoleIncluded());
        assertEquals(Authority.GUEST, userRole.getAuthority());

        Role role = userRole.getRole();
        assertEquals(crmRole.getRoleId(), role.getCrmRoleId());
        Optional<Organisation> optionalOrganisation = organisationRepository
                .findByCrmOrganisationId(crmContact.getOrganisation().getOrganisationId());
        assertTrue(optionalOrganisation.isPresent());
        Role roleFromDB = roleRepository.findByCrmRoleIdAndOrganisation(role.getCrmRoleId(), organisation).get();
        assertEquals(role.getId(), roleFromDB.getId());

        List<ServeEvent> events = getAllServeEvents().stream().filter(e -> e.getRequest().getUrl().startsWith("/api/scim/v2/")).toList();
        assertEquals(3, events.size());
    }

    @Test
    void contactProvisioningWrongApiHeader() throws JsonProcessingException {
        given()
                .when()
                .accept(ContentType.JSON)
                .header(API_KEY_HEADER, "nope")
                .contentType(ContentType.JSON)
                .body(Map.of())
                .post("/crm/profile")
                .then()
                .statusCode(HttpStatus.FORBIDDEN.value());
    }

    @Test
    void contactProvisioningExistingUser() throws JsonProcessingException {
        CRMRole crmRole = new CRMRole("roleId", "BVW", "Super");
        String crmContactID = UUID.randomUUID().toString();
        String crmOrganisationID = UUID.randomUUID().toString();
        CRMContact crmContact = createCrmContact(crmContactID, crmOrganisationID, crmRole, "guest", "example.com", true);
        //These two applications are linked to the 'BVW' CRM role
        super.stubForManageProviderByEntityID(EntityType.OIDC10_RP, "https://calendar");
        super.stubForManageProviderByEntityID(EntityType.SAML20_SP, "https://storage");
        //This will return the SCIM provisioning
        super.stubForManageProvisioning(List.of("5"));
        //The actual SCIM provisioning -
        super.stubForCreateScimUser();
        super.stubForCreateScimRole();
        super.stubForUpdateScimRole();
        //See "scim_user_identifier": "eduID", in src/main/resources/manage/provisioning.json,"_id": "7",
        super.stubForProvisionEduID(UUID.randomUUID().toString());

        User userBefore = userRepository.findBySubIgnoreCase(GUEST_SUB).get();
        assertEquals(3, userBefore.getUserRoles().size());

        String response = given()
                .when()
                .accept(ContentType.JSON)
                .header(API_KEY_HEADER, "secret")
                .contentType(ContentType.JSON)
                .body(crmContact)
                .post("/crm/profile")
                .then()
                .extract()
                .asString();
        assertEquals("updated", response);

        User user = userRepository.findBySubIgnoreCase(GUEST_SUB).get();
        assertEquals(4, user.getUserRoles().size());

        UserRole userRole = user.getUserRoles().stream().filter(ur -> crmRole.getRoleId().equals(ur.getRole().getCrmRoleId()))
                .findFirst().get();
        assertFalse(userRole.isGuestRoleIncluded());
        assertEquals(Authority.GUEST, userRole.getAuthority());
    }

    @Test
    void contactInviteNewUser() throws Exception {
        CRMRole crmRole = new CRMRole("roleId", "BVW", "Super");
        String crmContactID = UUID.randomUUID().toString();
        String crmOrganisationID = UUID.randomUUID().toString();
        CRMContact crmContact = createCrmContact(crmContactID, crmOrganisationID, crmRole, null, null, false);
        //These two applications are linked to the 'BVW' CRM role
        super.stubForManageProviderByEntityID(EntityType.OIDC10_RP, "https://calendar");
        super.stubForManageProviderByEntityID(EntityType.SAML20_SP, "https://storage");

        String response = given()
                .when()
                .accept(ContentType.JSON)
                .header(API_KEY_HEADER, "secret")
                .contentType(ContentType.JSON)
                .body(crmContact)
                .post("/crm/profile")
                .then()
                .extract()
                .asString();
        assertEquals("created", response);

        Organisation organisation = organisationRepository.findByCrmOrganisationId(crmOrganisationID)
                .orElseThrow(() -> new NotFoundException("Organisation not found: " + crmOrganisationID));
        Optional<User> optionalUser = userRepository.findByCrmContactIdAndOrganisation(crmContactID, organisation);
        assertTrue(optionalUser.isEmpty());

        MimeMessageParser mimeMessageParser = mailMessage();
        List<Address> toAddresses = mimeMessageParser.getTo();
        assertEquals(1, toAddresses.size());
        assertEquals("jdoe@example.com", toAddresses.getFirst().toString());
        assertTrue(mimeMessageParser.getHtmlContent()
                .contains("Invitation for Beveiligingsverantwoordelijke for Inc. Corporated at SURFconext Invite"));

        List<Invitation> invitations = invitationRepository.findByCrmContactIdAndCrmOrganisationId(
                crmContactID, crmOrganisationID);
        assertEquals(1, invitations.size());
        Invitation invitation = invitations.getFirst();
        assertEquals("SURF CRM", invitation.getRemoteApiUser());

        response = given()
                .when()
                .accept(ContentType.JSON)
                .header(API_KEY_HEADER, "secret")
                .contentType(ContentType.JSON)
                .body(crmContact)
                .delete("/crm/profile")
                .then()
                .extract()
                .asString();
        assertEquals("deleted", response);

        invitations = invitationRepository.findByCrmContactIdAndCrmOrganisationId(crmContactID, crmOrganisationID);
        assertEquals(0, invitations.size());
    }

    @Test
    void contactInviteNewUserSuppressEmail() throws Exception {
        CRMRole crmRole = new CRMRole("roleId", "BVW", "Super");
        String crmContactID = UUID.randomUUID().toString();
        String crmOrganisationID = UUID.randomUUID().toString();
        CRMContact crmContact = createCrmContact(crmContactID, crmOrganisationID, crmRole, null, null, true);
        //These two applications are linked to the 'BVW' CRM role
        super.stubForManageProviderByEntityID(EntityType.OIDC10_RP, "https://calendar");
        super.stubForManageProviderByEntityID(EntityType.SAML20_SP, "https://storage");

        String response = given()
                .when()
                .accept(ContentType.JSON)
                .header(API_KEY_HEADER, "secret")
                .contentType(ContentType.JSON)
                .body(crmContact)
                .post("/crm/profile")
                .then()
                .extract()
                .asString();
        assertEquals("created", response);

        Organisation organisation = organisationRepository.findByCrmOrganisationId(crmOrganisationID)
                .orElseThrow(() -> new NotFoundException("Organisation not found: " + crmOrganisationID));
        Optional<User> optionalUser = userRepository.findByCrmContactIdAndOrganisation(crmContactID, organisation);
        assertTrue(optionalUser.isEmpty());

        List<MimeMessageParser> allMailMessages = allMailMessages(0);
        assertEquals(0, allMailMessages.size());
    }

    @Test
    void contactProvisioningRemoveScimRole() throws JsonProcessingException {
        CRMRole crmRoleResearch = new CRMRole("5e17b508-08e4-e811-8100-005056956c1a", "CONBEH", "SURFconextbeheerder");
        CRMRole crmRoleCloud = new CRMRole("cf652619-08e4-e811-8100-005056956c1a", "CONVER", "SURFconextverantwoordelijke");
        String crmContactID = UUID.randomUUID().toString();
        String crmOrganisationID = UUID.randomUUID().toString();
        CRMContact crmContact = createCrmContact(crmContactID, crmOrganisationID, crmRoleResearch, "guest", "example.com", true);
        crmContact.setRoles(List.of(crmRoleCloud, crmRoleResearch));
        //This application is linked to the 'CONBEH' CRM role
        super.stubForManageProviderByEntityID(EntityType.SAML20_SP, "https://research");
        super.stubForManageProviderByEntityID(EntityType.OIDC10_RP, "https://cloud");
        //Ignore the SCIM provisioning
        super.stubForManageProvisioning(List.of());

        User userPre = userRepository.findBySubIgnoreCase(GUEST_SUB).get();
        assertEquals(3, userPre.getUserRoles().size());

        String response = given()
                .when()
                .accept(ContentType.JSON)
                .header(API_KEY_HEADER, "secret")
                .contentType(ContentType.JSON)
                .body(crmContact)
                .post("/crm/profile")
                .then()
                .extract()
                .asString();
        assertEquals("updated", response);

        User user = userRepository.findBySubIgnoreCase(GUEST_SUB).get();
        assertEquals(5, user.getUserRoles().size());

        crmContact.setRoles(List.of());
        String newResponse = given()
                .when()
                .accept(ContentType.JSON)
                .header(API_KEY_HEADER, "secret")
                .contentType(ContentType.JSON)
                .body(crmContact)
                .post("/crm/profile")
                .then()
                .extract()
                .asString();
        assertEquals("updated", newResponse);

        user = userRepository.findBySubIgnoreCase(GUEST_SUB).get();
        assertEquals(3, user.getUserRoles().size());
    }

    @Test
    void contactProvisioningRoleEmptyApplications() {
        CRMRole crmRoleResearch = new CRMRole("ea61793b-c4a9-47a0-9558-40e684ded3be", "EMPTY", "Deprecated");
        String crmContactID = UUID.randomUUID().toString();
        String crmOrganisationID = UUID.randomUUID().toString();
        CRMContact crmContact = createCrmContact(crmContactID, crmOrganisationID, crmRoleResearch, "steven", "nope.com", true);
        String sub = "urn:collab:person:nope.com:steven";
        Optional<User> userOptional = userRepository.findBySubIgnoreCase(sub);
        assertTrue(userOptional.isEmpty());

        String response = given()
                .when()
                .accept(ContentType.JSON)
                .header(API_KEY_HEADER, "secret")
                .contentType(ContentType.JSON)
                .body(crmContact)
                .post("/crm/profile")
                .then()
                .extract()
                .asString();
        assertEquals("created", response);

        userOptional = userRepository.findBySubIgnoreCase(sub);
        assertTrue(userOptional.isPresent());
        assertEquals(0, userOptional.get().getUserRoles().size());
    }

    @Test
    void contactInviteNoInvitationWithNoRoles() throws Exception {
        CRMRole crmRoleResearch = new CRMRole("ea61793b-c4a9-47a0-9558-40e684ded3be", "EMPTY", "Deprecated");
        String crmContactID = UUID.randomUUID().toString();
        String crmOrganisationID = UUID.randomUUID().toString();
        CRMContact crmContact = createCrmContact(crmContactID, crmOrganisationID, crmRoleResearch, null, null, false);
        String sub = "urn:collab:person:nope.com:steven";
        Optional<User> userOptional = userRepository.findBySubIgnoreCase(sub);
        assertTrue(userOptional.isEmpty());

        String response = given()
                .when()
                .accept(ContentType.JSON)
                .header(API_KEY_HEADER, "secret")
                .contentType(ContentType.JSON)
                .body(crmContact)
                .post("/crm/profile")
                .then()
                .extract()
                .asString();
        assertEquals("created", response);

        Organisation organisation = organisationRepository.findByCrmOrganisationId(crmOrganisationID)
                .orElseThrow(() -> new NotFoundException("Organisation not found: " + crmOrganisationID));
        Optional<User> optionalUser = userRepository.findByCrmContactIdAndOrganisation(crmContactID, organisation);
        assertTrue(optionalUser.isEmpty());

        List<Invitation> invitations = invitationRepository.findByCrmContactIdAndCrmOrganisationId(
                crmContactID, crmOrganisationID);
        assertEquals(0, invitations.size());

        List<MimeMessageParser> allMailMessages = allMailMessages(0);
        assertEquals(0, allMailMessages.size());
    }

    @Test
    void deleteUser() throws JsonProcessingException {
        CRMContact crmContact = new CRMContact();
        crmContact.setContactId(CRM_CONTACT_ID);
        crmContact.setOrganisation(new CRMOrganisation(CRM_ORGANIZATION_ID, "abbr", "name"));

        super.stubForManageProvisioning(List.of("5"));
        Role role = roleRepository.findByName("Research").get();
        RemoteProvisionedGroup remoteProvisionedGroup = new RemoteProvisionedGroup(role, UUID.randomUUID().toString(), "7");
        super.remoteProvisionedGroupRepository.save(remoteProvisionedGroup);

        User user = userRepository.findBySubIgnoreCase(KB_USER_SUB).get();
        RemoteProvisionedUser remoteProvisionedUser = new RemoteProvisionedUser(user, UUID.randomUUID().toString(), "7");
        super.remoteProvisionedUserRepository.save(remoteProvisionedUser);
        //Because of the PUT request of the change in the group, all users are fetched and checked if they exists in the remote SCIM
        User guestUser = userRepository.findBySubIgnoreCase(GUEST_SUB).get();
        RemoteProvisionedUser remoteProvisionedUserGuest = new RemoteProvisionedUser(guestUser, UUID.randomUUID().toString(), "7");
        super.remoteProvisionedUserRepository.save(remoteProvisionedUserGuest);

        super.stubForUpdateScimRole();
        super.stubForDeleteScimUser();

        String response = given()
                .when()
                .accept(ContentType.JSON)
                .header(API_KEY_HEADER, "secret")
                .contentType(ContentType.JSON)
                .body(crmContact)
                .delete("/crm/profile")
                .then()
                .extract()
                .asString();
        assertEquals("deleted", response);

        Organisation organisation = organisationRepository.findByCrmOrganisationId(CRM_ORGANIZATION_ID)
                .orElseThrow(() -> new NotFoundException("Organisation not found: " + CRM_ORGANIZATION_ID));
        Optional<User> optionalUser = userRepository.findByCrmContactIdAndOrganisation(CRM_CONTACT_ID, organisation);
        assertTrue(optionalUser.isEmpty());
    }

    @Test
    void scopeInviteRoleToUniqueCRMRoleIdAndOrganizationId() throws JsonProcessingException {
        String converRoleId = "92b2b379-07e4-e811-8100-005056956c1a";
        CRMRole crmRole = new CRMRole(converRoleId, "CONVER", "SURFconextverantwoordelijke");
        String crmContactID = UUID.randomUUID().toString();
        String crmOrganisationID = UUID.randomUUID().toString();
        CRMContact crmContact = createCrmContact(crmContactID, crmOrganisationID, crmRole, "total", "new.com", true);
        //These applications are linked to the 'AAI' CRM role
        super.stubForManageProviderByEntityID(EntityType.OIDC10_RP, "https://cloud");
        //Ignore the SCIM provisioning
        super.stubForManageProvisioning(List.of());

        String response = given()
                .when()
                .accept(ContentType.JSON)
                .header(API_KEY_HEADER, "secret")
                .contentType(ContentType.JSON)
                .body(crmContact)
                .post("/crm/profile")
                .then()
                .extract()
                .asString();
        assertEquals("created", response);

        User user = userRepository.findBySubIgnoreCase("urn:collab:person:new.com:total").get();
        assertEquals(1, user.getUserRoles().size());
        //Now post the same CRMContact for a different user, but the same role. Enusure the role is not re-used
        String newOrganisationID = UUID.randomUUID().toString();
        crmContact.setOrganisation(new CRMOrganisation(newOrganisationID, "abbrev", "name"));
        crmContact.setUid("second_user");
        response = given()
                .when()
                .accept(ContentType.JSON)
                .header(API_KEY_HEADER, "secret")
                .contentType(ContentType.JSON)
                .body(crmContact)
                .post("/crm/profile")
                .then()
                .extract()
                .asString();
        assertEquals("created", response);

        user = userRepository.findBySubIgnoreCase("urn:collab:person:new.com:total").get();
        assertEquals(1, user.getUserRoles().size());

        user = userRepository.findBySubIgnoreCase("urn:collab:person:new.com:second_user").get();
        assertEquals(1, user.getUserRoles().size());

        List<Role> roles = roleRepository.findByCrmRoleId(converRoleId);
        assertEquals(2, roles.size());
    }

    @Test
    void resendInviteMail() throws Exception {
        Invitation invitation = invitationRepository.findByHash(Authority.GUEST.name()).get();
        invitation.setExpiryDate(Instant.now().minus(5, ChronoUnit.DAYS));
        invitation.setCrmContactId(CRM_CONTACT_ID);
        invitation.setCrmOrganisationId(CRM_ORGANIZATION_ID);
        invitationRepository.save(invitation);

        super.stubForManageProviderById(EntityType.OIDC10_RP, "5");
        ResendInvitationResponse resendInvitationResponse = given()
                .when()
                .accept(ContentType.JSON)
                .header(API_KEY_HEADER, "secret")
                .contentType(ContentType.JSON)
                .body(new ResendInvitation(CRM_ORGANIZATION_ID, CRM_CONTACT_ID))
                .post("/crm/api/v1/invite/resend")
                .as(new TypeRef<>() {
                });
        Invitation savedInvitation = invitationRepository.findByHash(Authority.GUEST.name()).get();
        assertTrue(savedInvitation.getExpiryDate().isAfter(Instant.now().plus(13, ChronoUnit.DAYS)));

        MimeMessageParser mailMessage = mailMessage();
        assertEquals("Invitation for Mail at SURFconext Invite", mailMessage.getSubject());
    }

    @Test
    void connectionStatusWithInvitationExistingUser() {
        Invitation invitation = invitationRepository.findByHash(Authority.GUEST.name()).get();
        invitation.setCrmContactId(CRM_CONTACT_ID);
        invitation.setCrmOrganisationId(CRM_ORGANIZATION_ID);
        //This will cause the CRMStatusCode.NotPaired
        invitation.setExpiryDate(Instant.now().minus(600, ChronoUnit.DAYS));
        invitationRepository.save(invitation);

        Map<String, ConnectionStatusResponse> response = given()
                .when()
                .accept(ContentType.JSON)
                .header(API_KEY_HEADER, "secret")
                .contentType(ContentType.JSON)
                .body(new ConnectionStatus(CRM_ORGANIZATION_ID, false))
                .get("/crm/api/v1/profiles")
                .as(new TypeRef<>() {
                });
        assertEquals(1, response.size());
        ConnectionStatusResponse connectionStatusResponse = response.get(CRM_CONTACT_ID);
        assertEquals("gb@kb.nl", connectionStatusResponse.email());
        assertEquals("George Best", connectionStatusResponse.fullname());
        assertEquals(CRMStatusCode.NotPaired.getStatusCode(), connectionStatusResponse.statusCode());
    }

    @Test
    void connectionStatusWithInvitationNewUser() {
        Invitation invitation = invitationRepository.findByHash(Authority.GUEST.name()).get();
        //No user with this contactID exists
        String crmContactId = UUID.randomUUID().toString();
        invitation.setCrmContactId(crmContactId);
        invitation.setCrmOrganisationId(CRM_ORGANIZATION_ID);
        invitationRepository.save(invitation);

        Map<String, ConnectionStatusResponse> response = given()
                .when()
                .accept(ContentType.JSON)
                .header(API_KEY_HEADER, "secret")
                .contentType(ContentType.JSON)
                .body(new ConnectionStatus(CRM_ORGANIZATION_ID, false))
                .get("/crm/api/v1/profiles")
                .as(new TypeRef<>() {
                });
        assertEquals(1, response.size());
        ConnectionStatusResponse connectionStatusResponse = response.get(crmContactId);
        assertEquals(invitation.getEmail(), connectionStatusResponse.email());
        assertNull(connectionStatusResponse.fullname());
        assertEquals(CRMStatusCode.InProcess.getStatusCode(), connectionStatusResponse.statusCode());
    }

    @Test
    void connectionStatusWithUser() {
        Map<String, ConnectionStatusResponse> response = given()
                .when()
                .accept(ContentType.JSON)
                .header(API_KEY_HEADER, "secret")
                .contentType(ContentType.JSON)
                .body(new ConnectionStatus(CRM_ORGANIZATION_ID, true))
                .get("/crm/api/v1/profiles")
                .as(new TypeRef<>() {
                });
        assertEquals(1, response.size());
        ConnectionStatusResponse connectionStatusResponse = response.get(CRM_CONTACT_ID);
        assertEquals(CRMStatusCode.Paired.getStatusCode(), connectionStatusResponse.statusCode());
    }

    @Test
    void connectionStatusWithUnknownOrganisation() {
        Map<String, ConnectionStatusResponse> response = given()
                .when()
                .accept(ContentType.JSON)
                .header(API_KEY_HEADER, "secret")
                .contentType(ContentType.JSON)
                .body(new ConnectionStatus("nope", true))
                .get("/crm/api/v1/profiles")
                .as(new TypeRef<>() {
                });
        assertEquals(0, response.size());
    }

    @Test
    void profileWithUidIdp() {
        this.seedCRMData();
        ProfileResponse profileResponse = given()
                .when()
                .auth().preemptive().basic("pdp", "secret")
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .queryParam("uid", "guest")
                .queryParam("idp", "kb.nl")
                .get("/api/external/v1/invite/crm/profile")
                .as(new TypeRef<>() {
                });
        assertEquals(0, profileResponse.code());
        assertEquals("OK", profileResponse.message());
        assertEquals(1, profileResponse.profiles().size());

        Profile profile = profileResponse.profiles().getFirst();
        assertEquals(CRM_ORGANIZATION_ID, profile.organisation().get("guid"));
        assertEquals(1, profile.authorisations().size());

        Authorisation authorisation = profile.authorisations().getFirst();
        assertEquals("SUPER_ADMIN", authorisation.abbbrevation());
        assertEquals(SUPER_ADMIN_NAME, authorisation.role());
    }

    @Test
    void profileWithGuidRole() {
        this.seedCRMData();
        ProfileResponse profileResponse = given()
                .when()
                .auth().preemptive().basic("pdp", "secret")
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .queryParam("guid", CRM_ORGANIZATION_ID)
                .queryParam("role", SUPER_ADMIN_NAME)
                .get("/api/external/v1/invite/crm/profile")
                .as(new TypeRef<>() {
                });
        assertEquals(0, profileResponse.code());
        assertEquals("OK", profileResponse.message());
        assertEquals(1, profileResponse.profiles().size());

        Profile profile = profileResponse.profiles().getFirst();
        assertEquals(CRM_ORGANIZATION_ID, profile.organisation().get("guid"));
        assertEquals(1, profile.authorisations().size());

        Authorisation authorisation = profile.authorisations().getFirst();
        assertEquals("SUPER_ADMIN", authorisation.abbbrevation());
        assertEquals(SUPER_ADMIN_NAME, authorisation.role());
    }

    @Test
    void emptyProfileWithUidIdp() {
        ProfileResponse profileResponse = given()
                .when()
                .auth().preemptive().basic("pdp", "secret")
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .queryParam("uid", "nope")
                .queryParam("idp", "kb.nl")
                .get("/api/external/v1/invite/crm/profile")
                .as(new TypeRef<>() {
                });
        assertTrue(profileResponse.profiles().isEmpty());
        assertEquals(50, profileResponse.code());
    }

    @Test
    void emptyProfileWithGuidRole() {
        ProfileResponse profileResponse = given()
                .when()
                .auth().preemptive().basic("pdp", "secret")
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .queryParam("guid", CRM_ORGANIZATION_ID)
                .queryParam("role", "nope")
                .get("/api/external/v1/invite/crm/profile")
                .as(new TypeRef<>() {
                });
        assertTrue(profileResponse.profiles().isEmpty());
        assertEquals(50, profileResponse.code());
    }

    @Test
    void emptyProfileWithNoOrg() {
        ProfileResponse profileResponse = given()
                .when()
                .auth().preemptive().basic("pdp", "secret")
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .queryParam("guid", "nope")
                .queryParam("role", SUPER_ADMIN_NAME)
                .get("/api/external/v1/invite/crm/profile")
                .as(new TypeRef<>() {
                });
        assertTrue(profileResponse.profiles().isEmpty());
        assertEquals(50, profileResponse.code());
    }

    @Test
    void emptyProfile() {
        ProfileResponse profileResponse = given()
                .when()
                .auth().preemptive().basic("pdp", "secret")
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .get("/api/external/v1/invite/crm/profile")
                .as(new TypeRef<>() {
                });
        assertTrue(profileResponse.profiles().isEmpty());
        assertEquals(50, profileResponse.code());
    }

    private void seedCRMData() {
        Organisation organisation = organisationRepository.findByCrmOrganisationId(CRM_ORGANIZATION_ID).get();
        Role role = new Role();
        role.setCrmRoleId(UUID.randomUUID().toString());
        role.setCrmRoleAbbrevation("SUPER_ADMIN");
        role.setCrmRoleName(SUPER_ADMIN_NAME);
        role.setName("CRM_ROLE");
        role.setShortName("crm_role");
        role.setOrganisation(organisation);
        role.setIdentifier(UUID.randomUUID().toString());
        roleRepository.save(role);
        User user = userRepository.findBySubIgnoreCase(KB_USER_SUB).get();
        user.addUserRole(new UserRole(Authority.GUEST, role));
        userRepository.save(user);


    }

}