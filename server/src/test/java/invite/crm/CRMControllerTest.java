package invite.crm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import invite.AbstractMailTest;
import invite.mail.MimeMessageParser;
import invite.manage.EntityType;
import invite.model.Authority;
import invite.model.Invitation;
import invite.model.RemoteProvisionedGroup;
import invite.model.RemoteProvisionedUser;
import invite.model.Role;
import invite.model.User;
import invite.model.UserRole;
import io.restassured.http.ContentType;
import jakarta.mail.Address;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.getAllServeEvents;
import static invite.security.SecurityConfig.API_KEY_HEADER;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

class CRMControllerTest extends AbstractMailTest {

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
                .post("/api/internal/v1/crm")
                .then()
                .extract()
                .asString();
        assertEquals("created", response);

        User user = userRepository.findByCrmContactIdAndCrmOrganisationId(crmContactID, crmOrganisationID)
                .get();
        assertEquals(1, user.getUserRoles().size());

        UserRole userRole = user.getUserRoles().iterator().next();
        assertFalse(userRole.isGuestRoleIncluded());
        assertEquals(Authority.GUEST, userRole.getAuthority());

        Role role = userRole.getRole();
        assertEquals(crmRole.getRoleId(), role.getCrmRoleId());
        assertEquals(crmContact.getOrganisation().getOrganisationId(), role.getCrmOrganisationId());

        List<ServeEvent> events = getAllServeEvents().stream().filter(e -> e.getRequest().getUrl().startsWith("/api/scim/v2/")).toList();
        assertEquals(3, events.size());
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
                .post("/api/internal/v1/crm")
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
                .post("/api/internal/v1/crm")
                .then()
                .extract()
                .asString();
        assertEquals("created", response);

        Optional<User> optionalUser = userRepository.findByCrmContactIdAndCrmOrganisationId(crmContactID,
                crmOrganisationID);
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
                .delete("/api/internal/v1/crm")
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
                .post("/api/internal/v1/crm")
                .then()
                .extract()
                .asString();
        assertEquals("created", response);

        Optional<User> optionalUser = userRepository.findByCrmContactIdAndCrmOrganisationId(crmContactID,
                crmOrganisationID);
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
                .post("/api/internal/v1/crm")
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
                .post("/api/internal/v1/crm")
                .then()
                .extract()
                .asString();
        assertEquals("updated", newResponse);

        user = userRepository.findBySubIgnoreCase(GUEST_SUB).get();
        assertEquals(3, user.getUserRoles().size());
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
                .delete("/api/internal/v1/crm")
                .then()
                .extract()
                .asString();
        assertEquals("deleted", response);

        Optional<User> optionalUser = userRepository.findByCrmContactIdAndCrmOrganisationId(CRM_CONTACT_ID, CRM_ORGANIZATION_ID);
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
                .post("/api/internal/v1/crm")
                .then()
                .extract()
                .asString();
        assertEquals("created", response);

        User user = userRepository.findBySubIgnoreCase("urn:collab:person:new.com:total").get();
        assertEquals(1, user.getUserRoles().size());
        //Now post the same CRMContact for a different user, but the same role. Enusure the role is not re-used
        String newOrganisationID = UUID.randomUUID().toString();
        crmContact.setOrganisation(new CRMOrganisation(newOrganisationID,"abbrev","name"));
        crmContact.setUid("second_user");
        response = given()
                .when()
                .accept(ContentType.JSON)
                .header(API_KEY_HEADER, "secret")
                .contentType(ContentType.JSON)
                .body(crmContact)
                .post("/api/internal/v1/crm")
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


}