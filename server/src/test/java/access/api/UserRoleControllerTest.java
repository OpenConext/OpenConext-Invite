package access.api;

import access.AbstractTest;
import access.AccessCookieFilter;
import access.model.Role;
import access.model.UserRole;
import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

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

}