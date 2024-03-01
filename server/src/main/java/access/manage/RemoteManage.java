package access.manage;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.*;
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
        return Stream.of(entityTypes).map(entityType -> this.getRemoteMetaData(entityType.collectionName()))
                .flatMap(List::stream)
                .toList();
    }

    @Override
    public List<Map<String, Object>> providersByIdIn(EntityType entityType, List<String> identifiers) {
        if (CollectionUtils.isEmpty(identifiers)) {
            return Collections.emptyList();
        }
        String param = identifiers.stream().map(id -> String.format("\"%s\"", id)).collect(Collectors.joining(","));
        String query = URLEncoder.encode(String.format("{ \"id\": { $in: [%s]}}", param), Charset.defaultCharset());
        String queryUrl = String.format("%s/manage/api/internal/rawSearch/%s?query=%s", url, entityType.collectionName(), query);
        return transformProvider(restTemplate.getForEntity(queryUrl, List.class).getBody());
    }

    @Override
    public Optional<Map<String, Object>> providerByEntityID(EntityType entityType, String entityID) {
        String query = URLEncoder.encode(String.format("{\"data.entityid\":\"%s\"}", entityID), Charset.defaultCharset());
        String queryUrl = String.format("%s/manage/api/internal/rawSearch/%s?query=%s", url, entityType.collectionName(), query);
        List<Map<String, Object>> providers = transformProvider(restTemplate.getForEntity(queryUrl, List.class).getBody());
        return providers.isEmpty() ? Optional.empty() : Optional.of(providers.get(0));
    }

    @Override
    public Map<String, Object> providerById(EntityType entityType, String id) {
        String queryUrl = String.format("%s/manage/api/internal/metadata/%s/%s", url, entityType.collectionName(), id);
        return transformProvider(restTemplate.getForEntity(queryUrl, Map.class).getBody());
    }


    @Override
    public List<Map<String, Object>> provisioning(Collection<String> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyList();
        }
        String queryUrl = String.format("%s/manage/api/internal/provisioning", url);
        return transformProvider(restTemplate.postForObject(queryUrl, ids, List.class));
    }

    @Override
    public List<Map<String, Object>> providersByInstitutionalGUID(String organisationGUID) {
        Map<String, Object> baseQuery = getBaseQuery();
        baseQuery.put("metaDataFields.coin:institution_guid", organisationGUID);
        List serviceProviders = restTemplate.postForObject(
                String.format("%s/manage/api/internal/search/%s", this.url, EntityType.SAML20_SP.collectionName()),
                baseQuery, List.class);
        List relyingParties = restTemplate.postForObject(
                String.format("%s/manage/api/internal/search/%s", this.url, EntityType.OIDC10_RP.collectionName()),
                baseQuery, List.class);
        serviceProviders.addAll(relyingParties);
        return transformProvider(serviceProviders);
    }

    @Override
    public Optional<Map<String, Object>> identityProviderByInstitutionalGUID(String organisationGUID) {
        Map<String, Object> baseQuery = getBaseQuery();
        baseQuery.put("metaDataFields.coin:institution_guid", organisationGUID);
        List<Map<String, Object> > identityProviders = restTemplate.postForObject(
                String.format("%s/manage/api/internal/search/%s", this.url, EntityType.SAML20_IDP.collectionName()),
                baseQuery, List.class);
        return identityProviders.isEmpty() ? Optional.empty() : Optional.of(transformProvider(identityProviders.get(0)));
    }

    private List<Map<String, Object>> getRemoteMetaData(String type) {
        Map<String, Object> baseQuery = getBaseQuery();
        String url = String.format("%s/manage/api/internal/search/%s", this.url, type);
        return transformProvider(restTemplate.postForObject(url, baseQuery, List.class));
    }

    private Map<String, Object> getBaseQuery() {
        return new HashMap<>((Map<String, Object>) this.queries.get("base_query"));
    }


}
