package access.internal;

import access.AbstractTest;
import access.AccessCookieFilter;
import access.manage.EntityType;
import access.model.Authority;
import access.model.InvitationRequest;
import access.model.Language;
import access.model.Role;
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
        Role role = new Role("New", "New desc", application("4", EntityType.SAML20_SP), 365, false, false);

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
    void newInvitation() throws Exception {
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

}