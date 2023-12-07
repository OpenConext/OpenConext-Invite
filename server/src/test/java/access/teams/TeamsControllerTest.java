package access.teams;

import access.AbstractTest;
import access.manage.EntityType;
import access.model.Application;
import access.model.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static access.AbstractTest.GUEST_SUB;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamsControllerTest extends AbstractTest {

    @Test
    void migrateTeam() throws JsonProcessingException {
        List<Membership> memberships = List.of(
                new Membership(
                        new Person(
                                "urn:collab:person:surfnet.nl:mdoe",
                                "Mary Doe",
                                "mdoe@example.com",
                                "example.com"), Role.ADMIN),
                new Membership(
                        new Person(
                                "urn:collab:person:surfnet.nl:hdoe",
                                "Harry Doe",
                                "hdoe@example.com",
                                "example.com"), Role.MEMBER)

        );
        List<Application> applications = List.of(
                new Application("1", EntityType.SAML20_SP),
                new Application("5", EntityType.OIDC10_RP));
        Team team = new Team(
                "nl:surfnet:diensten:test",
                "test migration",
                "test migration",
                memberships,
                applications
        );
        applications.forEach(application -> super.stubForManageProviderById(application.getManageType(), application.getManageId()));
        super.stubForManageProvisioning(applications.stream().map(Application::getManageId).toList());
        super.stubForCreateScimRole();
        super.stubForCreateScimUser();
        super.stubForUpdateScimRole();

        given()
                .when()
                .auth().basic("teams", "secret")
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body(team)
                .put("/api/teams")
                .then()
                .statusCode(201);

        access.model.Role role = roleRepository.findByName(team.getName()).get(0);
        assertEquals(2L, role.getUserRoleCount());
        assertTrue(role.isTeamsOrigin());
        assertEquals(2, role.getApplications().size());

        List<User> users = memberships.stream()
                .map(membership -> userRepository.findBySubIgnoreCase(membership.getPerson().getUrn()).orElseThrow(RuntimeException::new))
                .toList();
        assertEquals(2, users.size());
        users.forEach(user -> assertEquals(team.getName(), user.getUserRoles().iterator().next().getRole().getName()));

        //Now check if we get the correct URN from the Voot interface
        List<Map<String, String>> groups = given()
                .when()
                .auth().basic("voot", "secret")
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .pathParam("sub", "urn:collab:person:surfnet.nl:hdoe")
                .get("/api/voot/{sub}")
                .as(new TypeRef<>() {
                });
        assertEquals(1, groups.size());
        Map<String, String> group = groups.get(0);
        assertEquals("urn:collab:group:test.surfteams.nl::nl:surfnet:diensten:test", group.get("urn"));
        assertEquals(team.getName(), group.get("name"));
    }

    @Test
    void migrateTeamInvalidApplications() {
        List<Membership> memberships = List.of();
        List<Application> applications = List.of(new Application("999", EntityType.SAML20_SP));
        Team team = new Team(
                "nl:surfnet:diensten:test",
                "test migration",
                "test migration",
                memberships,
                applications
        );

        given()
                .when()
                .auth().basic("teams", "secret")
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body(team)
                .put("/api/teams")
                .then()
                .statusCode(400);
    }
}