package access.profile;

import access.AbstractTest;
import access.manage.EntityType;
import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ProfileControllerTest extends AbstractTest {

    @Test
    void roles() {
        stubForManagerProvidersByIdIn(EntityType.SAML20_SP, List.of("1", "3", "4"));
        stubForManagerProvidersByIdIn(EntityType.OIDC10_RP, List.of("5"));

        List<UserRoleProfile> roles = given()
                .when()
                .auth().preemptive().basic("profile", "secret")
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .queryParam("collabPersonId", GUEST_SUB)
                .get("/api/profile")
                .as(new TypeRef<>() {
                });
        assertEquals(3, roles.size());

        UserRoleProfile research = roles.stream().filter(p -> p.getName().equals("Research")).findFirst().get();
        assertEquals("Research desc", research.getDescription());
        assertEquals(1, research.getApplications().size());

        ApplicationInfo applicationInfo = research.getApplications().get(0);
        assertEquals("Research EN", applicationInfo.getNameEn());
        assertEquals("Research NL", applicationInfo.getNameNl());
        assertEquals("SURF bv", applicationInfo.getOrganisationEn());
        assertNull(applicationInfo.getOrganisationNl());
        assertEquals("http://landingpage.com", applicationInfo.getLandingPage());
        assertEquals("https://static.surfconext.nl/media/idp/surfconext.png", applicationInfo.getLogo());

    }

    @Test
    void rolesGuestRoleIncluded() {
        stubForManagerProvidersByIdIn(EntityType.SAML20_SP, List.of("1"));

        List<UserRoleProfile> roles = given()
                .when()
                .auth().preemptive().basic("profile", "secret")
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .queryParam("collabPersonId", MANAGE_SUB)
                .get("/api/profile")
                .as(new TypeRef<>() {
                });
        assertEquals(1, roles.size());
    }

        @Test
    void rolesNotExistentUser() {
        List<UserRoleProfile> roles = given()
                .when()
                .auth().preemptive().basic("profile", "secret")
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .queryParam("collabPersonId", "nope")
                .get("/api/profile")
                .as(new TypeRef<>() {
                });
        assertEquals(0, roles.size());
    }

    @Test
    void rolesForbidden() {
        given()
                .when()
                .auth().preemptive().basic("teams", "secret")
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .queryParam("collabPersonId", "nope")
                .get("/api/profile")
                .then()
                .statusCode(403);
    }
}