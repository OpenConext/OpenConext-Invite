package access.manage;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;

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
        ResponseErrorHandler resilientErrorHandler = new ResilientErrorHandler();
        restTemplate.setErrorHandler(resilientErrorHandler);
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
            return emptyList();
        }
        String param = identifiers.stream().map(id -> String.format("\"%s\"", id)).collect(Collectors.joining(","));
        String body = String.format("{ \"id\": { \"$in\": [%s]}}", param);
        String manageUrl = String.format("%s/manage/api/internal/rawSearch/%s", url, entityType.collectionName());
        List<Map<String, Object>> providers = restTemplate.postForObject(manageUrl, body, List.class);
        return transformProvider(providers);
    }

    @Override
    public Optional<Map<String, Object>> providerByEntityID(EntityType entityType, String entityID) {
        String body = String.format("{\"data.entityid\":\"%s\"}", entityID);
        String manageUrl = String.format("%s/manage/api/internal/rawSearch/%s", url, entityType.collectionName());
        List<Map<String, Object>> providers = restTemplate.postForObject(manageUrl, body, List.class);
        List<Map<String, Object>> allProviders = transformProvider(providers);
        return allProviders.isEmpty() ? Optional.empty() : Optional.of(allProviders.get(0));
    }

    @Override
    public Map<String, Object> providerById(EntityType entityType, String id) {
        String queryUrl = String.format("%s/manage/api/internal/metadata/%s/%s", url, entityType.collectionName(), id);
        return transformProvider(restTemplate.getForEntity(queryUrl, Map.class).getBody());
    }


    @Override
    public List<Map<String, Object>> provisioning(Collection<String> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return emptyList();
        }
        String queryUrl = String.format("%s/manage/api/internal/provisioning", url);
        return transformProvider(restTemplate.postForObject(queryUrl, ids, List.class));
    }

    @Override
    public List<Map<String, Object>> providersAllowedByIdP(Map<String, Object> identityProvider) {
        Boolean allowedAll = (Boolean) identityProvider.getOrDefault("allowedall", Boolean.FALSE);
        if (allowedAll) {
            return this.providers(EntityType.SAML20_SP, EntityType.OIDC10_RP);
        }
        List<Map< String, String>> allowedEntities = (List<Map<String, String>>) identityProvider.getOrDefault("allowedEntities", emptyList());
        String split = allowedEntities.stream().map(m -> "\"" + m.get("name") + "\"")
                .collect(Collectors.joining(","));
        String body =  String.format("{\"data.entityid\":{\"$in\":[%s]}}", split) ;
        List<Map<String, Object>> results = new ArrayList<>();
        List.of(EntityType.SAML20_SP, EntityType.OIDC10_RP).forEach(entityType -> {
            String manageUrl = String.format("%s/manage/api/internal/rawSearch/%s", url,
                    entityType.collectionName());
            List<Map<String, Object>> providers = restTemplate.postForObject(manageUrl, body, List.class);
            List<Map<String, Object>> transformedProviders = transformProvider(providers);
            results.addAll(transformedProviders);
        });
        return results;
    }

    @Override
    public Optional<Map<String, Object>> identityProviderByInstitutionalGUID(String organisationGUID) {
        Map<String, Object> baseQuery = getBaseQuery();
        baseQuery.put("metaDataFields.coin:institution_guid", organisationGUID);

        List<String> requestedAttributes = (List<String>) baseQuery.get("REQUESTED_ATTRIBUTES");
        requestedAttributes.add("allowedEntities");
        requestedAttributes.add("allowedall");

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
        HashMap<String, Object> baseQuery = new HashMap<>((Map<String, Object>) this.queries.get("base_query"));
        baseQuery.put("REQUESTED_ATTRIBUTES", baseQuery.get("REQUESTED_ATTRIBUTES"));
        return baseQuery;
    }


}
