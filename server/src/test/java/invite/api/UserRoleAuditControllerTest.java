package invite.api;

import invite.AbstractTest;
import invite.AccessCookieFilter;
import invite.DefaultPage;
import invite.model.Role;
import invite.model.User;
import invite.model.UserRoleAudit;
import invite.repository.UserRoleAuditRepository;
import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

class UserRoleAuditControllerTest extends AbstractTest {

    @Autowired
    protected UserRoleAuditRepository userRoleAuditRepository;

    @BeforeEach
    protected void beforeEach() throws Exception {
        super.beforeEach();
        this.seedUserRoleAudits();
    }

    @Test
    void searchAsInstitutionAdmin() throws Exception {
        stubForManageProvidersAllowedByIdP(ORGANISATION_GUID);
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", INSTITUTION_ADMIN_SUB);

        DefaultPage<UserRoleAudit> page = given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .contentType(ContentType.JSON)
                .get("/api/v1/user_roles_audit/search")
                .as(new TypeRef<>() {
                });
        assertEquals(3, page.getTotalElements());
    }

    @Test
    void searchAsInstitutionAdminWithQuery() throws Exception {
        stubForManageProvidersAllowedByIdP(ORGANISATION_GUID);
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", INSTITUTION_ADMIN_SUB);

        DefaultPage<UserRoleAudit> page = given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .queryParam("query", "ann")
                .contentType(ContentType.JSON)
                .get("/api/v1/user_roles_audit/search")
                .as(new TypeRef<>() {
                });
        assertEquals(1, page.getTotalElements());
        UserRoleAudit userRoleAudit = page.getContent().getFirst();
        assertEquals("Research", userRoleAudit.getRoleName());
    }

    @Test
    void searchAsInstitutionAdminWithRoleId() throws Exception {
        stubForManageProvidersAllowedByIdP(ORGANISATION_GUID);
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", INSTITUTION_ADMIN_SUB);

        Role research = this.roleRepository.findByName("Research").get();
        DefaultPage<UserRoleAudit> page = given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .queryParam("roleId", research.getId())
                .contentType(ContentType.JSON)
                .get("/api/v1/user_roles_audit/search")
                .as(new TypeRef<>() {
                });
        assertEquals(2, page.getTotalElements());
    }

    @Test
    void searchAsInstitutionAdminWithRoleIdAndQuery() throws Exception {
        stubForManageProvidersAllowedByIdP(ORGANISATION_GUID);
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", INSTITUTION_ADMIN_SUB);

        Role research = this.roleRepository.findByName("Research").get();
        DefaultPage<UserRoleAudit> page = given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .queryParam("roleId", research.getId())
                .queryParam("query", "ann")
                .contentType(ContentType.JSON)
                .get("/api/v1/user_roles_audit/search")
                .as(new TypeRef<>() {
                });
        assertEquals(1, page.getTotalElements());
    }

    @Test
    void searchAsSuperUser() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", SUPER_SUB);

        DefaultPage<UserRoleAudit> page = given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .queryParam("pageNumber", 2)
                .queryParam("pageSize", 1)
                .queryParam("sort", "roleName")
                .queryParam("sortDirection", "DESC")
                .contentType(ContentType.JSON)
                .get("/api/v1/user_roles_audit/search")
                .as(new TypeRef<>() {
                });
        assertEquals(4, page.getTotalElements());
        assertEquals(1, page.getContent().size());
        UserRoleAudit userRoleAudit = page.getContent().getFirst();
        assertEquals("Network", userRoleAudit.getRoleName());
    }

    @Test
    void searchAsInstitutionAdminWithNotAllowedRole() throws Exception {
        stubForManageProvidersAllowedByIdP(ORGANISATION_GUID);
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", INSTITUTION_ADMIN_SUB);

        Role mail = this.roleRepository.findByName("Mail").get();
        given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .queryParam("roleId", mail.getId())
                .contentType(ContentType.JSON)
                .get("/api/v1/user_roles_audit/search")
                .then()
                .statusCode(HttpStatus.FORBIDDEN.value());
    }

    @Test
    void fetchRolesInstitutionAdmin() throws Exception {
        stubForManageProvidersAllowedByIdP(ORGANISATION_GUID);
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", INSTITUTION_ADMIN_SUB);

        List<Map<String, String> > res = given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .contentType(ContentType.JSON)
                .get("/api/v1/user_roles_audit/roles")
                .as(new TypeRef<>() {
                });
        assertEquals(3, res.size());
    }

    @Test
    void fetchRolesSuperUser() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", SUPER_SUB);

        List<Map<String, String> > res = given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .contentType(ContentType.JSON)
                .get("/api/v1/user_roles_audit/roles")
                .as(new TypeRef<>() {
                });
        assertEquals(6, res.size());
    }

    private void seedUserRoleAudits() {
        this.userRoleAuditRepository.deleteAllInBatch();
        Role network = this.roleRepository.findByName("Network").get();
        Role research = this.roleRepository.findByName("Research").get();
        Role mail = this.roleRepository.findByName("Mail").get();

        //paul.doe@example.com
        User inviter = this.userRepository.findBySubIgnoreCase(INVITER_SUB).get();
        //ann.doe@example.com
        User guest = this.userRepository.findBySubIgnoreCase(GUEST_SUB).get();

        UserRoleAudit auditNetworkInviter = new UserRoleAudit(network, inviter);
        UserRoleAudit auditResearchInviter = new UserRoleAudit(research, inviter);
        UserRoleAudit auditResearchGuest = new UserRoleAudit(research, guest);
        UserRoleAudit auditMailGuest = new UserRoleAudit(mail, guest);
        doSave(userRoleAuditRepository, auditNetworkInviter, auditResearchInviter, auditResearchGuest, auditMailGuest);
    }


}