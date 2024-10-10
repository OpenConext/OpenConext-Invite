package access.api;

import access.AbstractTest;
import access.AccessCookieFilter;
import access.exception.NotFoundException;
import access.manage.EntityType;
import access.model.Authority;
import access.model.RemoteProvisionedUser;
import access.model.User;
import access.model.UserRole;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;
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
@SuppressWarnings("unchecked")
class UserControllerNoLimitInstitutionAdminVisibilityTest extends AbstractTest {

    @Test
    void searchByApplication() throws Exception {
        //Institution admin is enriched with Manage information
        super.stubForManageProvidersAllowedByIdP(ORGANISATION_GUID);
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", INSTITUTION_ADMIN_SUB);

        List<Map<String, Object>> users = given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .queryParam("query", "Doe")
                .get("/api/v1/users/search-by-application")
                .as(new TypeRef<>() {
                });
        assertEquals(3, users.size());
    }

    @Test
    void searchAllUsersByApplication() throws Exception {
        //Institution admin is enriched with Manage information
        super.stubForManageProvidersAllowedByIdP(ORGANISATION_GUID);
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", INSTITUTION_ADMIN_SUB);

        List<Map<String, Object>> users = given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .queryParam("query", "owl")
                .get("/api/v1/users/search-by-application")
                .as(new TypeRef<>() {
                });
        assertEquals(3, users.size());
    }

}