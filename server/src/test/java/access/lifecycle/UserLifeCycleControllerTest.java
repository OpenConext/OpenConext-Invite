package access.lifecycle;

import access.AbstractTest;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static access.AbstractTest.GUEST_SUB;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

class UserLifeCycleControllerTest extends AbstractTest {

    @Test
    void preview() {
        LifeCycleResult lifeCycleResult = given()
                .when()
                .auth().basic("lifecyle", "secret")
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .pathParam("sub", GUEST_SUB)
                .get("/api/deprovision/{sub}")
                .as(new TypeRef<>() {
                });
        List<String> memberships = lifeCycleResult.getData().stream().filter(attribute -> attribute.getName().equals("membership"))
                .map(Attribute::getValue)
                .sorted().toList();
        assertEquals(List.of("Research", "Storage", "Wiki"), memberships);
    }

    @Test
    void lifeCycleRole() {
        given()
                .when()
                .auth().basic("voot", "secret")
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .pathParam("sub", GUEST_SUB)
                .get("/api/deprovision/{sub}")
                .then()
                .statusCode(400);
    }

    @Test
    void dryRun() {
        LifeCycleResult lifeCycleResult = given()
                .when()
                .auth().basic("lifecyle", "secret")
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .pathParam("sub", GUEST_SUB)
                .delete("/api/deprovision/{sub}/dry-run")
                .as(new TypeRef<>() {
                });
        List<String> memberships = lifeCycleResult.getData().stream().filter(attribute -> attribute.getName().equals("membership"))
                .map(Attribute::getValue)
                .sorted().toList();
        assertEquals(List.of("Research", "Storage", "Wiki"), memberships);
        assertTrue(userRepository.findBySubIgnoreCase(GUEST_SUB).isPresent());
    }

    @Test
    void deprovision() throws JsonProcessingException {
        super.stubForManageProvisioning(List.of("1"));
        super.stubForCreateScimUser();
        super.stubForCreateScimRole();
        super.stubForUpdateScimRole();
        super.stubForDeleteScimUser();
        LifeCycleResult lifeCycleResult = given()
                .when()
                .auth().basic("lifecyle", "secret")
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .pathParam("sub", GUEST_SUB)
                .delete("/api/deprovision/{sub}")
                .as(new TypeRef<>() {
                });
        List<String> memberships = lifeCycleResult.getData().stream().filter(attribute -> attribute.getName().equals("membership"))
                .map(Attribute::getValue)
                .sorted().toList();
        assertEquals(List.of("Research", "Storage", "Wiki"), memberships);
        assertFalse(userRepository.findBySubIgnoreCase(GUEST_SUB).isPresent());
    }


}
