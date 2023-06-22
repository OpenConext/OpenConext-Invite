package access.api;

import access.AbstractTest;
import access.AccessCookieFilter;
import access.manage.EntityType;
import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static access.Seed.MANAGE_SUB;
import static access.Seed.SUPER_SUB;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("unchecked")
class ManageControllerTest extends AbstractTest {

    @Test
    void applications() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", SUPER_SUB);
        String spBody = objectMapper.writeValueAsString(localManage.providersByIdIn(EntityType.SAML20_SP, List.of("1", "2", "3", "4")));
        stubFor(get(urlPathMatching("/manage/api/internal/rawSearch/saml20_sp")).willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(spBody)));
        String rpBody = objectMapper.writeValueAsString(localManage.providersByIdIn(EntityType.OIDC10_RP, List.of("5")));
        stubFor(get(urlPathMatching("/manage/api/internal/rawSearch/oidc10_rp")).willReturn(aResponse()
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
    void provisioningBySuperUser() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", SUPER_SUB);

        String spBody = objectMapper.writeValueAsString(localManage.provisioning(List.of("1")));
        stubFor(post(urlPathMatching("/manage/api/internal/provisioning")).willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(spBody)));
        List<Map<String, Object>> result = given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .contentType(ContentType.JSON)
                .get("/api/v1/manage/provisioning/1")
                .as(new TypeRef<>() {
                });
        assertEquals(1, result.size());

        Map<String, Object> data = (Map<String, Object>) result.get(0).get("data");
        Map<String, Object> metaDataFields = (Map<String, Object>) data.get("metaDataFields");
        assertTrue(Stream.of("scim_url", "scim_user", "scim_password")
                .allMatch(metaDataFields::containsKey));
    }

    @Test
    void provisioningByManager() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", MANAGE_SUB);

        String spBody = objectMapper.writeValueAsString(localManage.provisioning(List.of("1")));
        stubFor(post(urlPathMatching("/manage/api/internal/provisioning")).willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(spBody)));
        List<Map<String, Object>> result = given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .contentType(ContentType.JSON)
                .get("/api/v1/manage/provisioning/1")
                .as(new TypeRef<>() {
                });
        assertEquals(1, result.size());

        Map<String, Object> data = (Map<String, Object>) result.get(0).get("data");
        Map<String, Object> metaDataFields = (Map<String, Object>) data.get("metaDataFields");
        assertTrue(Stream.of("scim_url", "scim_user", "scim_password", "graph_token", "eva_token")
                .noneMatch(metaDataFields::containsKey));
        assertTrue(Stream.of("name:en")
                .allMatch(metaDataFields::containsKey));
    }
}