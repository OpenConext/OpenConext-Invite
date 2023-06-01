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
import java.util.stream.Collectors;

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
    public List<Map<String, Object>> providersByIdIn(EntityType entityType, List<String> identifiers) {
        String param = identifiers.stream().map(id -> String.format("\"%s\"", id)).collect(Collectors.joining(","));
        String query = URLEncoder.encode(String.format("{ \"id\": { $in: [%s]}}", param), Charset.defaultCharset());
        String queryUrl = String.format("%s/manage/api/internal/rawSearch/%s?query=%s", url, entityType.getType(), query);
        return addIdentifierAlias(restTemplate.getForEntity(queryUrl, List.class).getBody());
    }

    @Override
    public Map<String, Object> providerById(EntityType entityType, String id) {
        String queryUrl = String.format("%s/manage/api/internal/metadata/%s/%s", url, entityType.getType(), id);
        return addIdentifierAlias(restTemplate.getForEntity(queryUrl, Map.class).getBody());
    }

    @Override
    public List<Map<String, Object>> provisioning(String providerId) {
        String queryUrl = String.format("%s/manage/api/internal/provisioning/%s", url, providerId);
        return addIdentifierAlias(restTemplate.getForEntity(queryUrl, List.class).getBody());
    }

    private List<Map<String, Object>> getRemoteMetaData(String type) {
        Object baseQuery = this.queries.get("base_query");
        String url = String.format("%s/manage/api/internal/search/%s", this.url, type);
        return addIdentifierAlias(restTemplate.postForObject(url, baseQuery, List.class));

    }
}
