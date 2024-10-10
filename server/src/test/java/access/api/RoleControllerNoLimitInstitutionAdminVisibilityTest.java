package access.api;

import access.AbstractTest;
import access.AccessCookieFilter;
import access.manage.EntityType;
import access.model.Application;
import access.model.RemoteProvisionedGroup;
import access.model.Role;
import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.UUID;

import static access.security.SecurityConfig.API_TOKEN_HEADER;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

//Subclass properties are not merged, so we copy all from AbstractTest and add limit-institution-admin-role-visibility
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "oidcng.introspect-url=http://localhost:8081/introspect",
                "config.past-date-allowed=False",
                "spring.security.oauth2.client.provider.oidcng.authorization-uri=http://localhost:8081/authorization",
                "spring.security.oauth2.client.provider.oidcng.token-uri=http://localhost:8081/token",
                "spring.security.oauth2.client.provider.oidcng.user-info-uri=http://localhost:8081/user-info",
                "spring.security.oauth2.client.provider.oidcng.jwk-set-uri=http://localhost:8081/jwk-set",
                "manage.url: http://localhost:8081",
                "manage.enabled: true",
                "feature.limit-institution-admin-role-visibility=false"
        })
class RoleControllerNoLimitInstitutionAdminVisibilityTest extends AbstractTest {
    @Test
    void rolesByApplicationInstitutionAdminByAPI() throws Exception {
        super.stubForManagerProvidersByIdIn(EntityType.SAML20_SP, List.of("1", "2"));
        super.stubForManagerProvidersByIdIn(EntityType.OIDC10_RP, List.of("5"));
        super.stubForManageProvidersAllowedByIdP(ORGANISATION_GUID);

        List<Role> roles = given()
                .when()
                .header(API_TOKEN_HEADER, API_TOKEN_HASH)
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .get("/api/external/v1/roles")
                .as(new TypeRef<>() {
                });
        assertEquals(4, roles.size());
    }

    @Test
    void rolesByApplicationInstitutionAdmin() throws Exception {
        super.stubForManagerProvidersByIdIn(EntityType.SAML20_SP, List.of("1", "2"));
        super.stubForManagerProvidersByIdIn(EntityType.OIDC10_RP, List.of("5"));

        super.stubForManageProvidersAllowedByIdP(ORGANISATION_GUID);

        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/me", INSTITUTION_ADMIN_SUB,
                institutionalAdminEntitlementOperator(ORGANISATION_GUID));
        List<Role> roles = given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .contentType(ContentType.JSON)
                .get("/api/v1/roles")
                .as(new TypeRef<>() {
                });
        assertEquals(4, roles.size());
    }

    @Test
    void rolesByApplication() throws Exception {
        //Because the user is changed and provisionings are queried
        stubForManageProvisioning(List.of());
        stubForManageProvidersAllowedByIdP(ORGANISATION_GUID);
        stubForManagerProvidersByIdIn(EntityType.OIDC10_RP, List.of("5"));
        stubForManagerProvidersByIdIn(EntityType.SAML20_SP, List.of("1", "2"));

        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", INSTITUTION_ADMIN_SUB);

        super.stubForManagerProvidersByIdIn(EntityType.SAML20_SP, List.of("1"));

        List<Role> roles = given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .contentType(ContentType.JSON)
                .get("/api/v1/roles")
                .as(new TypeRef<>() {
                });
        assertEquals(4, roles.size());
    }

    @Test
    void deleteRoleWithAPI() throws Exception {
        Role role = roleRepository.search("network", 1).get(0);
        //Ensure delete provisioning is done
        remoteProvisionedGroupRepository.save(new RemoteProvisionedGroup(role, UUID.randomUUID().toString(), "7"));
        Application application = role.applicationsUsed().iterator().next();
        super.stubForManagerProvidersByIdIn(application.getManageType(), List.of(application.getManageId()));
        super.stubForManageProvidersAllowedByIdP(ORGANISATION_GUID);
        super.stubForManageProvisioning(List.of(application.getManageId()));
        super.stubForDeleteScimRole();

        given()
                .when()
                .accept(ContentType.JSON)
                .header(API_TOKEN_HEADER, API_TOKEN_HASH)
                .contentType(ContentType.JSON)
                .pathParams("id", role.getId())
                .delete("/api/external/v1/roles/{id}")
                .then()
                .statusCode(204);
        assertEquals(0, roleRepository.search("network", 1).size());
    }

}