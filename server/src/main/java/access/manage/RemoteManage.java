package access.manage;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unchecked")
public class RemoteManage implements Manage {

    private final String url;
    private final RestTemplate restTemplate = new RestTemplate();
    private final Map<String, Object> queries;

    @SneakyThrows
    public RemoteManage(String url, String user, String password, ObjectMapper objectMapper) {
        this.url = url;
        this.queries = objectMapper.readValue(new ClassPathResource("/manage/query_templates.json").getInputStream(), new TypeReference<>() {
        });
        restTemplate.getInterceptors().add(new BasicAuthenticationInterceptor(user, password));
    }

    @Override
    public List<Map<String, Object>> providers(EntityType entityType) {
        return getRemoteMetaData(entityType.getType());
    }

    @Override
    public Map<String, Object> providerById(EntityType entityType, String id) {
        String query = URLEncoder.encode(String.format("{\"_id\":\"%s\"}", id), Charset.defaultCharset());
        String queryUrl = String.format("%s/manage/api/internal/rawSearch/%s?query=%s", url, entityType.getType(), id, query);
        return restTemplate.getForEntity(queryUrl, Map.class).getBody();
    }

    @Override
    public List<Map<String, Object>> provisioning(String providerId) {
        String queryUrl = String.format("%s/manage/api/internal/provisioning/%s", url, providerId);
        return restTemplate.getForEntity(queryUrl, List.class).getBody();
    }

    private List<Map<String, Object>> getRemoteMetaData(String type) {
        return restTemplate.postForObject(String.format("%s/manage/api/internal/search/%s", url, type), this.queries.get("base_query"), List.class);
    }
}
