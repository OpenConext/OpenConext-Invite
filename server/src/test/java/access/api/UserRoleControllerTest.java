package access.api;

import access.AbstractTest;
import access.AccessCookieFilter;
import access.model.Authority;
import access.model.Role;
import access.model.UpdateUserRole;
import access.model.UserRole;
import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static access.Seed.INVITER_SUB;
import static access.Seed.MANAGE_SUB;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

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
}