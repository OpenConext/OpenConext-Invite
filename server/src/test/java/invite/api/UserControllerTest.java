package invite.api;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import invite.AbstractTest;
import invite.AccessCookieFilter;
import invite.DefaultPage;
import invite.exception.NotFoundException;
import invite.manage.EntityType;
import invite.model.Authority;
import invite.model.RemoteProvisionedUser;
import invite.model.User;
import invite.model.UserRole;
import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Stream;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("unchecked")
class UserControllerTest extends AbstractTest {

    @Test
    void config() {
        Map res = given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .get("/api/v1/users/config")
                .as(Map.class);
        assertFalse((Boolean) res.get("authenticated"));
    }

    @Test
    void configNewUserWithOauth2Login() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", "nope");

        Map res = given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .get("/api/v1/users/config")
                .as(Map.class);
        assertFalse((Boolean) res.get("authenticated"));
        assertEquals("John Doe", res.get("name"));
    }

    @Test
    void configMissingAttributes() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", "");

        Map res = given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .get("/api/v1/users/config")
                .as(Map.class);
        assertFalse((Boolean) res.get("authenticated"));
        assertEquals(1, ((List) res.get("missingAttributes")).size());
    }

    @Test
    void configMissingAttributesGuest() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", "", map -> {
            map.put("schac_home_organization", "");
            map.put("name", "");
            map.put("nickname", "");
            map.put("display_name", "");
            map.put("preferred_username", "");
            map.put("family_name", "");
            map.put("given_name", "");
            return map;
        });

        Map res = given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .queryParam("guest", "true")
                .get("/api/v1/users/config")
                .as(Map.class);
        assertFalse((Boolean) res.get("authenticated"));
        assertEquals(4, ((List) res.get("missingAttributes")).size());
    }

    @Test
    void meWithOauth2Login() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/me", "urn:collab:person:example.com:admin");

        User user = given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .get(accessCookieFilter.apiURL())
                .as(User.class);
        assertEquals("urn:collab:person:example.com:admin", user.getEmail());

        Map res = given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .get("/api/v1/users/config")
                .as(Map.class);
        assertTrue((Boolean) res.get("authenticated"));
    }

    @Test
    void institutionAdminProvision() throws Exception {
        super.stubForManageProvidersAllowedByIdP(ORGANISATION_GUID);

        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/me", "new_institution_admin",
                institutionalAdminEntitlementOperator(ORGANISATION_GUID));

        User user = given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .get(accessCookieFilter.apiURL())
                .as(User.class);
        assertNotNull(user.getId());
        assertTrue(user.isInstitutionAdmin());
        assertEquals(ORGANISATION_GUID, user.getOrganizationGUID());
        assertEquals(4, user.getApplications().size());

        Map res = given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .get("/api/v1/users/config")
                .as(Map.class);
        assertTrue((Boolean) res.get("authenticated"));
    }

    @Test
    void meWithRoles() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/me", INVITER_SUB);

        String body = objectMapper.writeValueAsString(List.of(localManage.providerById(EntityType.OIDC10_RP, "5")));
        stubFor(post(urlPathMatching("/manage/api/internal/rawSearch/oidc10_rp")).willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(body)));

        User user = given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .get(accessCookieFilter.apiURL())
                .as(User.class);
        List<String> roleNames = Stream.of("Mail", "Calendar").sorted().toList();

        assertEquals(roleNames, user.getUserRoles().stream().map(userRole -> userRole.getRole().getName()).sorted().toList());
        List<Object> applicationIdentifiers = user.getUserRoles().stream()
                .map(userRole -> userRole.getRole().getApplicationMaps().get(0).get("id")).sorted().toList();
        assertEquals(List.of("5", "5"), applicationIdentifiers);
    }

    @Test
    void loginWithOauth2Login() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow(
                "/api/v1/users/login?force=true&hash=" + Authority.GUEST.name(),
                "urn:collab:person:example.com:admin",
                authorizationUrl -> {
                    MultiValueMap<String, String> queryParams = UriComponentsBuilder.fromUriString(authorizationUrl).build().getQueryParams();
                    String prompt = queryParams.getFirst("prompt");
                    assertEquals("login", prompt);
                    String loginHint = URLDecoder.decode(queryParams.getFirst("login_hint"), StandardCharsets.UTF_8);
                    assertEquals("https://login.test2.eduid.nl", loginHint);
                },
                m -> m);

        String location = given()
                .redirects()
                .follow(false)
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .get(accessCookieFilter.apiURL())
                .header("Location");
        assertEquals("http://localhost:3000", location);
    }

    @Test
    void meWithImpersonation() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", SUPER_SUB);

        User manager = userRepository.findBySubIgnoreCase(MANAGE_SUB).get();
        stubForManageProviderById(EntityType.SAML20_SP, "1");
        super.stubForManageProviderByEntityID(EntityType.SAML20_SP, "https://wiki");
        super.stubForManageProviderByEntityID(EntityType.OIDC10_RP, "https://calendar");


        User user = given()
                .when()
                .header("X-IMPERSONATE-ID", manager.getId().toString())
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .get("/api/v1/users/me")
                .as(User.class);
        assertEquals(MANAGE_SUB, user.getSub());
    }

    @Test
    void delete() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", SUPER_SUB);

        User institutionAdmin = userRepository.findBySubIgnoreCase(INSTITUTION_ADMIN_SUB).get();

        given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .pathParam("userId", institutionAdmin.getId())
                .delete("/api/v1/users/{userId}")
                .then()
                .statusCode(HttpStatus.NO_CONTENT.value());
        Optional<User> userOptional = userRepository.findBySubIgnoreCase(INSTITUTION_ADMIN_SUB);
        assertTrue(userOptional.isEmpty());
    }

    @Test
    void meWithImpersonationInstitutionAdmin() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", SUPER_SUB);

        User institutionAdmin = userRepository.findBySubIgnoreCase(INSTITUTION_ADMIN_SUB).get();
        stubForManageProvidersAllowedByIdP(institutionAdmin.getOrganizationGUID());

        User user = given()
                .when()
                .header("X-IMPERSONATE-ID", institutionAdmin.getId().toString())
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .get("/api/v1/users/me")
                .as(User.class);
        assertEquals(INSTITUTION_ADMIN_SUB, user.getEduPersonPrincipalName());
        assertEquals(4, user.getApplications().size());
        assertEquals(EntityType.SAML20_IDP.collectionName(), user.getInstitution().get("type"));
    }

    @Test
    void meWithNotAllowedImpersonation() throws Exception {
        super.stubForManageProvisioning(List.of("1", "5"));

        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", MANAGE_SUB);

        User guest = userRepository.findBySubIgnoreCase(GUEST_SUB).get();
        super.stubForManageProviderByEntityID(EntityType.SAML20_SP, "https://wiki");
        super.stubForManageProviderByEntityID(EntityType.OIDC10_RP, "https://calendar");

        User user = given()
                .when()
                .header("X-IMPERSONATE-ID", guest.getId().toString())
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .get("/api/v1/users/me")
                .as(User.class);
        assertEquals(MANAGE_SUB, user.getSub());
    }

    @Test
    void meWithAccessToken() throws IOException {
        User user = given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .auth()
                .oauth2(opaqueAccessToken(
                        "urn:collab:person:example.com:admin",
                        "introspect.json",
                        "jdoe@example.com"))
                .get("/api/external/v1/users/me")
                .as(User.class);
        assertEquals("jdoe@example.com", user.getEmail());
    }

    @Test
    void search() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", SUPER_SUB);

        DefaultPage<User> page = given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .queryParam("query", "mary")
                .get("/api/v1/users/search")
                .as(new TypeRef<>() {
                });
        assertEquals(1, page.getTotalElements());
    }

    @Test
    void searchWithAtSign() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", SUPER_SUB);

        DefaultPage<User> page = given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .queryParam("query", "james.doe@example.com")
                .get("/api/v1/users/search")
                .as(new TypeRef<>() {
                });
        assertEquals(1, page.getTotalElements());
    }

    @Test
    void searchPaginated() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", SUPER_SUB);

        DefaultPage<Map<String, Object>> page = given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .queryParam("query", "exam")
                .queryParam("pageNumber", 0)
                .queryParam("pageSize", 3)
                .queryParam("sort", "given_name")
                .queryParam("sortDirection", Sort.Direction.ASC.name())
                .get("/api/v1/users/search")
                .as(new TypeRef<>() {
                });
        assertEquals(3, page.getContent().size());
        assertEquals(6, page.getTotalElements());
    }

    @Test
    void searchPaginatedSortedAuthority() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", SUPER_SUB);

        DefaultPage<Map<String, Object>> page = given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .queryParam("query", "exam")
                .queryParam("pageNumber", 1)
                .queryParam("pageSize", 2)
                .queryParam("sort", "authority")
                .queryParam("sortDirection", Sort.Direction.DESC.name())
                .get("/api/v1/users/search")
                .as(new TypeRef<>() {
                });
        assertEquals(2, page.getContent().size());
        assertEquals(6, page.getTotalElements());
    }

    @Test
    void searchByApplication() throws Exception {
        //Institution admin is enriched with Manage information
        super.stubForManageProvidersAllowedByIdP(ORGANISATION_GUID);
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", INSTITUTION_ADMIN_SUB);

        DefaultPage<Map<String, Object>> usersPage = given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .queryParam("query", "doe")
                .get("/api/v1/users/search-by-application")
                .as(new TypeRef<>() {
                });
        assertEquals(3, usersPage.getTotalElements());
        assertEquals(3, usersPage.getContent().size());
        //Sorted by name default
        List<String> names = usersPage.getContent().stream().map(m -> (String) m.get("name")).toList();
        assertEquals(List.of("Ann Doe", "James Doe", "Mary Doe"), names);
    }

    @Test
    void searchAllUsersByApplication() throws Exception {
        //Institution admin is enriched with Manage information
        super.stubForManageProvidersAllowedByIdP(ORGANISATION_GUID);
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", INSTITUTION_ADMIN_SUB);

        DefaultPage<Map<String, Object>> usersPage = given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .queryParam("pageSize", 1)
                .get("/api/v1/users/search-by-application")
                .as(new TypeRef<>() {
                });
        assertEquals(4, usersPage.getTotalElements());
        assertEquals(1, usersPage.getContent().size());
    }

    @Test
    void searchUsersByApplicationSortEndDate() throws Exception {
        //Institution admin is enriched with Manage information
        super.stubForManageProvidersAllowedByIdP(ORGANISATION_GUID);
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", INSTITUTION_ADMIN_SUB);

        DefaultPage<Map<String, Object>> usersPage = given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .queryParam("query", "exam")
                .queryParam("pageNumber", 1)
                .queryParam("pageSize", 2)
                .queryParam("sort", "endDate")
                .queryParam("sortDirection", Sort.Direction.DESC.name())
                .get("/api/v1/users/search-by-application")
                .as(new TypeRef<>() {
                });
        assertEquals(4, usersPage.getTotalElements());
        assertEquals(2, usersPage.getContent().size());
    }

    @Test
    void otherByInstitutionAdmin() throws Exception {
        //Institution admin is enriched with Manage information
        super.stubForManageProvidersAllowedByIdP(ORGANISATION_GUID);
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", INSTITUTION_ADMIN_SUB);

        Long id = userRepository.findBySubIgnoreCase(GUEST_SUB).get().getId();

        User user = given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .pathParams("id", id)
                .get("/api/v1/users/other/{id}")
                .as(User.class);
        Set<UserRole> userRoles = user.getUserRoles();
        assertEquals(3, userRoles.size());
    }

    @Test
    void otherBYInstitutionAdminForbidden() throws Exception {
        //Institution admin is enriched with Manage information
        super.stubForManageProvidersAllowedByIdP(ORGANISATION_GUID);
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", INSTITUTION_ADMIN_SUB);

        Long id = userRepository.findBySubIgnoreCase(SUPER_SUB).get().getId();
        super.stubForManageProviderByEntityID(EntityType.OIDC10_RP, "https://calendar");

        given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .pathParams("id", id)
                .get("/api/v1/users/other/{id}")
                .then()
                .statusCode(HttpStatus.FORBIDDEN.value());
    }

    @Test
    void other() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", SUPER_SUB);
        Long id = userRepository.findBySubIgnoreCase(INVITER_SUB).get().getId();

        super.stubForManageProviderByEntityID(EntityType.OIDC10_RP, "https://calendar");

        User user = given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .pathParams("id", id)
                .get("/api/v1/users/other/{id}")
                .as(User.class);
        Set<UserRole> userRoles = user.getUserRoles();
        assertEquals(2, userRoles.size());
        assertEquals(List.of("5", "5"), userRoles.stream().map(userRole -> userRole.getRole().getApplicationMaps().get(0).get("id")).toList());
    }

    @Test
    void msAcceptReturn() throws Exception {
        super.stubForUpdateGraphUser(GUEST_SUB);
        super.stubForManageProviderById(EntityType.PROVISIONING, "9");

        User user = userRepository.findBySubIgnoreCase(GUEST_SUB).orElseThrow(() -> new NotFoundException("User not found"));
        given().redirects()
                .follow(false)
                .when()
                .pathParams("manageId", "9")
                .pathParams("userId", user.getId())
                .get("/api/v1/users/ms-accept-return/{manageId}/{userId}")
                .then()
                .statusCode(302)
                .header("Location", "http://localhost:4000/proceed?hash=GUEST&isRedirect=true");
    }

    @Test
    void logout() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", SUPER_SUB);
        given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .get("/api/v1/users/logout")
                .then()
                .statusCode(200);

        String location = given()
                .redirects()
                .follow(false)
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .get("/api/v1/users/login")
                .header("Location");
        assertEquals("http://localhost:" + port + "/oauth2/authorization/oidcng", location);
    }

    @Test
    void logoutUnauthenticated() {
        given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .get("/api/v1/users/logout")
                .then()
                .statusCode(200);

        String location = given()
                .redirects()
                .follow(false)
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .get("/api/v1/users/login")
                .header("Location");
        assertEquals("http://localhost:" + port + "/oauth2/authorization/oidcng", location);
    }

    @Test
    void error() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", SUPER_SUB);

        given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .contentType(ContentType.JSON)
                .body(Map.of("error", "trouble"))
                .post("/api/v1/users/error")
                .then()
                .statusCode(201);
    }

    @Test
    void meUpdateScim() throws Exception {
        User user = userRepository.findBySubIgnoreCase(GUEST_SUB).get();
        String remoteScimIdentifier = UUID.randomUUID().toString();
        RemoteProvisionedUser remoteProvisionedUser = new RemoteProvisionedUser(user, remoteScimIdentifier, "7");
        remoteProvisionedUserRepository.save(remoteProvisionedUser);

        super.stubForManageProvisioning(List.of("1", "4", "5"));
        super.stubForUpdateScimUser();

        //This will trigger the SCIM update request, see CustomOidcUserService#loadUser
        openIDConnectFlow("/api/v1/users/login", GUEST_SUB);

        List<LoggedRequest> loggedRequests = findAll(putRequestedFor(urlPathMatching("/api/scim/v2/Users/(.*)")));

        assertEquals(1, loggedRequests.size());
        Map<String, Object> userRequest = objectMapper.readValue(loggedRequests.get(0).getBodyAsString(), Map.class);
        assertEquals(remoteScimIdentifier, userRequest.get("id"));
    }

}