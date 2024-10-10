package access.api;

import access.AbstractTest;
import access.AccessCookieFilter;
import access.manage.EntityType;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
class ManageControllerNoLimitInstitutionAdminVisibilityTest extends AbstractTest {

    @Test
    void applicationsByInstitutionAdmin() throws Exception {
        stubForManageProvidersAllowedByIdP(ORGANISATION_GUID);

        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", INSTITUTION_ADMIN_SUB);
        super.stubForManagerProvidersByIdIn(EntityType.SAML20_SP, List.of("1", "2", "3", "4"));
        stubForManageProvisioning(List.of("1", "2", "3", "4"));

        Map<String, List<Map<String, Object>>> result = given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .contentType(ContentType.JSON)
                .get("/api/v1/manage/applications")
                .as(new TypeRef<>() {
                });
        assertEquals(4, result.get("providers").size());
        assertEquals(4, result.get("provisionings").size());

        List<LoggedRequest> loggedRequestsForSP = findAll(postRequestedFor(urlPathMatching("/manage/api/internal/rawSearch/saml20_sp")));
        assertEquals(2, loggedRequestsForSP.size());
        Map spRequest = objectMapper.readValue(loggedRequestsForSP.get(0).getBody(), Map.class);
        //Because of the query logic in RemoteManage#providersAllowedByIdP
        Map<String, List<String>> inSPParameter = (Map<String, List<String>>) spRequest.get("data.entityid");
        assertEquals(4, inSPParameter.get("$in").size());

        spRequest = objectMapper.readValue(loggedRequestsForSP.get(1).getBody(), Map.class);
        inSPParameter = (Map<String, List<String>>) spRequest.get("id");
        //Four, because of NO limited visibility for institution admin
        assertEquals(4, inSPParameter.get("$in").size());

        List<LoggedRequest> loggedRequestsForRP = findAll(postRequestedFor(urlPathMatching("/manage/api/internal/rawSearch/oidc10_rp")));
        assertEquals(1, loggedRequestsForRP.size());
        Map rpRequest = objectMapper.readValue(loggedRequestsForRP.get(0).getBody(), Map.class);
        Map<String, List<String>> inRPParameter = (Map<String, List<String>>) rpRequest.get("data.entityid");
        //Because of the query logic in RemoteManage#providersAllowedByIdP
        assertEquals(4, inRPParameter.get("$in").size());
    }
}