package invite.eduid;

import invite.WireMockExtension;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Optional;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EduIDTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final EduID eduID = new EduID("http://localhost:8081/myconext/api/invite/provision-eduid", "invite", "secret");

    @RegisterExtension
    WireMockExtension mockServer = new WireMockExtension(8081);

    @SneakyThrows
    @Test
    void provisionEduid() {
        String eduIDValue = UUID.randomUUID().toString();
        String institutionGUID = UUID.randomUUID().toString();
        EduIDProvision eduIDProvision = new EduIDProvision(eduIDValue, institutionGUID);
        stubFor(post(urlPathMatching("/myconext/api/invite/provision-eduid")).willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(objectMapper.writeValueAsString(eduIDProvision))));
        Optional<String> eduid = eduID.provisionEduid(eduIDProvision);
        assertEquals(eduIDProvision.getEduIDValue(), eduid.get());
    }

    @SneakyThrows
    @Test
    void provisionEduid404() {
        EduIDProvision eduIDProvision = new EduIDProvision("eduIDValue", "institutionGUID");
        stubFor(post(urlPathMatching("/myconext/api/invite/provision-eduid")).willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withStatus(404)));
        Optional<String> eduid = eduID.provisionEduid(eduIDProvision);
        assertTrue(eduid.isEmpty());
    }
}