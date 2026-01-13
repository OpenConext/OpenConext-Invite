package invite.api;

import invite.AbstractTest;
import invite.AccessCookieFilter;
import invite.DefaultPage;
import invite.manage.EntityType;
import invite.model.*;
import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static invite.security.SecurityConfig.API_TOKEN_HEADER;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

class RoleControllerTest extends AbstractTest {

    @Test
    void createBySuperUser() throws Exception {
        //Because the user is changed and provisionings are queried
        stubForManageProvisioning(List.of());
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", SUPER_SUB);
        RoleRequest roleRequest = new RoleRequest("New", "New desc", 365, null, false, false, false, true, "ad93daef-0911-e511-80d0-005056956c1a", "From me", application("1", EntityType.SAML20_SP));

        super.stubForManagerProvidersByIdIn(EntityType.SAML20_SP, List.of("1"));
        super.stubForManageProvisioning(List.of("1"));
        super.stubForCreateScimRole();

        Map result = given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .contentType(ContentType.JSON)
                .body(roleRequest)
                .post("/api/v1/roles")
                .as(Map.class);
        assertNotNull(result.get("id"));
        Role roleFromDB = roleRepository.findById(Long.valueOf((Integer) result.get("id"))).get();
        assertEquals(roleRequest.getOrganizationGUID(), roleFromDB.getOrganizationGUID());
        assertEquals(roleRequest.getDefaultExpiryDays(), roleFromDB.getDefaultExpiryDays());
    }

    @Test
    void createByInstitutionAdmin() throws Exception {
        //Because the user is changed and provisionings are queried
        stubForManageProvidersAllowedByIdP(ORGANISATION_GUID);
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", INSTITUTION_ADMIN_SUB);

        Instant defaultExpiryDate = Instant.now().plus(365, ChronoUnit.DAYS);
        RoleRequest roleRequest = new RoleRequest("New", "New desc", null, defaultExpiryDate,
                false, false, false, true,
                UUID.randomUUID().toString(), "From me", application("1", EntityType.SAML20_SP));

        super.stubForManagerProvidersByIdIn(EntityType.SAML20_SP, List.of("1"));
        super.stubForManageProvisioning(List.of("1"));
        super.stubForCreateScimRole();

        Map result = given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .contentType(ContentType.JSON)
                .body(roleRequest)
                .post("/api/v1/roles")
                .as(Map.class);
        assertNotNull(result.get("id"));
        Role roleFromDB = roleRepository.findById(Long.valueOf((Integer) result.get("id"))).get();
        assertEquals(ORGANISATION_GUID, roleFromDB.getOrganizationGUID());
        assertEquals(roleRequest.getDefaultExpiryDate().truncatedTo(ChronoUnit.DAYS),
                roleFromDB.getDefaultExpiryDate().truncatedTo(ChronoUnit.DAYS));
    }

    @Test
    void createInvalidApplicationUsages() throws Exception {
        //Because the user is changed and provisionings are queried
        stubForManageProvisioning(List.of());
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", SUPER_SUB);
        RoleRequest roleRequest = new RoleRequest("New", "New desc", 365,null,
                false, false, false, true,
                null, "From me", Set.of());


        given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .contentType(ContentType.JSON)
                .body(roleRequest)
                .post("/api/v1/roles")
                .then()
                .statusCode(400);
    }

    @Test
    void createInvalidApplicationLandingPage() throws Exception {
        //Because the user is changed and provisionings are queried
        stubForManageProvisioning(List.of());
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", SUPER_SUB);
        Set<ApplicationUsage> applicationUsages = application("1", EntityType.SAML20_SP);
        applicationUsages.iterator().next().setLandingPage("nope");
        RoleRequest roleRequest = new RoleRequest("New", "New desc", 365, null,
                false, false, false, true,
                null, "From me", applicationUsages);

        given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .contentType(ContentType.JSON)
                .body(roleRequest)
                .post("/api/v1/roles")
                .then()
                .statusCode(400);
    }

    @Test
    void createInvalidLandingPage() throws Exception {
        //Because the user is changed and provisionings are queried
        stubForManageProvisioning(List.of());
        stubForManageProvidersAllowedByIdP(ORGANISATION_GUID);

        Application application = applicationRepository.findByManageIdAndManageTypeOrderById("1", EntityType.SAML20_SP).
                orElseGet(() -> applicationRepository.save(new Application("1", EntityType.SAML20_SP)));
        Set<ApplicationUsage> applications = Set.of(new ApplicationUsage(application, "bogus"));
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", INSTITUTION_ADMIN_SUB);
        Role role = new Role("New", "New desc", applications, 365, false, false);

        given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .contentType(ContentType.JSON)
                .body(role)
                .post("/api/v1/roles")
                .then()
                .statusCode(400);
    }

    @Test
    void createProvisionException() throws Exception {
        //Because the user is changed and provisionings are queried
        stubForManageProvisioning(List.of());
        stubForManageProvidersAllowedByIdP(ORGANISATION_GUID);

        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", INSTITUTION_ADMIN_SUB);
        Role role = new Role("New", "New desc", application("1", EntityType.SAML20_SP), 365, false, false);
        super.stubForManagerProvidersByIdIn(EntityType.SAML20_SP, List.of("1"));
        super.stubForManageProvisioning(List.of("1"));

        Map result = given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .contentType(ContentType.JSON)
                .body(role)
                .post("/api/v1/roles")
                .as(Map.class);
        assertNotNull(result.get("reference"));
    }

    @Test
    void update() throws Exception {
        //Because the user is changed and provisionings are queried
        stubForManageProvisioning(List.of());
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", MANAGE_SUB);

        super.stubForManagerProvidersByIdIn(EntityType.SAML20_SP, List.of("1"));
        super.stubForManageProvisioning(List.of("1"));
        Role roleDB = roleRepository.search("wiki", 1).get(0);
        roleDB.setDescription("changed");
        roleDB.setShortName("changed");

        Role updated = given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .contentType(ContentType.JSON)
                .body(roleDB)
                .put("/api/v1/roles")
                .as(Role.class);
        assertEquals("changed", updated.getDescription());
        assertEquals("wiki", updated.getShortName());
    }

    @Test
    void updateApplications() throws Exception {
        //Because the user is changed and provisionings are queried
        stubForManageProvisioning(List.of());
        stubForManageProvidersAllowedByIdP(ORGANISATION_GUID);

        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", INSTITUTION_ADMIN_SUB);

        super.stubForManagerProvidersByIdIn(EntityType.SAML20_SP, List.of("1", "2", "4"));
        super.stubForManageProvisioning(List.of("1", "2", "4"));
        super.stubForCreateGraphUser();
        super.stubForCreateScimUser();
        super.stubForCreateScimRole();
        super.stubForUpdateScimRole();
        super.stubForUpdateScimRolePatch();
        super.stubForDeleteScimRole();

        Role roleDB = roleRepository.search("Wiki", 1).get(0);
        roleDB.setApplicationUsages(Set.of(
                new ApplicationUsage(new Application("1", EntityType.SAML20_SP), "https://landingpage.com"),
                new ApplicationUsage(new Application("4", EntityType.SAML20_SP), "https://landingpage.com"))
        );
        String body = super.objectMapper.writeValueAsString(roleDB);
        Role updated = given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .contentType(ContentType.JSON)
                .body(body)
                .put("/api/v1/roles")
                .as(Role.class);
        //Because the applicationUsages are mutable for institution admins
        assertEquals(2, updated.getApplicationUsages().size());
    }

    @Test
    void updateApplicationsImmutableApplicationsForManager() throws Exception {
        //Because the user is changed and provisionings are queried
        stubForManageProvisioning(List.of());
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", MANAGE_SUB);

        super.stubForManagerProvidersByIdIn(EntityType.SAML20_SP, List.of("1", "2", "4"));
        super.stubForManageProvisioning(List.of("1", "2", "4"));
        super.stubForCreateScimRole();
        super.stubForDeleteScimRole();

        Role roleDB = roleRepository.search("Wiki", 1).get(0);
        assertEquals(1, roleDB.getApplicationUsages().size());
        roleDB.setApplicationUsages(Set.of(
                new ApplicationUsage(new Application("1", EntityType.SAML20_SP), "https://landingpage.com"),
                new ApplicationUsage(new Application("4", EntityType.SAML20_SP), "https://landingpage.com"))
        );
        String body = super.objectMapper.writeValueAsString(roleDB);
        Role updated = given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .contentType(ContentType.JSON)
                .body(body)
                .put("/api/v1/roles")
                .as(Role.class);
        //Because the applicationUsages are immutable for managers
        assertEquals(1, updated.getApplicationUsages().size());
    }

    @Test
    void rolesByApplicationInstitutionAdminByAPI() throws Exception {
        super.stubForManagerProvidersByIdIn(EntityType.SAML20_SP, List.of("1", "2"));
        super.stubForManagerProvidersByIdIn(EntityType.OIDC10_RP, List.of("5"));
        super.stubForManageProvidersAllowedByIdP(ORGANISATION_GUID);

        DefaultPage<Role> page = given()
                .when()
                .header(API_TOKEN_HEADER, API_TOKEN_HASH)
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .get("/api/external/v1/roles")
                .as(new TypeRef<>() {
                });
        assertEquals(3, page.getTotalElements());
    }

    @Test
    void rolesByApplicationInstitutionAdmin() throws Exception {
        super.stubForManagerProvidersByIdIn(EntityType.SAML20_SP, List.of("1", "2"));
        super.stubForManagerProvidersByIdIn(EntityType.OIDC10_RP, List.of("5"));

        super.stubForManageProvidersAllowedByIdP(ORGANISATION_GUID);

        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/me", INSTITUTION_ADMIN_SUB,
                institutionalAdminEntitlementOperator(ORGANISATION_GUID));

        DefaultPage<Role> page = given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .queryParam("force", true)
                .contentType(ContentType.JSON)
                .get("/api/v1/roles")
                .as(new TypeRef<>() {
                });
        assertEquals(3, page.getTotalElements());
        List<Role> roles = page.getContent();
        assertEquals(3, roles.size());
        roles.forEach(role -> assertEquals(ORGANISATION_GUID, roleRepository.findById(role.getId()).get().getOrganizationGUID()));
    }

    @Test
    void rolesByApplicationSuperUser() throws Exception {
        //Because the user is changed and provisionings are queried
        stubForManagerProvidersByIdIn(EntityType.OIDC10_RP, List.of("5"));
        stubForManagerProvidersByIdIn(EntityType.SAML20_SP, List.of("1", "2"));

        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", SUPER_SUB);

        DefaultPage<Role> page = given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .queryParam("force", true)
                .contentType(ContentType.JSON)
                .get("/api/v1/roles")
                .as(new TypeRef<>() {
                });
        assertEquals(roleRepository.count(), page.getTotalElements());
        List<Role> roles = page.getContent();
        assertTrue(roleByName("Wiki", roles).isOverrideSettingsAllowed());
        assertTrue(roleByName("Network", roles).isEduIDOnly());
        assertTrue(roleByName("Storage", roles).isEnforceEmailEquality());
        assertEquals(900, roleByName("Research", roles).getDefaultExpiryDays());
    }

    @Test
    void rolesByApplicationSuperUserPagination() throws Exception {
        //Because the user is changed and provisionings are queried
        stubForManagerProvidersByIdIn(EntityType.OIDC10_RP, List.of("5"));
        stubForManagerProvidersByIdIn(EntityType.SAML20_SP, List.of("1", "2"));

        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", SUPER_SUB);

        DefaultPage<Role> page = given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .queryParam("force", false)
                .queryParam("query", "desc")
                .queryParam("pageNumber", 2)
                .queryParam("pageSize", 2)
                .queryParam("sort", "description")
                .queryParam("sortDirection", Sort.Direction.DESC.name())
                .contentType(ContentType.JSON)
                .get("/api/v1/roles")
                .as(new TypeRef<>() {
                });
        assertEquals(roleRepository.count(), page.getTotalElements());
        assertEquals(2, page.getPageable().getPageSize());
        assertEquals(2, page.getPageable().getPageNumber());
        assertEquals(6, page.getTotalElements());
        List<Role> roles = page.getContent();
        assertEquals(List.of("Calendar", "Mail"), roles.stream().map(Role::getName).sorted().toList());
    }

    @Test
    void rolesByApplicationSuperUserPaginationSortUserRoleCount() throws Exception {
        //Because the user is changed and provisionings are queried
        stubForManagerProvidersByIdIn(EntityType.OIDC10_RP, List.of("5"));
        stubForManagerProvidersByIdIn(EntityType.SAML20_SP, List.of("1", "2"));

        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", SUPER_SUB);

        DefaultPage<Role> page = given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .queryParam("force", false)
                .queryParam("pageNumber", 2)
                .queryParam("pageSize", 2)
                .queryParam("sort", "userRoleCount")
                .queryParam("sortDirection", Sort.Direction.DESC.name())
                .contentType(ContentType.JSON)
                .get("/api/v1/roles")
                .as(new TypeRef<>() {
                });
        assertEquals(roleRepository.count(), page.getTotalElements());
        List<Role> roles = page.getContent();
        assertEquals(1L, roles.getFirst().getUserRoleCount());
    }

    @Test
    void rolesByApplicationSuperUserPaginationMultipleApplications() throws Exception {
        //Because the user is changed and provisionings are queried
        stubForManagerProvidersByIdIn(EntityType.OIDC10_RP, List.of("1"));
        stubForManagerProvidersByIdIn(EntityType.SAML20_SP, List.of("1"));

        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", SUPER_SUB);

        DefaultPage<Role> page = given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .queryParam("force", false)
                .queryParam("query", "stora")
                .queryParam("pageNumber", 0)
                .queryParam("pageSize", 10)
                .contentType(ContentType.JSON)
                .get("/api/v1/roles")
                .as(new TypeRef<>() {
                });
        assertEquals(1, page.getTotalElements());
        assertEquals(10, page.getPageable().getPageSize());
        assertEquals(0, page.getPageable().getPageNumber());
        assertEquals(2, page.getContent().getFirst().getApplicationMaps().size());
    }

    @Test
    void deleteRoleWithAPI() throws Exception {
        Role role = roleRepository.search("network", 1).get(0);
        //Ensure delete provisioning is done
        remoteProvisionedGroupRepository.save(new RemoteProvisionedGroup(role, UUID.randomUUID().toString(), "7"));
        Application application = role.applicationsUsed().iterator().next();
        super.stubForManagerProvidersByIdIn(application.getManageType(), List.of(application.getManageId()));
        super.stubForManageProvidersAllowedByIdP(ORGANISATION_GUID);
        super.stubForManageProvisioning(List.of(application.getManageId()));
        super.stubForDeleteScimRole();

        given()
                .when()
                .accept(ContentType.JSON)
                .header(API_TOKEN_HEADER, API_TOKEN_HASH)
                .contentType(ContentType.JSON)
                .pathParams("id", role.getId())
                .delete("/api/external/v1/roles/{id}")
                .then()
                .statusCode(204);
        assertEquals(0, roleRepository.search("network", 1).size());
    }

    @Test
    void roleById() throws Exception {
        //Because the user is changed and provisionings are queried
        stubForManageProvisioning(List.of());
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", MANAGE_SUB);

        Role roleDB = roleRepository.search("wiki", 1).get(0);
        super.stubForManagerProvidersByIdIn(EntityType.SAML20_SP, List.of("1"));
        Role role = given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .contentType(ContentType.JSON)
                .pathParams("id", roleDB.getId())
                .get("/api/v1/roles/{id}")
                .as(new TypeRef<>() {
                });
        assertEquals(roleDB.getName(), role.getName());
        assertEquals("1", role.getApplicationMaps().get(0).get("id"));
    }

    @Test
    void deleteRole() throws Exception {
        //Because the user is changed and provisionings are queried
        stubForManageProvisioning(List.of("4"));
        stubForManageProvidersAllowedByIdP(ORGANISATION_GUID);

        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", INSTITUTION_ADMIN_SUB);

        Role role = roleRepository.search("storage", 1).get(0);
        given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .contentType(ContentType.JSON)
                .pathParams("id", role.getId())
                .delete("/api/v1/roles/{id}")
                .then()
                .statusCode(403);
        assertEquals(1, roleRepository.search("storage", 1).size());
    }

    @Test
    void roleByIdForbidden() throws Exception {
        //Because the user is changed and provisionings are queried
        stubForManageProvisioning(List.of());
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", MANAGE_SUB);
        Role roleDB = roleRepository.search("research", 1).get(0);
        super.stubForManageProviderById(EntityType.SAML20_SP, "4");
        given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .contentType(ContentType.JSON)
                .pathParams("id", roleDB.getId())
                .get("/api/v1/roles/{id}")
                .then()
                .statusCode(403);
    }

    @Test
    void roleNotFound() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", MANAGE_SUB);
        given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .contentType(ContentType.JSON)
                .pathParams("id", 0)
                .get("/api/v1/roles/{id}")
                .then()
                .statusCode(404);

    }

    @Test
    void rolesByApplicationSuperUserWithAPIToken() {
        super.stubForManagerProvidersByIdIn(EntityType.SAML20_SP, List.of("1", "2", "3", "4"));
        super.stubForManagerProvidersByIdIn(EntityType.OIDC10_RP, List.of("5", "6"));

        DefaultPage<Role> page = given()
                .when()
                .accept(ContentType.JSON)
                .header(API_TOKEN_HEADER, API_TOKEN_SUPER_USER_HASH)
                .contentType(ContentType.JSON)
                .get("/api/external/v1/roles")
                .as(new TypeRef<>() {
                });
        assertEquals(6, page.getTotalElements());
    }

    @Test
    void rolesPerApplicationId() throws Exception {
        stubForManageProvidersAllowedByIdP(ORGANISATION_GUID);
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", INSTITUTION_ADMIN_SUB);

        List<Role> roles = given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .contentType(ContentType.JSON)
                .pathParams("manageId", "2")
                .get("/api/v1/roles/application/{manageId}")
                .as(new TypeRef<>() {
                });
        assertEquals(1, roles.size());
    }

    @Test
    void rolesPerApplicationIdNotAllowed() throws Exception {
        stubForManageProvidersAllowedByIdP(ORGANISATION_GUID);
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", INSTITUTION_ADMIN_SUB);

        given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .contentType(ContentType.JSON)
                .pathParams("manageId", "6")
                .get("/api/v1/roles/application/{manageId}")
                .then()
                .statusCode(HttpStatus.FORBIDDEN.value());
    }

    @Test
    void rolesPerApplicationIdSuperUser() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", SUPER_SUB);
        super.stubForManagerProvidersByIdIn(EntityType.SAML20_SP, List.of("3"));
        super.stubForManagerProvidersByIdIn(EntityType.OIDC10_RP, List.of("6"));

        List<Role> roles = given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .contentType(ContentType.JSON)
                .pathParams("manageId", "6")
                .get("/api/v1/roles/application/{manageId}")
                .as(new TypeRef<>() {
                });
        assertEquals(1, roles.size());
        assertEquals(2, roles.get(0).getApplicationUsages().size());
    }

    @Test
    void createWithToken() throws Exception {
        stubForManageProvidersAllowedByIdP(ORGANISATION_GUID);
        stubForManageProvisioning(List.of());

        stubForCreateScimRole();

        Role role = new Role("#333", "New desc", application("1", EntityType.SAML20_SP), 365, false, false);

        Map result = given()
                .when()
                .accept(ContentType.JSON)
                .header(API_TOKEN_HEADER, API_TOKEN_HASH)
                .contentType(ContentType.JSON)
                .body(role)
                .post("/api/external/v1/roles")
                .as(Map.class);
        assertNotNull(result.get("id"));
        assertEquals(String.format("urn:mace:surf.nl:test.surfaccess.nl:%s:333", result.get("identifier")), result.get("urn"));
    }

    @Test
    void createWithSuperUserToken() throws Exception {
        stubForCreateScimRole();

        RoleRequest role = new RoleRequest(
                "New Role Super User",
                "description",
                null,
                null,
                false,
                false,
                false,
                false,
                null,
                null,
                application("1", EntityType.SAML20_SP)
        );

        Map result = given()
                .when()
                .accept(ContentType.JSON)
                .header(API_TOKEN_HEADER, API_TOKEN_SUPER_USER_HASH)
                .contentType(ContentType.JSON)
                .body(role)
                .post("/api/external/v1/roles")
                .as(Map.class);
        assertNotNull(result.get("id"));
        assertEquals(365, result.get("defaultExpiryDays"));
    }

    @Test
    void createWithLegacyToken() throws Exception {
        stubForManageProvidersAllowedByIdP(ORGANISATION_GUID);
        stubForManageProvisioning(List.of());

        stubForCreateScimRole();

        Role role = new Role("#570/A", "New desc", application("1", EntityType.SAML20_SP), 365, false, false);

        Map result = given()
                .when()
                .accept(ContentType.JSON)
                .header(API_TOKEN_HEADER, API_TOKEN_LEGACY_HASH)
                .contentType(ContentType.JSON)
                .body(role)
                .post("/api/external/v1/roles")
                .as(Map.class);
        assertNotNull(result.get("id"));
        assertEquals(String.format("urn:mace:surf.nl:test.surfaccess.nl:%s:570/a", result.get("identifier")), result.get("urn"));
    }

    private Role roleByName(String name, List<Role> roles) {
        return roles.stream().filter(role -> role.getName().equalsIgnoreCase(name)).findFirst().orElseThrow(IllegalArgumentException::new);
    }

}