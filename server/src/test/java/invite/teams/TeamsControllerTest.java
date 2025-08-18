package invite.teams;

import invite.AbstractTest;
import invite.exception.InvalidInputException;
import invite.manage.EntityType;
import invite.model.Application;
import invite.model.Authority;
import invite.model.User;
import invite.model.UserRole;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static invite.teams.TeamsController.mapAuthority;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
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
                .auth().preemptive().basic("teams", "secret")
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body(team)
                .put("/api/external/v1/teams")
                .then()
                .statusCode(201);

        invite.model.Role role = roleRepository.findByName(team.getName()).get();
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
            assertTrue(Instant.now().minus(28, ChronoUnit.DAYS).isAfter(user.getCreatedAt()));
            UserRole userRole = user.getUserRoles().stream().findFirst().get();
            assertTrue(Instant.now().minus(13, ChronoUnit.DAYS).isAfter(userRole.getCreatedAt()));
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
                .auth().preemptive().basic("voot", "secret")
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .pathParam("sub", harryDoe.getPerson().getUrn())
                .get("/api/external/v1/voot/{sub}")
                .as(new TypeRef<>() {
                });
        assertEquals(1, groups.size());
        Map<String, String> group = groups.get(0);
        assertEquals("nl:surfnet:diensten:test", group.get("urn"));
        assertEquals(team.getName(), group.get("name"));
    }

    @Test
    void migrateTeamObjectOptimisticLockingFailureException() throws JsonProcessingException {
        List<Membership> memberships = getMemberships();
        String manageId = UUID.randomUUID().toString();
        Application application = new Application(manageId, EntityType.OIDC10_RP);
        application.setId(Long.MAX_VALUE - 1);

        List<Application> applications = List.of(application);
        Team team = new Team(
                "nl:surfnet:diensten:test",
                "test migration",
                "test migration",
                memberships,
                applications
        );
        String path = String.format("/manage/api/internal/metadata/%s/%s",
                application.getManageType().name().toLowerCase(), manageId);
        String body = objectMapper.writeValueAsString(localManage.providerById(application.getManageType(), "7"));
        stubFor(get(urlPathMatching(path)).willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(body)));

        super.stubForManageProvisioning(List.of());

        given()
                .when()
                .auth().preemptive().basic("teams", "secret")
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body(team)
                .put("/api/external/v1/teams")
                .then()
                .statusCode(201);

    }

    private List<Membership> getMemberships() {
        return userRoles.entrySet().stream()
                .map(entry -> new Membership(
                        new Person(
                                "urn:collab:person:surfnet.nl:" + entry.getKey().toLowerCase().replaceAll(" ", "."),
                                entry.getKey(),
                                entry.getKey().replaceAll(" ", "") + "@example.com",
                                "example.com",
                                Instant.now().minus(30, ChronoUnit.DAYS)),
                        entry.getValue(),
                        Instant.now().minus(15, ChronoUnit.DAYS)
                ))
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
                .auth().preemptive().basic("teams", "secret")
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body(team)
                .put("/api/external/v1/teams")
                .then()
                .statusCode(400);
    }

    @Test
    void migrateTeamNonExistentApplications() {
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
                .auth().preemptive().basic("teams", "secret")
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body(team)
                .put("/api/external/v1/teams")
                .then()
                .statusCode(400);
    }

    @Test
    void migrateTeamEmptyApplications() {
        List<Membership> memberships = List.of(new Membership());
        Team team = new Team(
                "nl:surfnet:diensten:test",
                "test migration",
                "test migration",
                memberships,
                List.of()
        );

        Map<String, Object> responseBody = given()
                .when()
                .auth().preemptive().basic("teams", "secret")
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body(team)
                .put("/api/external/v1/teams")
                .as(new TypeRef<>() {
                });
        assertEquals("Applications are required", responseBody.get("message"));
    }

    @Test
    void migrateTeamInvalidPerson() {
        List<Application> applications = List.of(new Application("999", EntityType.SAML20_SP));
        List<Membership> memberships = List.of(new Membership());
        Team team = new Team(
                "nl:surfnet:diensten:test",
                "test migration",
                "test migration",
                memberships,
                applications
        );

        Map<String, Object> responseBody = given()
                .when()
                .auth().preemptive().basic("teams", "secret")
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body(team)
                .put("/api/external/v1/teams")
                .as(new TypeRef<>() {
                });
        assertEquals("Person of a membership is required", responseBody.get("message"));
    }

    @Test
    void migrateTeamInvalidSchacHome() {
        List<Application> applications = List.of(new Application("999", EntityType.SAML20_SP));
        Person person = new Person();
        Membership membership = new Membership(person, Role.MANAGER, Instant.now());
        List<Membership> memberships = List.of(membership);
        Team team = new Team(
                "nl:surfnet:diensten:test",
                "test migration",
                "test migration",
                memberships,
                applications
        );

        Map<String, Object> responseBody = given()
                .when()
                .auth().preemptive().basic("teams", "secret")
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body(team)
                .put("/api/external/v1/teams")
                .as(new TypeRef<>() {
                });
        assertEquals("SchacHomeOrganization of a person is required", responseBody.get("message"));
    }

    @Test
    void migrateTeamManageUnavailable() {
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

        given()
                .when()
                .auth().preemptive().basic("teams", "secret")
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body(team)
                .put("/api/external/v1/teams")
                .then()
                .statusCode(400);
    }

    @Test
    void mapAuthorityNull() {
        assertThrows(InvalidInputException.class, () -> mapAuthority(null));
    }
}