package invite.security;

import invite.AbstractTest;
import invite.config.HashGenerator;
import invite.manage.EntityType;
import invite.model.APIToken;
import invite.model.User;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static invite.security.SecurityConfig.API_TOKEN_HEADER;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class UserHandlerMethodArgumentResolverTest extends AbstractTest {

    @BeforeEach
    protected void beforeEach() throws Exception {
        super.beforeEach();
        stubForManageProvisioning(List.of());
        stubForManageProvidersAllowedByIdP(ORGANISATION_GUID);
    }

    private int status(String token, String path) {
        return given()
                .when()
                .header(API_TOKEN_HEADER, token)
                .accept(ContentType.JSON)
                .get("/api/external/v1" + path)
                .statusCode();
    }

    private int institutionAdminStatus(String token) {
        return status(token, "/users/institutionAdmins");
    }

    private String createToken(String organizationGUID, boolean superUserToken, User owner) {
        String token = UUID.randomUUID().toString();
        apiTokenRepository.save(new APIToken(organizationGUID, HashGenerator.hashToken(token), superUserToken, "test", owner));
        return token;
    }

    private User user(String sub) {
        return userRepository.findBySubIgnoreCase(sub).orElseThrow();
    }

    @Test
    void tokenOfDowngradedInstitutionAdmin() throws Exception {
        openIDConnectFlow("/api/v1/users/login", GUEST_SUB, institutionalAdminEntitlementOperator(ORGANISATION_GUID));
        String token = createToken(ORGANISATION_GUID, false, user(GUEST_SUB));
        assertEquals(200, institutionAdminStatus(token));

        //Entitlement is no longer received
        openIDConnectFlow("/api/v1/users/login", GUEST_SUB);
        assertFalse(user(GUEST_SUB).isInstitutionAdmin());
        assertEquals(403, institutionAdminStatus(token));

        openIDConnectFlow("/api/v1/users/login", GUEST_SUB, institutionalAdminEntitlementOperator(ORGANISATION_GUID));
        assertEquals(200, institutionAdminStatus(token));
    }

    @Test
    void tokenOfInstitutionAdminByInvite() throws Exception {
        //No entitlement received
        openIDConnectFlow("/api/v1/users/login", INSTITUTION_ADMIN_SUB);
        assertEquals(200, institutionAdminStatus(API_TOKEN_HASH));
    }

    @Test
    void tokenOfRemovedInstitutionAdmin() {
        User user = user(INSTITUTION_ADMIN_SUB);
        user.setInstitutionAdmin(false);
        user.setInstitutionAdminByInvite(false);
        user.setOrganizationGUID(null);
        userRepository.save(user);
        assertEquals(403, institutionAdminStatus(API_TOKEN_HASH));
    }

    @Test
    void tokenOfDowngradedInstitutionAdminKeepsOwnRoles() {
        //The manager of the Wiki role is not an institution admin, and not a member of the Network role of the same organization
        String token = createToken(ORGANISATION_GUID, false, user(MANAGE_SUB));
        Long wikiId = roleRepository.findByName("Wiki").orElseThrow().getId();
        Long networkId = roleRepository.findByName("Network").orElseThrow().getId();
        stubForManagerProvidersByIdIn(EntityType.SAML20_SP, List.of("1", "2"));

        assertEquals(403, institutionAdminStatus(token));
        assertEquals(200, status(token, "/roles/" + wikiId));
        assertEquals(403, status(token, "/roles/" + networkId));
    }

    @Test
    void tokenOfOtherOrganization() {
        User user = user(INSTITUTION_ADMIN_SUB);
        user.setOrganizationGUID("test_institution_guid");
        userRepository.save(user);
        assertEquals(403, institutionAdminStatus(API_TOKEN_HASH));
    }

    @Test
    void tokenOfDowngradedSuperUser() {
        User user = user(SUPER_SUB);
        String token = createToken(ORGANISATION_GUID, true, user);
        assertEquals(200, institutionAdminStatus(token));

        user.setSuperUser(false);
        userRepository.save(user);
        assertEquals(403, institutionAdminStatus(token));
    }

    @Test
    void tokenWithoutOwner() {
        String token = createToken(ORGANISATION_GUID, false, null);
        assertEquals(200, institutionAdminStatus(token));

        User user = user(INSTITUTION_ADMIN_SUB);
        user.setInstitutionAdmin(false);
        userRepository.save(user);
        assertEquals(403, institutionAdminStatus(token));
    }

}
