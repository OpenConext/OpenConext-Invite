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

    private final boolean local = false;

    @Override
    protected boolean seedDatabase() {
        return false;
    }

    @Test
    void providers() throws JsonProcessingException {
        LocalManage localManage = new LocalManage(objectMapper, local);
        List<Map<String, Object>> serviceProviders = localManage.providers(EntityType.SAML20_SP);
        String body = objectMapper.writeValueAsString(serviceProviders);
        stubFor(post(urlPathMatching("/manage/api/internal/search/saml20_sp")).willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(body)));
        List<Map<String, Object>> remoteServiceProviders = manage.providers(EntityType.SAML20_SP);
        assertEquals(4, remoteServiceProviders.size());
    }

    @Test
    void providerById() throws JsonProcessingException {
        LocalManage localManage = new LocalManage(objectMapper, local);
        Map<String, Object> provider = localManage.providerById(EntityType.SAML20_SP, "1");
        String body = objectMapper.writeValueAsString(provider);
        stubFor(get(urlPathMatching("/manage/api/internal/metadata/saml20_sp/1")).willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(body)));
        Map<String, Object> remoteProvider = manage.providerById(EntityType.SAML20_SP, "1");
        assertEquals(provider, remoteProvider);
    }

    @Test
    void providersByIdIn() throws JsonProcessingException {
        LocalManage localManage = new LocalManage(objectMapper, local);
        List<Map<String, Object>> providers = localManage.providersByIdIn(EntityType.SAML20_SP,List.of("1","3","4"));
        String body = objectMapper.writeValueAsString(providers);
        stubFor(get(urlPathMatching("/manage/api/internal/rawSearch/saml20_sp")).willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(body)));
        List<Map<String, Object>> remoteProviders =  manage.providersByIdIn(EntityType.SAML20_SP,List.of("1","3","4"));
        assertEquals(providers, remoteProviders);
    }

    @Test
    void allowedEntries() throws JsonProcessingException {
        LocalManage localManage = new LocalManage(objectMapper, local);
        List<Map<String, Object>> serviceProviders = localManage.allowedEntries(EntityType.SAML20_SP, "1");
        String body = objectMapper.writeValueAsString(serviceProviders);
        stubFor(get(urlPathMatching("/manage/api/internal/allowedEntities/saml20_sp/1")).willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(body)));
        List<Map<String, Object>> allowedEntries = manage.allowedEntries(EntityType.SAML20_SP, "1");
        assertEquals(2, allowedEntries.size());
    }

}