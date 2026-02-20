package invite.crm;

import invite.AbstractTest;
import invite.model.InvitationResponse;
import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static invite.security.SecurityConfig.API_KEY_HEADER;
import static invite.security.SecurityConfig.API_TOKEN_HEADER;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

class CRMControllerTest extends AbstractTest {

    @Test
    void contact() {
        CRMContact crmContact = new CRMContact(
                "contactId",
                "John",
                "from",
                "Doe",
                "jdoe@example.com",
                new CRMOrganisation(
                        "organisationId",
                        "abbrec",
                        "Inc. Corporated"
                ),
                List.of(new CRMRole("roleId","sabCode","Super"))
        );
        String response = given()
                .when()
                .accept(ContentType.JSON)
                .header(API_KEY_HEADER, "secret")
                .contentType(ContentType.JSON)
                .body(crmContact)
                .post("/api/internal/v1/crm")
                .then()
                .extract()
                .asString();
        assertEquals("created", response);
    }
}