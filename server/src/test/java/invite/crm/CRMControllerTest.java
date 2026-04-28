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
import static org.hamcrest.core.IsEqual.equalTo;
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
        stubForManageProviderByEntityID(EntityType.OIDC10_RP, "https://calendar");
        stubForManageProviderByEntityID(EntityType.SAML20_SP, "https://storage");
        //This will return the SCIM provisioning
        stubForManageProvisioning(List.of("5"));
        //The actual SCIM provisioning
        stubForCreateScimRole();
        stubForCreateScimUser();
        stubForUpdateScimRole();

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
    void contactProvisioningWrongApiHeader() {
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
        stubForManageProviderByEntityID(EntityType.OIDC10_RP, "https://calendar");
        stubForManageProviderByEntityID(EntityType.SAML20_SP, "https://storage");
        //This will return the SCIM provisioning
        stubForManageProvisioning(List.of("5"));
        //The actual SCIM provisioning -
        stubForCreateScimUser();
        stubForCreateScimRole();
        stubForUpdateScimRole();
        //See "scim_user_identifier": "eduID", in src/main/resources/manage/provisioning.json,"_id": "7",
        stubForProvisionEduID(UUID.randomUUID().toString());

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
        stubForManageProviderByEntityID(EntityType.OIDC10_RP, "https://calendar");
        stubForManageProviderByEntityID(EntityType.SAML20_SP, "https://storage");

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

        //Now we send the same POST again, and because of idenpotentcy no new invitation should be created
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
        List<Invitation> invitationsAfterSyncs = invitationRepository.findByCrmContactIdAndCrmOrganisationId(
                crmContactID, crmOrganisationID);
        assertEquals(1, invitationsAfterSyncs.size());
        assertEquals(invitations.getFirst().getId(), invitationsAfterSyncs.getFirst().getId());

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
    void contactInviteNewUserWithRoleAdjustment() throws Exception {
        CRMRole crmRole = new CRMRole("roleId", "BVW", "Super");
        String crmContactID = UUID.randomUUID().toString();
        String crmOrganisationID = UUID.randomUUID().toString();
        CRMContact crmContact = createCrmContact(crmContactID, crmOrganisationID, crmRole, null, null, false);
        //These two applications are linked to the 'BVW' CRM role
        stubForManageProviderByEntityID(EntityType.OIDC10_RP, "https://calendar");
        stubForManageProviderByEntityID(EntityType.SAML20_SP, "https://storage");

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

        deleteMailMessages();
        //Now we send the POST again, but with a different Role
        CRMRole newCrmRole = new CRMRole("differentRoleId", "CONBEH", "SURFconextbeheerder");
        CRMContact newCrmContact = createCrmContact(crmContactID, crmOrganisationID, newCrmRole, null, null, false);
        //This application is linked to the 'CONBEH' CRM role
        stubForManageProviderByEntityID(EntityType.SAML20_SP, "https://research");

        String newResponse = given()
                .when()
                .accept(ContentType.JSON)
                .header(API_KEY_HEADER, "secret")
                .contentType(ContentType.JSON)
                .body(newCrmContact)
                .post("/crm/profile")
                .then()
                .extract()
                .asString();
        assertEquals("created", newResponse);
        List<Invitation> invitationsAfterSyncs = invitationRepository.findByCrmContactIdAndCrmOrganisationId(
                crmContactID, crmOrganisationID);
        assertEquals(1, invitationsAfterSyncs.size());
        assertNotEquals(invitations.getFirst().getId(), invitationsAfterSyncs.getFirst().getId());
        //The previous invitation should be deleted
        assertTrue(invitationRepository.findById(invitations.getFirst().getId()).isEmpty());

        MimeMessageParser latetestMimeMessageParser = mailMessage();
        assertTrue(latetestMimeMessageParser.getHtmlContent()
                .contains("Invitation for SURFconextbeheerder for Inc. Corporated at SURFconext Invite"));

    }

    @Test
    void contactInviteNewUserSuppressEmail() throws Exception {
        CRMRole crmRole = new CRMRole("roleId", "BVW", "Super");
        String crmContactID = UUID.randomUUID().toString();
        String crmOrganisationID = UUID.randomUUID().toString();
        CRMContact crmContact = createCrmContact(crmContactID, crmOrganisationID, crmRole, null, null, true);
        //These two applications are linked to the 'BVW' CRM role
        stubForManageProviderByEntityID(EntityType.OIDC10_RP, "https://calendar");
        stubForManageProviderByEntityID(EntityType.SAML20_SP, "https://storage");

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
        stubForManageProviderByEntityID(EntityType.SAML20_SP, "https://research");
        stubForManageProviderByEntityID(EntityType.OIDC10_RP, "https://cloud");
        //Ignore the SCIM provisioning
        stubForManageProvisioning(List.of());

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

        stubForManageProvisioning(List.of("5"));
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

        stubForUpdateScimRole();
        stubForDeleteScimUser();

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
        stubForManageProviderByEntityID(EntityType.OIDC10_RP, "https://cloud");
        //Ignore the SCIM provisioning
        stubForManageProvisioning(List.of());

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

        stubForManageProviderById(EntityType.OIDC10_RP, "5");
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
        assertEquals("guest", connectionStatusResponse.link().get("uid"));
        assertEquals("kb.nl", connectionStatusResponse.link().get("idp"));
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
    void profileWithIdp() {
        this.seedCRMData();
        ProfileResponse profileResponse = given()
                .when()
                .header(API_KEY_HEADER, "secret")
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .queryParam("idp", "kb.nl")
                .get("/api/profile")
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
    void profileWithUid() {
        this.seedCRMData();
        ProfileResponse profileResponse = given()
                .when()
                .header(API_KEY_HEADER, "secret")
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .queryParam("uid", "guest")
                .get("/api/profile")
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
    void profileWithNoParameters() {
        this.seedCRMData();
        ProfileResponse profileResponse = given()
                .when()
                .header(API_KEY_HEADER, "secret")
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .get("/api/profile")
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
    void profileWithRoleName() {
        this.seedCRMData();
        ProfileResponse profileResponse = given()
                .when()
                .header(API_KEY_HEADER, "secret")
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .queryParam("role", SUPER_ADMIN_NAME)
                .get("/api/profile")
                .as(new TypeRef<>() {
                });
        assertEquals(0, profileResponse.code());
        assertEquals("OK", profileResponse.message());
        assertEquals(1, profileResponse.profiles().size());
    }

    @Test
    void profileWithOrgGUID() {
        this.seedCRMData();
        ProfileResponse profileResponse = given()
                .when()
                .header(API_KEY_HEADER, "secret")
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .queryParam("guid", CRM_ORGANIZATION_ID)
                .get("/api/profile")
                .as(new TypeRef<>() {
                });
        assertEquals(0, profileResponse.code());
        assertEquals("OK", profileResponse.message());
        assertEquals(1, profileResponse.profiles().size());
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
                .queryParam("uid", "nope")
                .get("/api/external/v1/invite/crm/profile")
                .as(new TypeRef<>() {
                });
        assertTrue(profileResponse.profiles().isEmpty());
        assertEquals(50, profileResponse.code());
    }

    @Test
    void organisations() {
        List<CRMOrganisation> organisations = given()
                .when()
                .accept(ContentType.JSON)
                .header(API_KEY_HEADER, "secret")
                .contentType(ContentType.JSON)
                .get("/crm/api/v1/organisations")
                .as(new TypeRef<>() {
                });
        assertEquals(1, organisations.size());
        assertEquals(CRM_ORGANIZATION_ID, organisations.getFirst().getOrganisationId());
    }

    @Test
    void deleteOrganisation() {
        given()
                .when()
                .accept(ContentType.JSON)
                .header(API_KEY_HEADER, "secret")
                .contentType(ContentType.JSON)
                .body(new CRMOrganisation(CRM_ORGANIZATION_ID, "abbr", "name"))
                .delete("/crm/api/v1/organisations")
                .then()
                .statusCode(200)
                .body("status", equalTo("deleted"));
        Optional<Organisation> optionalOrganisation = organisationRepository.findByCrmOrganisationId(CRM_ORGANIZATION_ID);
        assertTrue(optionalOrganisation.isEmpty());
    }

    @Test
    void deleteOrganisationNotFound() {
        given()
                .when()
                .accept(ContentType.JSON)
                .header(API_KEY_HEADER, "secret")
                .contentType(ContentType.JSON)
                .body(new CRMOrganisation("nope", "abbr", "name"))
                .delete("/crm/api/v1/organisations")
                .then()
                .statusCode(400)
                .body("status", equalTo("Organisation with crmOrganisationId nope not found"));
    }

    @Test
    void removeCRMRoles() throws JsonProcessingException {
        this.seedCRMData();

        Organisation organisation = organisationRepository.findByCrmOrganisationId(CRM_ORGANIZATION_ID).get();
        User user = userRepository.findByCrmContactIdAndOrganisation(CRM_CONTACT_ID, organisation).get();
        assertEquals(2, user.getUserRoles().size());

        stubForManageProvisioning(List.of());

        String response = given()
                .when()
                .accept(ContentType.JSON)
                .header(API_KEY_HEADER, "secret")
                .contentType(ContentType.JSON)
                .body(new RemoveRoles(CRM_ORGANIZATION_ID, CRM_CONTACT_ID))
                .post("/crm/api/v1/invite/remove")
                .then()
                .extract()
                .asString();
        assertEquals("removed", response);

        user = userRepository.findByCrmContactIdAndOrganisation(CRM_CONTACT_ID, organisation).get();
        assertEquals(1, user.getUserRoles().size());
    }

    @Test
    void removeCRMRolesNoUserFound() {
        this.seedCRMData();

        String response = given()
                .when()
                .accept(ContentType.JSON)
                .header(API_KEY_HEADER, "secret")
                .contentType(ContentType.JSON)
                .body(new RemoveRoles(CRM_ORGANIZATION_ID, "nope"))
                .post("/crm/api/v1/invite/remove")
                .then()
                .extract()
                .asString();
        assertEquals("removed", response);
    }

    @Test
    void removeCRMRolesNoOrganisationFound() {
        this.seedCRMData();

        String response = given()
                .when()
                .accept(ContentType.JSON)
                .header(API_KEY_HEADER, "secret")
                .contentType(ContentType.JSON)
                .body(new RemoveRoles("nope", "nope"))
                .post("/crm/api/v1/invite/remove")
                .then()
                .extract()
                .asString();
        assertEquals("removed", response);
    }

    @Test
    void sendInvitation() throws Exception {
        CRMRole crmRole = new CRMRole("roleId", "BVW", "Super");
        String crmContactID = UUID.randomUUID().toString();
        String crmOrganisationID = CRM_ORGANIZATION_ID;
        SendInvitation sendInvitation = new SendInvitation(crmOrganisationID, crmContactID, "dpo@example.org", List.of(crmRole));

        //Provisioning is tested on other places
       stubForManageProvisioning(List.of());


        //These two applications are linked to the 'BVW' CRM role
        stubForManageProviderByEntityID(EntityType.SAML20_SP, "https://storage");
        stubForManageProviderByEntityID(EntityType.OIDC10_RP, "https://calendar");

         stubForManageProviderById(EntityType.OIDC10_RP, "5");
         stubForManageProviderById(EntityType.SAML20_SP, "3");
      //  stubForManageProvidersAllowedByIdP(ORGANISATION_GUID);

        String response = given()
                .when()
                .accept(ContentType.JSON)
                .header(API_KEY_HEADER, "secret")
                .contentType(ContentType.JSON)
                .body(sendInvitation)
                .post("/crm/api/v1/invite/send")
                .then()
                .extract()
                .asString();
        assertEquals("send", response);

        Organisation organisation = organisationRepository.findByCrmOrganisationId(crmOrganisationID)
                .orElseThrow(() -> new NotFoundException("Organisation not found: " + crmOrganisationID));
        Optional<User> optionalUser = userRepository.findByCrmContactIdAndOrganisation(crmContactID, organisation);
        assertTrue(optionalUser.isEmpty());

        MimeMessageParser mimeMessageParser = mailMessage();
        List<Address> toAddresses = mimeMessageParser.getTo();
        assertEquals(1, toAddresses.size());
        assertEquals("dpo@example.org", toAddresses.getFirst().toString());
        String htmlContent = mimeMessageParser.getHtmlContent();
        assertTrue(htmlContent
                .contains("Invitation for Beveiligingsverantwoordelijke"));

        List<Invitation> invitations = invitationRepository.findByCrmContactIdAndCrmOrganisationId(
                crmContactID, crmOrganisationID);
        assertEquals(1, invitations.size());
        Invitation invitation = invitations.getFirst();
        assertEquals("SURF CRM", invitation.getRemoteApiUser());

        //Now we send the same POST again, and because of idenpotentcy no new invitation should be created
        String newResponse = given()
                .when()
                .accept(ContentType.JSON)
                .header(API_KEY_HEADER, "secret")
                .contentType(ContentType.JSON)
                .body(sendInvitation)
                .post("/crm/api/v1/invite/send")
                .then()
                .extract()
                .asString();
        assertEquals("send", newResponse);
        List<Invitation> invitationsAfterSyncs = invitationRepository.findByCrmContactIdAndCrmOrganisationId(
                crmContactID, crmOrganisationID);
        assertEquals(1, invitationsAfterSyncs.size());
        assertEquals(invitations.getFirst().getId(), invitationsAfterSyncs.getFirst().getId());
    }

    @Test
    void sendInvitationWithoutRoles() {
        String crmContactID = UUID.randomUUID().toString();
        String crmOrganisationID = UUID.randomUUID().toString();
        SendInvitation sendInvitation = new SendInvitation(crmOrganisationID, crmContactID, "dpo@example.org", List.of());

        given()
                .when()
                .accept(ContentType.JSON)
                .header(API_KEY_HEADER, "secret")
                .contentType(ContentType.JSON)
                .body(sendInvitation)
                .post("/crm/api/v1/invite/send")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    void sendInvitationUnknownOrganisation() {
        CRMRole crmRole = new CRMRole("roleId", "BVW", "Super");
        String crmContactID = UUID.randomUUID().toString();
        String crmOrganisationID = UUID.randomUUID().toString();
        SendInvitation sendInvitation = new SendInvitation(crmOrganisationID, crmContactID, "dpo@example.org", List.of(crmRole));

        given()
                .when()
                .accept(ContentType.JSON)
                .header(API_KEY_HEADER, "secret")
                .contentType(ContentType.JSON)
                .body(sendInvitation)
                .post("/crm/api/v1/invite/send")
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
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