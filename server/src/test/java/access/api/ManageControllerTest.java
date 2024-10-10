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

@SuppressWarnings("unchecked")
class ManageControllerTest extends AbstractTest {

    @Test
    void applications() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", SUPER_SUB);
        String spBody = objectMapper.writeValueAsString(localManage.providersByIdIn(EntityType.SAML20_SP, List.of("1", "2", "3", "4")));
        stubFor(post(urlPathMatching("/manage/api/internal/rawSearch/saml20_sp")).willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(spBody)));
        String rpBody = objectMapper.writeValueAsString(localManage.providersByIdIn(EntityType.OIDC10_RP, List.of("5")));
        stubFor(post(urlPathMatching("/manage/api/internal/rawSearch/oidc10_rp")).willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(rpBody)));
        stubForManageProvisioning(List.of("1", "2", "3", "4", "5"));

        Map<String, List<Map<String, Object>>> result = given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .contentType(ContentType.JSON)
                .get("/api/v1/manage/applications")
                .as(new TypeRef<>() {
                });
        assertEquals(5, result.get("providers").size());
        assertEquals(4, result.get("provisionings").size());

        List<LoggedRequest> loggedRequestsForSP = findAll(postRequestedFor(urlPathMatching("/manage/api/internal/rawSearch/saml20_sp")));
        assertEquals(1, loggedRequestsForSP.size());
        Map spRequest = objectMapper.readValue(loggedRequestsForSP.get(0).getBody(), Map.class);
        Map<String, List<String>> inSPParameter = (Map<String, List<String>>) spRequest.get("id");
        assertEquals(4, inSPParameter.get("$in").size());

        List<LoggedRequest> loggedRequestsForRP = findAll(postRequestedFor(urlPathMatching("/manage/api/internal/rawSearch/oidc10_rp")));
        assertEquals(1, loggedRequestsForSP.size());
        Map rpRequest = objectMapper.readValue(loggedRequestsForRP.get(0).getBody(), Map.class);
        Map<String, List<String>> inRPParameter = (Map<String, List<String>>) rpRequest.get("id");
        assertEquals(2, inRPParameter.get("$in").size());
    }

    @Test
    void providers() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", SUPER_SUB);

        String spBody = objectMapper.writeValueAsString(localManage.providers(EntityType.SAML20_SP));
        stubFor(post(urlPathMatching("/manage/api/internal/search/saml20_sp")).willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(spBody)));
        String rpBody = objectMapper.writeValueAsString(localManage.providers(EntityType.OIDC10_RP));
        stubFor(post(urlPathMatching("/manage/api/internal/search/oidc10_rp")).willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(rpBody)));

        List<Map<String, Object>> result = given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .contentType(ContentType.JSON)
                .get("/api/v1/manage/providers")
                .as(new TypeRef<>() {
                });
        assertEquals(6, result.size());
    }

    @Test
    void providerById() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", SUPER_SUB);

        String spBody = objectMapper.writeValueAsString(localManage.providerById(EntityType.SAML20_SP, "1"));
        stubFor(get(urlPathMatching("/manage/api/internal/metadata/saml20_sp/1")).willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(spBody)));
        Map<String, Object> result = given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .contentType(ContentType.JSON)
                .get("/api/v1/manage/provider/SAML20_SP/1")
                .as(new TypeRef<>() {
                });
        assertEquals("1", result.get("id"));
    }

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
        //Only two, because of the limited visibility for institution admin
        assertEquals(2, inSPParameter.get("$in").size());

        List<LoggedRequest> loggedRequestsForRP = findAll(postRequestedFor(urlPathMatching("/manage/api/internal/rawSearch/oidc10_rp")));
        assertEquals(1, loggedRequestsForRP.size());
        Map rpRequest = objectMapper.readValue(loggedRequestsForRP.get(0).getBody(), Map.class);
        Map<String, List<String>> inRPParameter = (Map<String, List<String>>) rpRequest.get("data.entityid");
        //Because of the query logic in RemoteManage#providersAllowedByIdP
        assertEquals(4, inRPParameter.get("$in").size());
    }

    @Test
    void organizationGUIDValidation() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", SUPER_SUB);

        String postPath = "/manage/api/internal/search/%s";
        Map<String, Object> localIdentityProvider = localManage.identityProviderByInstitutionalGUID(ORGANISATION_GUID).get();
        stubFor(post(urlPathMatching(String.format(postPath, EntityType.SAML20_IDP.collectionName()))).willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(objectMapper.writeValueAsString(List.of(localIdentityProvider)))));


        Map<String, Object> identityProvider = given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .contentType(ContentType.JSON)
                .pathParam("organizationGUID", ORGANISATION_GUID)
                .get("/api/v1/manage/organization-guid-validation/{organizationGUID}")
                .as(new TypeRef<>() {
                });
        assertEquals(ORGANISATION_GUID, identityProvider.get("institutionGuid"));
    }

    @Test
    void organizationGUIDValidationNotAllowed() throws Exception {
        stubForManageProvidersAllowedByIdP(ORGANISATION_GUID);
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", INSTITUTION_ADMIN_SUB);

        given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .contentType(ContentType.JSON)
                .pathParam("organizationGUID", ORGANISATION_GUID)
                .get("/api/v1/manage/organization-guid-validation/{organizationGUID}")
                .then()
                        .statusCode(403);
    }
}