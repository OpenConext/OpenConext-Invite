package invite.internal;

import invite.AbstractTest;
import invite.manage.EntityType;
import invite.model.*;
import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
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

    private ValidatableResponse createRole(String username, String password, String organizationGUID, String manageId, EntityType entityType) throws Exception {
        Role role = new Role("Required role name", "Required role description", application(manageId, entityType),
                365, false, false);
        role.setOrganizationGUID(organizationGUID);

        super.stubForManagerProvidersByIdIn(entityType, List.of(manageId));
        super.stubForManageProvisioning(List.of());
        stubFor(post(urlPathMatching("/manage/api/internal/search/" + EntityType.SAML20_IDP.collectionName()))
                .willReturn(okJson("[]")));
        //Only return the identity providers of the requested organization
        for (String guid : List.of(ORGANISATION_GUID, "test_institution_guid")) {
            stubFor(post(urlPathMatching("/manage/api/internal/search/" + EntityType.SAML20_IDP.collectionName()))
                    .withRequestBody(containing(guid))
                    .willReturn(okJson(objectMapper.writeValueAsString(localManage.identityProvidersByInstitutionalGUID(guid)))));
        }
        return given()
                .when()
                .auth().preemptive().basic(username, password)
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body(role)
                .post("/api/external/v1/internal/invite/roles")
                .then();
    }

    @Test
    void createWithAPIUserForeignOrganizationGUID() throws Exception {
        long before = roleRepository.count();
        //Existing organization of another institution
        createRole("sp_dashboard", "secret", "test_institution_guid", "4", EntityType.SAML20_SP)
                .statusCode(HttpStatus.FORBIDDEN.value());
        assertEquals(before, roleRepository.count());
    }

    @Test
    void createWithAPIUserOwnOrganizationGUID() throws Exception {
        for (String organizationGUID : Arrays.asList(null, "", ORGANISATION_GUID)) {
            Role role = createRole("sp_dashboard", "secret", organizationGUID, "4", EntityType.SAML20_SP)
                    .statusCode(HttpStatus.CREATED.value())
                    .extract().as(Role.class);
            assertEquals(ORGANISATION_GUID, role.getOrganizationGUID());
        }
    }

    @Test
    void createWithAccessForeignOrganizationGUID() throws Exception {
        Role role = createRole("access_dashboard", "invite_access_dashboard_secret", "test_institution_guid", "6", EntityType.OIDC10_RP)
                .statusCode(HttpStatus.CREATED.value())
                .extract().as(Role.class);
        assertEquals("test_institution_guid", role.getOrganizationGUID());
    }

    @Test
    void createWithAccessUnknownOrganizationGUID() throws Exception {
        createRole("access_dashboard", "invite_access_dashboard_secret", "nope", "6", EntityType.OIDC10_RP)
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("message", containsString("nope"));
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

    private InvitationRequest privilegedInvitationRequest(Authority authority, List<invite.manage.ManageIdentifier> manageIdentifiers) {
        return new InvitationRequest(authority, "Message", Language.en, true, true, null, false, false,
                List.of("new@new.nl"), List.of(), List.of(), manageIdentifiers, "ad93daef-0911-e511-80d0-005056956c1a",
                Instant.now().plus(365, ChronoUnit.DAYS), Instant.now().plus(12, ChronoUnit.DAYS));
    }

    @Test
    void newInvitationPrivilegedAuthorityNotAllowedForSpDashboard() {
        for (Authority authority : List.of(Authority.SUPER_USER, Authority.INSTITUTION_ADMIN)) {
            given()
                    .when()
                    .auth().preemptive().basic("sp_dashboard", "secret")
                    .accept(ContentType.JSON)
                    .contentType(ContentType.JSON)
                    .body(privilegedInvitationRequest(authority, List.of()))
                    .post("/api/external/v1/internal/invite/invitations")
                    .then()
                    .statusCode(HttpStatus.FORBIDDEN.value());
        }
    }

    @Test
    void newInvitationApplicationManagerForForeignApplicationNotAllowed() {
        long before = applicationRepository.count();
        given()
                .when()
                .auth().preemptive().basic("sp_dashboard", "secret")
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body(privilegedInvitationRequest(Authority.APPLICATION_MANAGER,
                        List.of(new invite.manage.ManageIdentifier("9999", EntityType.SAML20_SP))))
                .post("/api/external/v1/internal/invite/invitations")
                .then()
                .statusCode(HttpStatus.FORBIDDEN.value());
        assertEquals(before, applicationRepository.count());
    }

    @Test
    void updateRoleOfForeignApplicationNotAllowed() {
        Role foreign = roleRepository.findAll().stream()
                .filter(r -> r.applicationsUsed().stream().noneMatch(a -> "4".equals(a.getManageId())))
                .findFirst().orElseThrow();
        Role body = roleRepository.findByName("Research").get();
        //Attack: the id of a role of another application, combined with the applicationUsages of an owned application
        body.setId(foreign.getId());
        given()
                .when()
                .auth().preemptive().basic("sp_dashboard", "secret")
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body(body)
                .put("/api/external/v1/internal/invite/roles")
                .then()
                .statusCode(HttpStatus.FORBIDDEN.value());
    }

    @Test
    void updateRoleDoesNotChangeProtectedFields() {
        Role role = roleRepository.findByName("Research").get();
        String urn = role.getUrn();
        role.setUrn("urn:evil");
        role.setCrmRoleId("crm");
        role.setCrmRoleName("evil");
        role.setRemoteApiUser("spoofed");
        given()
                .when()
                .auth().preemptive().basic("sp_dashboard", "secret")
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body(role)
                .put("/api/external/v1/internal/invite/roles")
                .then()
                .statusCode(HttpStatus.CREATED.value());
        Role reloaded = roleRepository.findByName("Research").get();
        assertEquals(urn, reloaded.getUrn());
        assertNull(reloaded.getCrmRoleId());
        assertNotEquals("spoofed", reloaded.getRemoteApiUser());
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
                .auth().preemptive().basic("access_dashboard", "invite_access_dashboard_secret")
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
                .auth().preemptive().basic("access_dashboard", "invite_access_dashboard_secret")
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

    @Test
    void profile() {
        User user = given()
                .when()
                .auth().preemptive().basic("access_dashboard", "invite_access_dashboard_secret")
                .accept(ContentType.JSON)
                .pathParam("sub", MANAGE_SUB)
                .get("/api/external/v1/internal/invite/profile/{sub}")
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .as(User.class);
        assertEquals(MANAGE_SUB, user.getSub());
    }

    @Test
    void profileIgnoreCase() {
        User user = given()
                .when()
                .auth().preemptive().basic("access_dashboard", "invite_access_dashboard_secret")
                .accept(ContentType.JSON)
                .pathParam("sub", MANAGE_SUB.toUpperCase())
                .get("/api/external/v1/internal/invite/profile/{sub}")
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .as(User.class);
        assertEquals(MANAGE_SUB, user.getSub());
    }

    @Test
    void profileNotFound() {
        given()
                .when()
                .auth().preemptive().basic("access_dashboard", "invite_access_dashboard_secret")
                .accept(ContentType.JSON)
                .pathParam("sub", "urn:collab:person:example.com:nope")
                .get("/api/external/v1/internal/invite/profile/{sub}")
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    void profileForbiddenForSpDashboard() {
        given()
                .when()
                .auth().preemptive().basic("sp_dashboard", "secret")
                .accept(ContentType.JSON)
                .pathParam("sub", MANAGE_SUB)
                .get("/api/external/v1/internal/invite/profile/{sub}")
                .then()
                .statusCode(HttpStatus.FORBIDDEN.value());
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
                .auth().preemptive().basic("access_dashboard", "invite_access_dashboard_secret")
                .accept(ContentType.JSON)
                .pathParam("sub", sub)
                .get("/api/external/v1/internal/invite/roles-mine/{sub}")
                .as(new TypeRef<>() {
                });
    }

    @Test
    void resendInvitationWithoutRolesNotAllowed() {
        for (Authority authority : List.of(Authority.SUPER_USER, Authority.APPLICATION_MANAGER)) {
            Invitation invitation = invitationRepository.findByHash(authority.name()).get();
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
                    .statusCode(HttpStatus.FORBIDDEN.value());
            assertTrue(invitationRepository.findByHash(authority.name()).get().getExpiryDate().isBefore(Instant.now()));
        }
    }

    @Test
    void applicationManagerInvitationForOwnedApplication() {
        long before = invitationRepository.count();
        InvitationResponse response = given()
                .when()
                .auth().preemptive().basic("sp_dashboard", "secret")
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body(privilegedInvitationRequest(Authority.APPLICATION_MANAGER,
                        List.of(new invite.manage.ManageIdentifier("4", EntityType.SAML20_SP))))
                .post("/api/external/v1/internal/invite/invitations")
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .extract().as(InvitationResponse.class);
        assertEquals(before + 1, invitationRepository.count());

        given()
                .when()
                .auth().preemptive().basic("sp_dashboard", "secret")
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .pathParam("id", response.getInvitationIdentifiers().getFirst())
                .put("/api/external/v1/internal/invite/invitations/{id}")
                .then()
                .statusCode(HttpStatus.CREATED.value());
    }

    @Test
    void resendInvitationWithoutRolesAllowedForAccess() {
        Invitation invitation = invitationRepository.findByHash(Authority.SUPER_USER.name()).get();
        given()
                .when()
                .auth().preemptive().basic("access_dashboard", "invite_access_dashboard_secret")
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .pathParam("id", invitation.getId())
                .put("/api/external/v1/internal/invite/invitations/{id}")
                .then()
                .statusCode(HttpStatus.CREATED.value());
    }

    @Test
    void newGuestInvitationWithForeignApplicationNotAllowed() {
        long before = applicationRepository.count();
        InvitationRequest invitationRequest = privilegedInvitationRequest(Authority.GUEST,
                List.of(new invite.manage.ManageIdentifier("9999", EntityType.SAML20_SP)));
        invitationRequest.setRoleIdentifiers(List.of(roleRepository.findByName("Research").get().getId()));
        given()
                .when()
                .auth().preemptive().basic("sp_dashboard", "secret")
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body(invitationRequest)
                .post("/api/external/v1/internal/invite/invitations")
                .then()
                .statusCode(HttpStatus.FORBIDDEN.value());
        assertEquals(before, applicationRepository.count());
    }

    @Test
    void roleWithoutApplicationsNotAllowed() {
        Role role = new Role("Empty", "desc", new java.util.HashSet<>(), 365, false, false);
        role.setOrganizationGUID("other");
        Long roleId = roleRepository.save(role).getId();

        given()
                .when()
                .auth().preemptive().basic("sp_dashboard", "secret")
                .accept(ContentType.JSON)
                .pathParam("roleId", roleId)
                .get("/api/external/v1/internal/invite/user_roles/{roleId}")
                .then()
                .statusCode(HttpStatus.FORBIDDEN.value());
        given()
                .when()
                .auth().preemptive().basic("sp_dashboard", "secret")
                .accept(ContentType.JSON)
                .pathParam("id", roleId)
                .delete("/api/external/v1/internal/invite/roles/{id}")
                .then()
                .statusCode(HttpStatus.FORBIDDEN.value());
        assertTrue(roleRepository.existsById(roleId));
    }

    @Test
    void updateCrmRoleKeepsOrganisation() throws Exception {
        stubForManagerProvidersByIdIn(EntityType.OIDC10_RP, List.of("5"));
        stubForManageProvisioning(List.of());
        Role role = roleRepository.findByName("Mail").get();
        String crmRoleId = role.getCrmRoleId();
        role.setCrmRoleId(null);
        given()
                .when()
                .auth().preemptive().basic("access_dashboard", "invite_access_dashboard_secret")
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body(role)
                .put("/api/external/v1/internal/invite/roles")
                .then()
                .statusCode(HttpStatus.CREATED.value());
        assertEquals(crmRoleId, roleRepository.findByName("Mail").get().getCrmRoleId());
        assertNotNull(roleRepository.findByCrmRoleIdAndOrganisation(crmRoleId,
                organisationRepository.findByCrmOrganisationId(CRM_ORGANIZATION_ID).get()).get());
    }

}
