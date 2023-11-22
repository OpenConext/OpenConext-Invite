package access.api;

import access.AbstractTest;
import access.AccessCookieFilter;
import access.Seed;
import access.manage.EntityType;
import access.model.Application;
import access.model.RemoteProvisionedGroup;
import access.model.Role;
import access.model.RoleExists;
import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static access.Seed.*;
import static access.security.SecurityConfig.API_TOKEN_HEADER;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

class RoleControllerTest extends AbstractTest {

    @Test
    void create() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", MANAGE_SUB);
        Role role = new Role("New", "New desc", "https://landingpage.com", Seed.application("1", EntityType.SAML20_SP), 365, false, false);

        stubForManageProviderById(EntityType.SAML20_SP, "1");
        stubForManageProvisioning(List.of("1"));
        stubForCreateScimRole();

        Map result = given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .contentType(ContentType.JSON)
                .body(role)
                .post("/api/v1/roles")
                .as(Map.class);
        assertNotNull(result.get("id"));
    }

    @Test
    void createInvalidLandingPage() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", MANAGE_SUB);
        Role role = new Role("New", "New desc", "javascript:alert(42)", Seed.application("1", EntityType.SAML20_SP), 365, false, false);

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
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", MANAGE_SUB);
        Role role = new Role("New", "New desc", "https://landingpage.com", Seed.application("1", EntityType.SAML20_SP), 365, false, false);

        stubForManageProviderById(EntityType.SAML20_SP, "1");
        stubForManageProvisioning(List.of("1"));

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
    void createWithDuplicateShortName() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", MANAGE_SUB);
        Role role = new Role("Wiki", "New desc", "https://landingpage.com", Seed.application("1", EntityType.SAML20_SP), 365, false, false);

        given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .contentType(ContentType.JSON)
                .body(role)
                .post("/api/v1/roles")
                .then()
                .statusCode(409);
    }

    @Test
    void update() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", MANAGE_SUB);
        String body = objectMapper.writeValueAsString(localManage.providerById(EntityType.SAML20_SP, "1"));
        stubFor(get(urlPathMatching("/manage/api/internal/metadata/saml20_sp/1")).willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(body)));
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
        assertEquals(updated.getDescription(), "changed");
        assertEquals(updated.getShortName(), "wiki");
    }

    @Test
    void nameExistsTransientRole() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", MANAGE_SUB);
        Map result = given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .contentType(ContentType.JSON)
                .body(new RoleExists("WIKI", "1", null))
                .post("/api/v1/roles/validation/short_name")
                .as(Map.class);
        assertTrue((Boolean) result.get("exists"));
    }

    @Test
    void nameNotExistsTransientRole() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", MANAGE_SUB);
        Map result = given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .contentType(ContentType.JSON)
                .body(new RoleExists("unique", "1", null))
                .post("/api/v1/roles/validation/short_name")
                .as(Map.class);
        assertFalse((Boolean) result.get("exists"));
    }

    @Test
    void nameExists() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", MANAGE_SUB);
        Role wiki = roleRepository.findByShortNameIgnoreCaseAndApplicationsManageId("1", "WIKI").get();
        Role research = roleRepository.findByShortNameIgnoreCaseAndApplicationsManageId("4", "research").get();
        Map result = given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .contentType(ContentType.JSON)
                .body(new RoleExists(research.getShortName(), research.getApplications().iterator().next().getManageId(), wiki.getId()))
                .post("/api/v1/roles/validation/short_name")
                .as(Map.class);
        assertTrue((Boolean) result.get("exists"));
    }

    @Test
    void rolesByApplication() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", MANAGE_SUB);
        super.stubForManageProviderByEntityID(EntityType.SAML20_SP, "https://wiki");

        List<Role> roles = given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .contentType(ContentType.JSON)
                .get("/api/v1/roles")
                .as(new TypeRef<>() {
                });
        assertEquals(1, roles.size());
        assertEquals("Wiki", roles.get(0).getName());
    }

    @Test
    void rolesByApplicationInstitutionAdmin() throws Exception {
        super.stubForManageProviderByEntityID(EntityType.SAML20_SP, "https://wiki");
        super.stubForManageProviderByEntityID(EntityType.OIDC10_RP, "https://wiki");
        super.stubForManageProviderByOrganisationGUID(ORGANISATION_GUID);

        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/me", INSTITUTION_ADMIN,
                institutionalAdminEntitlementOperator(ORGANISATION_GUID));
        List<Role> roles = given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .contentType(ContentType.JSON)
                .get("/api/v1/roles")
                .as(new TypeRef<>() {
                });
        assertEquals(4, roles.size());
    }

    @Test
    void rolesByApplicationSuperUser() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", SUPER_SUB);
        super.stubForManageProviderByEntityID(EntityType.SAML20_SP, "https://wiki");
        super.stubForManageProviderByEntityID(EntityType.OIDC10_RP, "https://calendar");

        List<Role> roles = given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .contentType(ContentType.JSON)
                .get("/api/v1/roles")
                .as(new TypeRef<>() {
                });
        assertEquals(6, roles.size());
    }

    @Test
    void roleById() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", MANAGE_SUB);
        Role roleDB = roleRepository.search("wiki", 1).get(0);
        stubForManageProviderById(EntityType.SAML20_SP, "1");
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
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", MANAGE_SUB);
        Role role = roleRepository.search("wiki", 1).get(0);
        //Ensure delete provisioning is done
        remoteProvisionedGroupRepository.save(new RemoteProvisionedGroup(role, UUID.randomUUID().toString(), "7"));
        Application application = role.getApplications().iterator().next();
        stubForManageProviderById(application.getManageType(), application.getManageId());
        stubForManageProvisioning(List.of(application.getManageId()));
        stubForDeleteScimRole();

        given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .contentType(ContentType.JSON)
                .pathParams("id", role.getId())
                .delete("/api/v1/roles/{id}")
                .then()
                .statusCode(204);
        assertEquals(0, roleRepository.search("wiki", 1).size());
    }

    @Test
    void search() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", SUPER_SUB);
        super.stubForManageProviderByEntityID(EntityType.SAML20_SP, "https://wiki");
        super.stubForManageProviderByEntityID(EntityType.OIDC10_RP, "https://calendar");

        List<Role> roles = given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .contentType(ContentType.JSON)
                .queryParam("query", "desc")
                .get("/api/v1/roles/search")
                .as(new TypeRef<>() {
                });
        assertEquals(6, roles.size());
    }

    @Test
    void roleByIdForbidden() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", MANAGE_SUB);
        Role roleDB = roleRepository.search("research", 1).get(0);
        stubForManageProviderById(EntityType.SAML20_SP, "4");
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
    void rolesByApplicationInstitutionAdminByAPI() throws Exception {
        super.stubForManageProviderByEntityID(EntityType.SAML20_SP, "https://wiki");
        super.stubForManageProviderByEntityID(EntityType.OIDC10_RP, "https://wiki");
        super.stubForManageProviderByOrganisationGUID(ORGANISATION_GUID);

        List<Role> roles = given()
                .when()
                .header(API_TOKEN_HEADER, API_TOKEN_HASH)
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .get("/api/external/v1/roles")
                .as(new TypeRef<>() {
                });
        assertEquals(4, roles.size());
    }

    @Test
    void deleteRoleWithAPI() throws Exception {
        Role role = roleRepository.search("wiki", 1).get(0);
        //Ensure delete provisioning is done
        remoteProvisionedGroupRepository.save(new RemoteProvisionedGroup(role, UUID.randomUUID().toString(), "7"));
        Application application = role.getApplications().iterator().next();
        stubForManageProviderById(application.getManageType(), application.getManageId());
        stubForManageProviderByOrganisationGUID(ORGANISATION_GUID);
        stubForManageProvisioning(List.of(application.getManageId()));
        stubForDeleteScimRole();

        given()
                .when()
                .accept(ContentType.JSON)
                .header(API_TOKEN_HEADER, API_TOKEN_HASH)
                .contentType(ContentType.JSON)
                .pathParams("id", role.getId())
                .delete("/api/external/v1/roles/{id}")
                .then()
                .statusCode(204);
        assertEquals(0, roleRepository.search("wiki", 1).size());
    }

}