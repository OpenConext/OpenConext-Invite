package access.manage;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("unchecked")
public class RemoteManage implements Manage {

    private final String url;
    private final RestTemplate restTemplate = new RestTemplate();
    private final Map<String, Object> queries;

    public RemoteManage(String url, String user, String password, ObjectMapper objectMapper) throws IOException {
        this.url = url;
        this.queries = objectMapper.readValue(new ClassPathResource("/manage/query_templates.json").getInputStream(), new TypeReference<>() {
        });
        restTemplate.getInterceptors().add(new BasicAuthenticationInterceptor(user, password));
    }

    @Override
    public List<Map<String, Object>> providers(EntityType... entityTypes) {
        return addIdentifierAlias(Stream.of(entityTypes).map(entityType -> this.getRemoteMetaData(entityType.collectionName()))
                .flatMap(List::stream)
                .toList());
    }

    @Override
    public List<Map<String, Object>> providersByIdIn(EntityType entityType, List<String> identifiers) {
        String param = identifiers.stream().map(id -> String.format("\"%s\"", id)).collect(Collectors.joining(","));
        String query = URLEncoder.encode(String.format("{ \"id\": { $in: [%s]}}", param), Charset.defaultCharset());
        String queryUrl = String.format("%s/manage/api/internal/rawSearch/%s?query=%s", url, entityType.collectionName(), query);
        return addIdentifierAlias(restTemplate.getForEntity(queryUrl, List.class).getBody());
    }

    @Override
    public Optional<Map<String, Object>> providerByEntityID(EntityType entityType, String entityID) {
        String query = URLEncoder.encode(String.format("{\"data.entityid\":\"%s\"}", entityID), Charset.defaultCharset());
        String queryUrl = String.format("%s/manage/api/internal/rawSearch/%s?query=%s", url, entityType.collectionName(), query);
        List<Map<String, Object>> providers = addIdentifierAlias(restTemplate.getForEntity(queryUrl, List.class).getBody());
        return providers.isEmpty() ? Optional.empty() : Optional.of(providers.get(0));
    }

    @Override
    public Map<String, Object> providerById(EntityType entityType, String id) {
        String queryUrl = String.format("%s/manage/api/internal/metadata/%s/%s", url, entityType.collectionName(), id);
        return addIdentifierAlias(restTemplate.getForEntity(queryUrl, Map.class).getBody());
    }


    @Override
    public List<Map<String, Object>> provisioning(List<String> ids) {
        String queryUrl = String.format("%s/manage/api/internal/provisioning", url);
        return addIdentifierAlias(restTemplate.postForObject(queryUrl, ids, List.class));
    }

    @Override
    public List<Map<String, Object>> allowedEntries(EntityType entityType, String id) {
        String queryUrl = String.format("%s/manage/api/internal/allowedEntities/%s/%s", url, entityType.collectionName(), id);
        return addIdentifierAlias(restTemplate.getForEntity(queryUrl, List.class).getBody());
    }

    @Override
    public List<Map<String, Object>> providersByInstitutionalGUID(String organisationGUID) {
        Map<String, Object> baseQuery = (Map<String, Object>) this.queries.get("base_query");
        baseQuery.put("metaDataFields.coin:institution_guid", organisationGUID);
        List serviceProviders = restTemplate.postForObject(
                String.format("%s/manage/api/internal/search/%s", this.url, EntityType.SAML20_SP.collectionName()),
                baseQuery, List.class);
        List relyingParties = restTemplate.postForObject(
                String.format("%s/manage/api/internal/search/%s", this.url, EntityType.OIDC10_RP.collectionName()),
                baseQuery, List.class);
        serviceProviders.addAll(relyingParties);
        return addIdentifierAlias(serviceProviders);
    }

    private List<Map<String, Object>> getRemoteMetaData(String type) {
        Object baseQuery = this.queries.get("base_query");
        String url = String.format("%s/manage/api/internal/search/%s", this.url, type);
        return addIdentifierAlias(restTemplate.postForObject(url, baseQuery, List.class));
    }

}
