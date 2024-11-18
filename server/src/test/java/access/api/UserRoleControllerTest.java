package access.api;

import access.AbstractTest;
import access.AccessCookieFilter;
import access.exception.NotFoundException;
import access.manage.EntityType;
import access.model.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static access.security.SecurityConfig.API_TOKEN_HEADER;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

class UserRoleControllerTest extends AbstractTest {

    @Test
    void byRole() throws Exception {
        //Because the user is changed and provisionings are queried
        stubForManageProvisioning(List.of());
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", MANAGE_SUB);

        Role role = roleRepository.search("wiki", 1).get(0);
        List<UserRole> userRoles = given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .pathParams("roleId", role.getId())
                .get("/api/v1/user_roles/roles/{roleId}")
                .as(new TypeRef<>() {
                });
        assertEquals(3, userRoles.size());
        assertNotNull(userRoles.get(0).getUserInfo().get("name"));
    }

    @Test
    void searchGuestsByPage() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", INVITER_WIKI_SUB);

        Role role = roleRepository.findByName("Wiki").get();
        Map<String, Object> userRoles = given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .queryParams(Map.of(
                        "pageNumber", 0,
                        "pageSize", 1,
                        "sort", "end_date",
                        "query","example.com",
                        "sortDirection", Sort.Direction.DESC
                ))
                .pathParams("roleId", role.getId())
                .pathParams("guests", true)
                .get("/api/v1/user_roles/search/{roleId}/{guests}")
                .as(new TypeRef<>() {
                });
        assertEquals(2, userRoles.get("totalElements"));
        assertEquals(1, ((List<?>) userRoles.get("content")).size());
    }

    @Test
    void searchNonGuestsByPage() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", INVITER_WIKI_SUB);

        Role role = roleRepository.findByName("Wiki").get();
        Map<String, Object> userRoles = given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .queryParams(Map.of(
                        "pageNumber", 0,
                        "pageSize", 1,
                        "query","example",
                        "sort", "end_date",
                        "sortDirection", Sort.Direction.DESC
                ))
                .pathParams("roleId", role.getId())
                .pathParams("guests", false)
                .get("/api/v1/user_roles/search/{roleId}/{guests}")
                .as(new TypeRef<>() {
                });
        assertEquals(2, userRoles.get("totalElements"));
        assertEquals(1, ((List<?>) userRoles.get("content")).size());
    }

    @Test
    void searchGuestsByPageWithKeyword() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", INVITER_WIKI_SUB);

        Role role = roleRepository.findByName("Wiki").get();
        Map<String, Object> userRoles = given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .queryParams(Map.of(
                        "query", "mary",
                        "pageNumber", 0,
                        "pageSize", 1,
                        "sort", "end_date",
                        "sortDirection", Sort.Direction.DESC
                ))
                .pathParams("roleId", role.getId())
                .pathParams("guests", true)
                .get("/api/v1/user_roles/search/{roleId}/{guests}")
                .as(new TypeRef<>() {
                });
        assertEquals(1, userRoles.get("totalElements"));
        assertEquals(1, ((List<?>) userRoles.get("content")).size());
    }

    @Test
    void searchNonGuestsByPageWithKeyword() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", INVITER_WIKI_SUB);

        Role role = roleRepository.findByName("Wiki").get();
        Map<String, Object> userRoles = given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .queryParams(Map.of(
                        "query", "doe",
                        "pageNumber", 0,
                        "pageSize", 1,
                        "sort", "end_date",
                        "sortDirection", Sort.Direction.DESC
                ))
                .pathParams("roleId", role.getId())
                .pathParams("guests", false)
                .get("/api/v1/user_roles/search/{roleId}/{guests}")
                .as(new TypeRef<>() {
                });
        assertEquals(2, userRoles.get("totalElements"));
        assertEquals(1, ((List<?>) userRoles.get("content")).size());
    }

    @Test
    void byRoleNotFound() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", MANAGE_SUB);
        given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .pathParams("roleId", Integer.MAX_VALUE)
                .get("/api/v1/user_roles/roles/{roleId}")
                .then()
                .statusCode(404);
    }

    @Test
    void updateEndDate() throws Exception {
        //Because the user is changed and provisionings are queried
        stubForManageProvisioning(List.of());
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
    void updateEndDateNotFound() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", MANAGE_SUB);
        given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body(new UpdateUserRole(-1L, Instant.now()))
                .put("/api/v1/user_roles")
                .then()
                .statusCode(404);
    }

    @Test
    void updateEndDateInThePast() throws Exception {
        //Because the user is changed and provisionings are queried
        stubForManageProvisioning(List.of());
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
        //Because the user is changed and provisionings are queried
        stubForManageProvisioning(List.of("4"));
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", MANAGE_SUB);

        List<UserRole> userRoles = userRoleRepository.findByRoleName("Wiki");
        UserRole guestUserRole = userRoles.stream().filter(userRole -> userRole.getAuthority().equals(Authority.GUEST)).findFirst().get();

        String remoteScimIdentifier = UUID.randomUUID().toString();
        remoteProvisionedGroupRepository.save(new RemoteProvisionedGroup(guestUserRole.getRole(), remoteScimIdentifier, "8"));
        remoteProvisionedUserRepository.save(new RemoteProvisionedUser(guestUserRole.getUser(), remoteScimIdentifier, "8"));
        stubForUpdateScimRolePatch();

        given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .pathParams("userRoleId", guestUserRole.getId())
                .pathParams("isGuest", true)
                .delete("/api/v1/user_roles/{userRoleId}/{isGuest}")
                .then()
                .statusCode(204);

        Optional<UserRole> optionalUserRole = userRoleRepository.findById(guestUserRole.getId());
        assertFalse(optionalUserRole.isPresent());
    }

    @Test
    void deleteUserRoleNotFound() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", MANAGE_SUB);
        given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .pathParams("userRoleId", Integer.MAX_VALUE)
                .pathParams("isGuest", false)
                .delete("/api/v1/user_roles/{userRoleId}/{isGuest}")
                .then()
                .statusCode(404);
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
                .pathParams("isGuest", false)
                .delete("/api/v1/user_roles/{userRoleId}/{isGuest}")
                .then()
                .statusCode(403);
    }

    @Test
    void userRoleProvisioningEmail() throws Exception {
        List<Long> roleIdentifiers = List.of(
                roleRepository.findByName("Network").get().getId(),
                roleRepository.findByName("Wiki").get().getId()
        );
        doUserRoleProvisioning(new UserRoleProvisioning(
                roleIdentifiers,
                Authority.GUEST,
                null,
                "new_user@domain.org",
                null,
                null,
                null,
                "Charly Green",
                null,
                true
        ), "urn:collab:person:domain.org:new_user", 2);
    }

    @Test
    void userRoleProvisioningSub() throws Exception {
        List<Long> roleIdentifiers = List.of(
                roleRepository.findByName("Network").get().getId(),
                roleRepository.findByName("Wiki").get().getId()
        );
        doUserRoleProvisioning(new UserRoleProvisioning(
                roleIdentifiers,
                Authority.GUEST,
                "urn:collab:person:example.com:brandcoper",
                "some@domain.org",
                null,
                null,
                null,
                "Charly Green",
                null,
                true
        ), "urn:collab:person:example.com:brandcoper", 2);
    }

    @Test
    void userRoleProvisioningEPPN() throws Exception {
        List<Long> roleIdentifiers = List.of(
                roleRepository.findByName("Network").get().getId(),
                roleRepository.findByName("Wiki").get().getId()
        );
        doUserRoleProvisioning(new UserRoleProvisioning(
                roleIdentifiers,
                Authority.GUEST,
                "",
                "some@domain.org",
                "eppn@domain.org",
                null,
                null,
                "Charly Green",
                null,
                true
        ), "urn:collab:person:domain.org:eppn", 2);
    }

    @Test
    void userRoleProvisioningExistingUser() throws Exception {
        List<Long> roleIdentifiers = List.of(
                roleRepository.findByName("Network").get().getId(),
                roleRepository.findByName("Wiki").get().getId()
        );
        doUserRoleProvisioning(new UserRoleProvisioning(
                roleIdentifiers,
                Authority.GUEST,
                "urn:collab:person:example.com:inviter",
                "some@domain.org",
                "eppn@domain.org",
                null,
                null,
                "Charly Green",
                null,
                true
        ), "urn:collab:person:example.com:inviter", 4);
    }

    @Test
    void consequencesForDeletion() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", SUPER_SUB);

        Role role = roleRepository.search("wiki", 1).get(0);
        List<Map<String, Object>> userRoles = given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .pathParams("roleId", role.getId())
                .get("/api/v1/user_roles/consequences/{roleId}")
                .as(new TypeRef<>() {
                });
        assertEquals(3, userRoles.size());
    }

    @Test
    void consequencesForDeletionNotFound() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", SUPER_SUB);
        given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .pathParams("roleId", Integer.MAX_VALUE)
                .get("/api/v1/user_roles/consequences/{roleId}")
                .then()
                .statusCode(404);
    }

    @Test
    void invalidAPIToken() {
        List<Long> roleIdentifiers = List.of(
                roleRepository.findByName("Network").get().getId(),
                roleRepository.findByName("Wiki").get().getId()
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
                null,
                true
        );
        given()
                .when()
                .header(API_TOKEN_HEADER, "bogus")
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body(userRoleProvisioning)
                .post("/api/external/v1/user_roles/user_role_provisioning")
                .then()
                .statusCode(HttpStatus.FORBIDDEN.value());

    }

    private void doUserRoleProvisioning(UserRoleProvisioning userRoleProvisioning, String expectedSub, int expectedUserRoleCount) throws JsonProcessingException {
        super.stubForManagerProvidersByIdIn(EntityType.SAML20_SP, List.of("1", "2"));
        super.stubForManageProvidersAllowedByIdP(ORGANISATION_GUID);
        super.stubForManageProvisioning(List.of("1", "2"));

        super.stubForCreateScimUser();
        super.stubForCreateGraphUser();
        super.stubForCreateScimRole();
        super.stubForUpdateScimRole();

        User savedUser = given()
                .when()
                .header(API_TOKEN_HEADER, API_TOKEN_HASH)
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body(userRoleProvisioning)
                .post("/api/external/v1/user_roles/user_role_provisioning")
                .as(new TypeRef<>() {
                });
        assertEquals(expectedSub, savedUser.getSub());
        User user = userRepository.findBySubIgnoreCase(expectedSub).orElseThrow(() -> new NotFoundException("User not found"));
        assertEquals(expectedUserRoleCount, user.getUserRoles().size());
    }

    @Test
    void deleteUserRoleWithGuestRoleIncluded() throws Exception {
        //Inviter in the same wiki role as the Manager with guestRoleIncluded. As Inviter is allowed the guest part
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", INVITER_WIKI_SUB);

        List<UserRole> userRoles = userRoleRepository.findByRoleName("Wiki");
        // Manager role which is also guest included
        UserRole managerUserRole = userRoles.stream()
                .filter(userRole -> userRole.isGuestRoleIncluded() && userRole.getAuthority().equals(Authority.MANAGER))
                .findFirst().get();

        given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .pathParams("userRoleId", managerUserRole.getId())
                .pathParams("isGuest", true)
                .delete("/api/v1/user_roles/{userRoleId}/{isGuest}")
                .then()
                .statusCode(204);

        UserRole updatedUserRole = userRoleRepository.findById(managerUserRole.getId()).get();
        assertFalse(updatedUserRole.isGuestRoleIncluded());
        assertEquals(Authority.MANAGER, updatedUserRole.getAuthority());
    }

    @Test
    void deleteUserRoleWithGuestRoleIncludedNotAllowed() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", INVITER_SUB);

        List<UserRole> userRoles = userRoleRepository.findByRoleName("Wiki");
        // Manager role which is also guest included
        UserRole managerUserRole = userRoles.stream()
                .filter(userRole -> userRole.isGuestRoleIncluded() && userRole.getAuthority().equals(Authority.MANAGER))
                .findFirst().get();

        given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .pathParams("userRoleId", managerUserRole.getId())
                .pathParams("isGuest", false)
                .delete("/api/v1/user_roles/{userRoleId}/{isGuest}")
                .then()
                .statusCode(403);
    }

    @Test
    void demoteUserRoleWithGuestRoleIncluded() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", SUPER_SUB);

        List<UserRole> userRoles = userRoleRepository.findByRoleName("Wiki");
        // Manager role which is also guest included
        UserRole managerUserRole = userRoles.stream()
                .filter(userRole -> userRole.isGuestRoleIncluded() && userRole.getAuthority().equals(Authority.MANAGER))
                .findFirst().get();

        given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .pathParams("userRoleId", managerUserRole.getId())
                .pathParams("isGuest", false)
                .delete("/api/v1/user_roles/{userRoleId}/{isGuest}")
                .then()
                .statusCode(204);

        UserRole updatedUserRole = userRoleRepository.findById(managerUserRole.getId()).get();
        assertFalse(updatedUserRole.isGuestRoleIncluded());
        assertEquals(Authority.GUEST, updatedUserRole.getAuthority());
    }

}