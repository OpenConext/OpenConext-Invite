package access.internal;

import access.AbstractTest;
import access.manage.EntityType;
import access.model.Role;
import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;


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
                .post("/api/internal/invite/role")
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
                .put("/api/internal/invite/role")
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
                .get("/api/internal/invite/role/{id}")
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
                .delete("/api/internal/invite/role/{id}")
                .then()
                .statusCode(204);
        Optional<Role> roleOptional = roleRepository.findByName("Research");
        assertTrue(roleOptional.isEmpty());
    }
}