package invite.api;

import invite.InviteServerApplication;
import invite.model.Authority;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import wiremock.org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MetricsControllerTest {

    @Test
    void prometheus() throws IOException {
        ConfigurableApplicationContext applicationContext = SpringApplication.run(InviteServerApplication.class, "--server.port=8098");
        RestAssured.port = 8098;

        InputStream inputStream = given()
                .when()
                .auth().preemptive().basic("internal", "secret")
                .get("/internal/prometheus")
                .asInputStream();
        String metrics = IOUtils.toString(inputStream, Charset.defaultCharset());
        SpringApplication.exit(applicationContext);

        List.of(
                        "total_number_of_pending_invitations",
                        "total_number_of_applications",
                        "total_number_of_super_users",
                        "total_number_of_institution_admins",
                        "total_number_of_access_roles",
                        "total_number_of_users",
                        "")
                .forEach(s -> assertTrue(metrics.contains(s)));

        Stream.of(Authority.values())
                .filter(authority -> !authority.equals(Authority.SUPER_USER) && !authority.equals(Authority.INSTITUTION_ADMIN))
                .forEach(authority -> assertTrue(metrics.contains("total_number_of_" + authority.name().toLowerCase() + "_users")));
    }

}