package invite.internal;

import invite.AbstractTest;
import invite.manage.EntityType;
import invite.model.*;
import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
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
        String path = String.format("/manage/api/internal/search/%s", EntityType.SAML20_IDP.collectionName());
        List<Map<String, Object>> providers = localManage.identityProvidersByInstitutionalGUID("ad93daef-0911-e511-80d0-005056956c1a");
        stubFor(post(urlPathMatching(path)).willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(objectMapper.writeValueAsString(providers))));

        Role newRole = given()
                .when()
                .auth().preemptive().basic("sp_dashboard", "secret")
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body(role)
                .post("/api/external/v1/internal/invite/roles")
                .as(new TypeRef<>() {
                });
        assertNotNull(newRole.getId());
        assertEquals("ad93daef-0911-e511-80d0-005056956c1a", newRole.getOrganizationGUID());
    }

    @Test
    void createWithAPIUserInvalidOrganizationGUID() throws Exception {
        Role role = new Role("Required role name", "Required role description", application("4", EntityType.SAML20_SP),
                365, false, false);
        role.setOrganizationGUID("nope");

        given()
                .when()
                .auth().preemptive().basic("sp_dashboard", "secret")
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body(role)
                .post("/api/external/v1/internal/invite/roles")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @SneakyThrows
    @Test
    void createWithAPIUserNotAllowed() {
        Role role = new Role("Required role name", "Required role description", application("3", EntityType.SAML20_SP),
                365, false, false);

        super.stubForManagerProvidersByIdIn(EntityType.SAML20_SP, List.of("3"));
        String path = String.format("/manage/api/internal/search/%s", EntityType.SAML20_IDP.collectionName());
        List<Map<String, Object>> providers = localManage.identityProvidersByInstitutionalGUID("ad93daef-0911-e511-80d0-005056956c1a");
        stubFor(post(urlPathMatching(path)).willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(objectMapper.writeValueAsString(providers))));
        given()
                .when()
                .auth().preemptive().basic("sp_dashboard", "secret")
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body(role)
                .post("/api/external/v1/internal/invite/roles")
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
                .put("/api/external/v1/internal/invite/roles")
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
                .get("/api/external/v1/internal/invite/roles")
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
                .get("/api/external/v1/internal/invite/roles/{id}")
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
                .delete("/api/external/v1/internal/invite/roles/{id}")
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
                true,
                null,
                false,
                false,
                List.of("new@new.nl"),
                List.of(),
                roleIdentifiers,
                List.of(),
                null,
                Instant.now().plus(365, ChronoUnit.DAYS),
                Instant.now().plus(12, ChronoUnit.DAYS));

        Map<String, Object> results = given()
                .when()
                .auth().preemptive().basic("sp_dashboard", "secret")
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body(invitationRequest)
                .post("/api/external/v1/internal/invite/invitations")
                .as(new TypeRef<Map<String, Object>>() {
                });
        assertEquals(201, results.get("status"));
        assertEquals(1, ((List) results.get("recipientInvitationURLs")).size());
    }

    @Test
    void newInvitationNL() {
        stubForManageProviderById(EntityType.SAML20_SP, "4");
        List<Long> roleIdentifiers = List.of(roleRepository.findByName("Research").get().getId());

        InvitationRequest invitationRequest = new InvitationRequest(
                Authority.GUEST,
                "Message",
                Language.nl,
                true,
                true,
                null,
                false,
                false,
                List.of("new@new.nl"),
                List.of(),
                roleIdentifiers,
                List.of(),
                null,
                Instant.now().plus(365, ChronoUnit.DAYS),
                Instant.now().plus(12, ChronoUnit.DAYS));

        Map<String, Object> results = given()
                .when()
                .auth().preemptive().basic("sp_dashboard", "secret")
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body(invitationRequest)
                .post("/api/external/v1/internal/invite/invitations")
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
                .put("/api/external/v1/internal/invite/invitations/{id}")
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
                .put("/api/external/v1/internal/invite/invitations/{id}")
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
                .get("/api/external/v1/internal/invite/user_roles/{roleId}")
                .as(new TypeRef<>() {
                });
        assertEquals(2, userRoles.size());
    }

    @Test
    void rolesPerOrganizationApplicationId() {
        List<Role> roles = given()
                .when()
                .auth().preemptive().basic("access", "secret")
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .pathParam("organizationGUID", ORGANISATION_GUID)
                //See Role research in AbstractTest#doSeed
                .pathParam("manageId", "4")
                .get("/api/external/v1/internal/invite/roles/{organizationGUID}/{manageId}")
                .as(new TypeRef<>() {
                });

        assertEquals(1, roles.size());

        Role role = roles.getFirst();
        assertEquals("Research", role.getName());
        assertEquals(2L, role.getUserRoleCount());
    }

    @Test
    void rolesSummary() {

        List<Map<String, String>> roles = given()
                .when()
                .auth().preemptive().basic("access", "secret")
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .pathParams("organizationGUID", ORGANISATION_GUID)
                .get("/api/external/v1/internal/invite/roles-summary/{organizationGUID}")
                .as(new TypeRef<>() {
                });

        assertEquals(3, roles.size());
        List.of("name", "description","urn")
                .forEach(attr -> assertTrue(roles.stream().allMatch(role -> role.containsKey(attr))));
    }

    @Test
    void rolesMineInstitutionAdmin() {
        //Institution admin has no user roles, but all roles of the organization
        List<String> names = rolesMine(INSTITUTION_ADMIN_SUB);
        assertEquals(Set.of("Network", "Research", "Wiki"), Set.copyOf(names));
        assertEquals(3, names.size());
    }

    @Test
    void rolesMineApplicationManager() {
        //No user roles, but manages application 6 which is used by the role Storage
        assertEquals(List.of("Storage"), rolesMine(APPLICATION_MANAGER_SUB));
    }

    @Test
    void rolesMineManager() {
        //Only the user role, no institution admin and no applications
        assertEquals(List.of("Wiki"), rolesMine(MANAGE_SUB));
    }

    @Test
    void rolesMineGuest() {
        //The guest only has GUEST user roles, which are filtered out
        assertTrue(rolesMine(GUEST_SUB).isEmpty());
    }

    @Test
    void rolesMineSubIgnoreCase() {
        assertEquals(List.of("Wiki"), rolesMine(MANAGE_SUB.toUpperCase()));
    }

    @Test
    void rolesMineUnknownUser() {
        assertTrue(rolesMine("urn:collab:person:example.com:nope").isEmpty());
    }

    @Test
    void rolesMineForbiddenForSpDashboard() {
        given()
                .when()
                .auth().preemptive().basic("sp_dashboard", "secret")
                .accept(ContentType.JSON)
                .pathParam("sub", MANAGE_SUB)
                .get("/api/external/v1/internal/invite/roles-mine/{sub}")
                .then()
                .statusCode(HttpStatus.FORBIDDEN.value());
    }

    @Test
    void rolesMineHasManageMetaData() {
        List<Role> roles = rolesMineRoles(APPLICATION_MANAGER_SUB);
        assertEquals(1, roles.size());
        List<Map<String, Object>> applicationMaps = roles.getFirst().getApplicationMaps();
        assertEquals(2, applicationMaps.size());
        applicationMaps.forEach(applicationMap -> {
            assertNull(applicationMap.get("unknown"));
            assertNotNull(applicationMap.get("id"));
        });
    }

    private List<String> rolesMine(String sub) {
        return rolesMineRoles(sub).stream().map(Role::getName).toList();
    }

    private List<Role> rolesMineRoles(String sub) {
        //Manage is called for the applications of the returned roles, stub all seeded applications
        stubForManagerProvidersByIdIn(EntityType.SAML20_SP, List.of("1", "2", "3", "4"));
        stubForManagerProvidersByIdIn(EntityType.OIDC10_RP, List.of("5", "6"));
        return given()
                .when()
                .auth().preemptive().basic("access", "secret")
                .accept(ContentType.JSON)
                .pathParam("sub", sub)
                .get("/api/external/v1/internal/invite/roles-mine/{sub}")
                .as(new TypeRef<>() {
                });
    }

}