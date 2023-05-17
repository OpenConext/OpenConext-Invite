package access.manage;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.web.client.RestTemplate;

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
    public Map<String, Object> serviceProviders() {
        return restTemplate.postForObject(url + "/manage/api/internal/search/saml20_sp", this.queries.get("base_query"), Map.class);
    }

    @Override
    public Map<String, Object> identityProviders() {
        return restTemplate.postForObject(url + "/manage/api/internal/internal/saml20_idp", this.queries.get("base_query"), Map.class);
    }
}
