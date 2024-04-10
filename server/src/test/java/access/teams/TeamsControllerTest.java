package access.teams;

import access.AbstractTest;
import access.manage.EntityType;
import access.model.Application;
import access.model.Authority;
import access.model.User;
import access.model.UserRole;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static access.teams.TeamsController.mapAuthority;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

class TeamsControllerTest extends AbstractTest {

    private static final Map<String, Role> userRoles = Map.of(
            "Mary Doe", Role.ADMIN,
            "Harry Doe", Role.MEMBER,
            "John Doe", Role.MANAGER,
            "Paul Doe", Role.OWNER
    );

    @Test
    void migrateTeam() throws JsonProcessingException {
        List<Membership> memberships = getMemberships();
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
        assertEquals(4L, role.getUserRoleCount());
        assertTrue(role.isTeamsOrigin());
        assertEquals(2, role.applicationsUsed().size());

        List<User> users = memberships.stream()
                .map(membership -> userRepository.findBySubIgnoreCase(membership.getPerson().getUrn()).orElseThrow(RuntimeException::new))
                .toList();
        assertEquals(4, users.size());

        users.forEach(user -> assertEquals(team.getName(), user.getUserRoles().iterator().next().getRole().getName()));
        userRoles.forEach((key, teamsRole) -> {
            User user = users.stream().filter(u -> u.getName().equals(key)).findFirst().get();
            UserRole userRole = user.getUserRoles().stream().findFirst().get();
            Authority authority = userRole.getAuthority();
            assertEquals(mapAuthority(teamsRole), authority);
            if (teamsRole.equals(Role.MEMBER) || teamsRole.equals(Role.OWNER)) {
                assertFalse(userRole.isGuestRoleIncluded());
            } else {
                assertTrue(userRole.isGuestRoleIncluded());
            }
        });

        //Now check if we get the correct URN from the Voot interface
        Membership harryDoe = memberships.stream().filter(m -> m.getPerson().getName().equals("Harry Doe")).findFirst().get();
        List<Map<String, String>> groups = given()
                .when()
                .auth().basic("voot", "secret")
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .pathParam("sub", harryDoe.getPerson().getUrn())
                .get("/api/voot/{sub}")
                .as(new TypeRef<>() {
                });
        assertEquals(1, groups.size());
        Map<String, String> group = groups.get(0);
        assertEquals("nl:surfnet:diensten:test", group.get("urn"));
        assertEquals(team.getName(), group.get("name"));
    }

    private List<Membership> getMemberships() {
        return userRoles.entrySet().stream()
                .map(entry -> new Membership(
                        new Person(
                                "urn:collab:person:surfnet.nl:" + entry.getKey().toLowerCase().replaceAll(" ", "."),
                                entry.getKey(),
                                entry.getKey().replaceAll(" ", "") + "@example.com",
                                "example.com"), entry.getValue()))
                .collect(Collectors.toList());
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