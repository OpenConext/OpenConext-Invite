package access.manage;

import access.AbstractTest;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RemoteManageTest extends AbstractTest {

    @Autowired
    private Manage manage;

    @Test
    void providers() throws JsonProcessingException {
        LocalManage localManage = new LocalManage(objectMapper);
        List<Map<String, Object>> serviceProviders = localManage.providers(EntityType.SP);
        String body = objectMapper.writeValueAsString(serviceProviders);
        stubFor(post(urlPathMatching("/manage/api/internal/search/saml20_sp")).willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(body)));
        List<Map<String, Object>> remoteServiceProviders = manage.providers(EntityType.SP);
        assertEquals(28, remoteServiceProviders.size());
    }

    @Test
    void providerById() throws JsonProcessingException {
        LocalManage localManage = new LocalManage(objectMapper);
        Map<String, Object> provider = localManage.providerById(EntityType.SP, "76171457-6ed5-4505-9071-9e0e47557985");
        String body = objectMapper.writeValueAsString(provider);
        stubFor(get(urlPathMatching("/manage/api/internal/rawSearch/saml20_sp")).willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(body)));
        Map<String, Object> remoteProvider = manage.providerById(EntityType.SP, "76171457-6ed5-4505-9071-9e0e47557985");
        assertEquals(provider, remoteProvider);
    }
}