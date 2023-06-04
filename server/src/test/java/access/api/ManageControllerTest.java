package access.api;

import access.AbstractTest;
import access.AccessCookieFilter;
import access.manage.EntityType;
import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

class ManageControllerTest extends AbstractTest {

    @Test
    void applications() throws Exception {
        AccessCookieFilter accessCookieFilter = openIDConnectFlow("/api/v1/users/login", "super@example.com");

        String spBody = objectMapper.writeValueAsString(localManage.providersByIdIn(EntityType.SAML20_SP, List.of("1","2","3", "4")));
        stubFor(get(urlPathMatching("/manage/api/internal/rawSearch/saml20_sp")).willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(spBody)));
        String rpBody = objectMapper.writeValueAsString(localManage.providersByIdIn(EntityType.OIDC10_RP, List.of("5")));
        stubFor(get(urlPathMatching("/manage/api/internal/rawSearch/oidc10_rp")).willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(rpBody)));

        List<Map<String, Object>> result = given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .header(accessCookieFilter.csrfToken().getHeaderName(), accessCookieFilter.csrfToken().getToken())
                .contentType(ContentType.JSON)
                .get("/api/v1/manage/applications")
                .as(new TypeRef<>() {
                });
        assertEquals(5, result.size());
    }

}