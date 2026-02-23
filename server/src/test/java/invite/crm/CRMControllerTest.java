package invite.crm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import invite.AbstractMailTest;
import invite.mail.MimeMessageParser;
import invite.manage.EntityType;
import invite.model.Authority;
import invite.model.Role;
import invite.model.User;
import invite.model.UserRole;
import io.restassured.http.ContentType;
import jakarta.mail.Address;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.getAllServeEvents;
import static invite.security.SecurityConfig.API_KEY_HEADER;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

class CRMControllerTest extends AbstractMailTest {

    @Test
    void contactProvisioningNewUser() throws JsonProcessingException {
        CRMRole crmRole = new CRMRole("roleId", "BVW", "Super");
        CRMContact crmContact = getCrmContact(crmRole, "new_user", "hardewijk.org", true);
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

        User user = userRepository.findByCrmContactId("contactId").getFirst();
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
        CRMContact crmContact = getCrmContact(crmRole, "guest", "example.com", true);
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

        User userBefore = userRepository.findBySubIgnoreCase("urn:collab:person:example.com:guest").get();
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

        User user = userRepository.findBySubIgnoreCase("urn:collab:person:example.com:guest").get();
        assertEquals(4, user.getUserRoles().size());

        UserRole userRole = user.getUserRoles().stream().filter(ur -> crmRole.getRoleId().equals(ur.getRole().getCrmRoleId()))
                .findFirst().get();
        assertFalse(userRole.isGuestRoleIncluded());
        assertEquals(Authority.GUEST, userRole.getAuthority());
    }

    @Test
    void contactProvisioningMissingUID() throws JsonProcessingException {
        CRMRole crmRole = new CRMRole("roleId", "BVW", "Super");
        CRMContact crmContact = getCrmContact(crmRole, "new_user", "hardewijk.org", true);
        //This will force the InvalidInputException
        crmContact.setUid("");

        given()
                .when()
                .accept(ContentType.JSON)
                .header(API_KEY_HEADER, "secret")
                .contentType(ContentType.JSON)
                .body(crmContact)
                .post("/api/internal/v1/crm")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    void contactInviteNewUser() throws Exception {
        CRMRole crmRole = new CRMRole("roleId", "BVW", "Super");
        CRMContact crmContact = getCrmContact(crmRole, "new_user", "hardewijk.org", false);
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

        List<User> users = userRepository.findByCrmContactId("contactId");
        assertTrue(users.isEmpty());

        MimeMessageParser mimeMessageParser = mailMessage();
        List<Address> toAddresses = mimeMessageParser.getTo();
        assertEquals(1, toAddresses.size());
        assertEquals("jdoe@example.com", toAddresses.getFirst().toString());
        assertTrue(mimeMessageParser.getHtmlContent()
                .contains("Invitation for Beveiligingsverantwoordelijke for Inc. Corporated at SURFconext Invite"));
    }

    private CRMContact getCrmContact(CRMRole crmRole, String uid, String schacHomeOrganisation, boolean suppressInvitation) {
        return new CRMContact(
                uid,
                schacHomeOrganisation,
                suppressInvitation,
                "contactId",
                "John",
                "from",
                "Doe",
                "jdoe@example.com",
                new CRMOrganisation(
                        "organisationId",
                        "abbrec",
                        "Inc. Corporated"
                ),
                List.of(crmRole)
        );
    }
}