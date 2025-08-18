package invite.internal;

import invite.AbstractTest;
import invite.manage.EntityType;
import invite.model.*;
import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

/*
 * We use the role "Research" as this is mapped to the application with manage identifier "4". In the
 * application.yml we have configured the sp_dashboard user to have access to this application.
 */
class InternalInviteControllerTest extends AbstractTest {

    @Test
    void createWithAPIUser() throws Exception {
        Role role = new Role("Required role name", "Required role description", application("4", EntityType.SAML20_SP),
                365, false, false);

        super.stubForManagerProvidersByIdIn(EntityType.SAML20_SP, List.of("4"));
        super.stubForManageProvisioning(List.of("1"));
        super.stubForCreateScimRole();

        Role newRole = given()
                .when()
                .auth().preemptive().basic("sp_dashboard", "secret")
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body(role)
                .post("/api/internal/invite/roles")
                .as(new TypeRef<>() {
                });
        assertNotNull(newRole.getId());
    }

    @Test
    void createWithAPIUserNotAllowed() {
        Role role = new Role("Required role name", "Required role description", application("3", EntityType.SAML20_SP),
                365, false, false);

        super.stubForManagerProvidersByIdIn(EntityType.SAML20_SP, List.of("3"));

        given()
                .when()
                .auth().preemptive().basic("sp_dashboard", "secret")
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body(role)
                .post("/api/internal/invite/roles")
                .then()
                .statusCode(403);
    }

    @Test
    void updateWithAPIUser() {
        Role role = roleRepository.findByName("Research").get();
        role.setDescription("changed");
        Role newRole = given()
                .when()
                .auth().preemptive().basic("sp_dashboard", "secret")
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body(role)
                .put("/api/internal/invite/roles")
                .as(new TypeRef<>() {
                });
        assertEquals("changed", newRole.getDescription());
    }


    @Test
    void roleByApplication() {
        List<Role> roles = given()
                .when()
                .auth().preemptive().basic("sp_dashboard", "secret")
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .get("/api/internal/invite/roles")
                .as(new TypeRef<>() {
                });

        assertEquals(1, roles.size());
    }

    @Test
    void findRole() {
        Role role = roleRepository.findByName("Research").get();
        Role roleFromDB = given()
                .when()
                .auth().preemptive().basic("sp_dashboard", "secret")
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .pathParams("id", role.getId())
                .get("/api/internal/invite/roles/{id}")
                .as(new TypeRef<>() {
                });

        assertEquals(role.getIdentifier(), roleFromDB.getIdentifier());
    }

    @Test
    void deleteRole() {
        Role role = roleRepository.findByName("Research").get();
        given()
                .when()
                .auth().preemptive().basic("sp_dashboard", "secret")
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .pathParams("id", role.getId())
                .delete("/api/internal/invite/roles/{id}")
                .then()
                .statusCode(204);
        Optional<Role> roleOptional = roleRepository.findByName("Research");
        assertTrue(roleOptional.isEmpty());
    }

    @Test
    void newInvitation() {
        stubForManageProviderById(EntityType.SAML20_SP, "4");
        List<Long> roleIdentifiers = List.of(roleRepository.findByName("Research").get().getId());

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
                null,
                Instant.now().plus(365, ChronoUnit.DAYS),
                Instant.now().plus(12, ChronoUnit.DAYS));

        Map<String, Object> results = given()
                .when()
                .auth().preemptive().basic("sp_dashboard", "secret")
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body(invitationRequest)
                .post("/api/internal/invite/invitations")
                .as(new TypeRef<Map<String, Object>>() {
                });
        assertEquals(201, results.get("status"));
        assertEquals(1, ((List) results.get("recipientInvitationURLs")).size());
    }

    @Test
    void resendInvitation() {
        // This invitation is for the application Research
        Invitation invitation = invitationRepository.findByHash(Authority.MANAGER.name()).get();
        invitation.setExpiryDate(Instant.now().minus(5, ChronoUnit.DAYS));
        invitationRepository.save(invitation);

        given()
                .when()
                .auth().preemptive().basic("sp_dashboard", "secret")
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .pathParam("id", invitation.getId())
                .put("/api/internal/invite/invitations/{id}")
                .then()
                .statusCode(201);

        Invitation savedInvitation = invitationRepository.findByHash(Authority.MANAGER.name()).get();
        assertTrue(savedInvitation.getExpiryDate().isAfter(Instant.now().plus(13, ChronoUnit.DAYS)));
    }

    @Test
    void resendInvitationNotAllowed() {
        // This invitation is for the application Research
        Invitation invitation = invitationRepository.findByHash(Authority.GUEST.name()).get();

        given()
                .when()
                .auth().preemptive().basic("sp_dashboard", "secret")
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .pathParam("id", invitation.getId())
                .put("/api/internal/invite/invitations/{id}")
                .then()
                .statusCode(403);
    }

    @Test
    void userRolesByRole() {
        Long roleId = roleRepository.findByName("Research").get().getId();
        List<UserRole> userRoles = given()
                .when()
                .auth().preemptive().basic("sp_dashboard", "secret")
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .pathParam("roleId", roleId)
                .get("/api/internal/invite/user_roles/{roleId}")
                .as(new TypeRef<>() {
                });
        assertEquals(1, userRoles.size());
    }

}