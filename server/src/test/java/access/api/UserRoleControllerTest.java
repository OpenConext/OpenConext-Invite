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
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static access.Seed.MANAGE_SUB;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

class UserRoleControllerTest extends AbstractTest {

    @Test
    void byRole() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", MANAGE_SUB);
        Role role = roleRepository.findByManageIdAndShortNameIgnoreCase("1", "wiki").get();
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
}