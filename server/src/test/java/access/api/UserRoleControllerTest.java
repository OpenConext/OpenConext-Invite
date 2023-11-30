package access.api;

import access.AbstractTest;
import access.AccessCookieFilter;
import access.exception.NotFoundException;
import access.manage.EntityType;
import access.model.*;
import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static access.Seed.*;
import static access.security.SecurityConfig.API_TOKEN_HEADER;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class UserRoleControllerTest extends AbstractTest {

    @Test
    void byRole() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", MANAGE_SUB);
        Role role = roleRepository.findByShortNameIgnoreCaseAndApplicationsManageId("1", "wiki").get();
        List<UserRole> userRoles = given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .pathParams("roleId", role.getId())
                .get("/api/v1/user_roles/roles/{roleId}")
                .as(new TypeRef<>() {
                });
        assertEquals(2, userRoles.size());
        assertNotNull(userRoles.get(0).getUserInfo().get("name"));
    }

    @Test
    void updateEndDate() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", MANAGE_SUB);
        UserRole userRole = userRoleRepository.findByRoleName("Wiki").stream()
                .filter(userRole1 -> userRole1.getAuthority().equals(Authority.GUEST))
                .findFirst()
                .get();
        Instant endDate = userRole.getEndDate();

        given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body(new UpdateUserRole(userRole.getId(), endDate.plus(365, ChronoUnit.DAYS)))
                .put("/api/v1/user_roles")
                .then()
                .statusCode(201);
        UserRole updatedRole = userRoleRepository.findByRoleName("Wiki").stream()
                .filter(userRole1 -> userRole1.getAuthority().equals(Authority.GUEST))
                .findFirst()
                .get();
        Instant newEndDate = updatedRole.getEndDate();
        assertEquals(365L, endDate.until(newEndDate, ChronoUnit.DAYS));
    }

    @Test
    void updateEndDateInThePast() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", MANAGE_SUB);
        UserRole userRole = userRoleRepository.findByRoleName("Wiki").stream()
                .filter(userRole1 -> userRole1.getAuthority().equals(Authority.GUEST))
                .findFirst()
                .get();
        given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body(new UpdateUserRole(userRole.getId(), Instant.now().minus(365, ChronoUnit.DAYS)))
                .put("/api/v1/user_roles")
                .then()
                .statusCode(409);
    }

    @Test
    void deleteUserRole() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", MANAGE_SUB);
        List<UserRole> userRoles = userRoleRepository.findByRoleName("Wiki");
        UserRole guestUserRole = userRoles.stream().filter(userRole -> userRole.getAuthority().equals(Authority.GUEST)).findFirst().get();
        given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .pathParams("userRoleId", guestUserRole.getId())
                .delete("/api/v1/user_roles/{userRoleId}")
                .then()
                .statusCode(204);

        List<UserRole> newUserRoles = userRoleRepository.findByRoleName("Wiki");
        assertEquals(userRoles.size(), newUserRoles.size() + 1);
    }

    @Test
    void deleteUserRoleNotAllowed() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", INVITER_SUB);
        List<UserRole> userRoles = userRoleRepository.findByRoleName("Calendar");
        UserRole guestUserRole = userRoles.stream().filter(userRole -> userRole.getAuthority().equals(Authority.INVITER)).findFirst().get();
        given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .pathParams("userRoleId", guestUserRole.getId())
                .delete("/api/v1/user_roles/{userRoleId}")
                .then()
                .statusCode(403);
    }

    @Test
    void userRoleProvisioning() throws Exception {
        super.stubForManagerProvidersByIdIn(EntityType.SAML20_SP, List.of("1", "2"));
        super.stubForManageProviderByOrganisationGUID(ORGANISATION_GUID);
        super.stubForManageProvisioning(List.of("1", "2"));

        super.stubForCreateScimUser();
        super.stubForCreateGraphUser();
        super.stubForCreateScimRole();
        super.stubForUpdateScimRole();

        List<Long> roleIdentifiers = List.of(
                roleRepository.findByName("Network").get(0).getId(),
                roleRepository.findByName("Wiki").get(0).getId()
        );
        UserRoleProvisioning userRoleProvisioning = new UserRoleProvisioning(
                roleIdentifiers,
                Authority.GUEST,
                null,
                "new_user@domain.org",
                null,
                null,
                null,
                "Charly Green",
                null
        );
        given()
                .when()
                .header(API_TOKEN_HEADER, API_TOKEN_HASH)
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body(userRoleProvisioning)
                .post("/api/external/v1/user_roles/user_role_provisioning")
                .then()
                .statusCode(201);

        User user = userRepository.findBySubIgnoreCase("urn:collab:person:domain.org:new_user").orElseThrow(NotFoundException::new);
        assertEquals(2, user.getUserRoles().size());
    }

}