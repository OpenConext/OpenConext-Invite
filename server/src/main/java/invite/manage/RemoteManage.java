package invite.manage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.joining;

@SuppressWarnings("unchecked")
public class RemoteManage implements Manage {

    private static final Log LOG = LogFactory.getLog(RemoteManage.class);

    private final String url;
    private final RestTemplate restTemplate = new RestTemplate();

    public RemoteManage(String url, String user, String password) {
        this.url = url;
        restTemplate.getInterceptors().add(new BasicAuthenticationInterceptor(user, password));
        ResponseErrorHandler resilientErrorHandler = new ResilientErrorHandler();
        restTemplate.setErrorHandler(resilientErrorHandler);
    }

    @Override
    public List<Map<String, Object>> providers(EntityType... entityTypes) {
        LOG.debug("Providers for entityTypes: " + List.of(entityTypes));
        return Stream.of(entityTypes).map(entityType -> this.getRemoteMetaData(entityType.collectionName()))
                .flatMap(List::stream)
                .toList();
    }

    @Override
    public List<Map<String, Object>> providersByIdIn(EntityType entityType, List<String> identifiers) {
        LOG.debug(String.format("providersByIdIn %s", entityType));
        if (CollectionUtils.isEmpty(identifiers)) {
            LOG.debug("No identifiers in providersByIdIn");
            return emptyList();
        }
        String param = identifiers.stream().map(id -> String.format("\"%s\"", id)).collect(joining(","));
        String body = String.format("{ \"id\": { \"$in\": [%s]}}", param);
        String manageUrl = String.format("%s/manage/api/internal/rawSearch/%s", url, entityType.collectionName());
        List<Map<String, Object>> providers = restTemplate.postForObject(manageUrl, body, List.class);
        if (providers != null) {
            LOG.debug(String.format("Got %d results for providersByIdIn", providers.size()));
        }
        return transformProvider(providers);
    }

    @Override
    public List<String> idpEntityIdentifiersByServiceEntityId(List<String> serviceEntityIdentifiers) {
        LOG.debug("idpEntityIdentifiersByServiceEntityId");
        if (CollectionUtils.isEmpty(serviceEntityIdentifiers)) {
            LOG.debug("No identifiers in idpEntityIdentifiersByServiceEntityId");
            return emptyList();
        }
        Map<String, Object> body = Map.of(
                "allowedEntities.name", serviceEntityIdentifiers,
                "allowedall", true,
                "LOGICAL_OPERATOR_IS_AND", false,
                "REQUESTED_ATTRIBUTES", List.of("entityid")
        );
        String manageUrl = String.format("%s/manage/api/internal/search/%s", url, EntityType.SAML20_IDP.collectionName());
        List<Map<String, Object>> providers = restTemplate.postForObject(manageUrl, body, List.class);
        if (providers != null) {
            LOG.debug(String.format("Got %d results for idpEntityIdentifiersByServiceEntityId", providers.size()));
        }
        return providers.stream().map(m -> (String) ((Map) m.get("data")).get("entityid")).toList();

    }

    @Override
    public Optional<Map<String, Object>> providerByEntityID(EntityType entityType, String entityID) {
        LOG.debug(String.format("providerByEntityID (%s) %s", entityType, entityID));

        String body = String.format("{\"data.entityid\":\"%s\"}", entityID);
        String manageUrl = String.format("%s/manage/api/internal/rawSearch/%s", url, entityType.collectionName());
        List<Map<String, Object>> providers = restTemplate.postForObject(manageUrl, body, List.class);
        List<Map<String, Object>> allProviders = transformProvider(providers);
        if (allProviders != null) {
            LOG.debug(String.format("Got %d results for providerByEntityID", allProviders.size()));
        }
        return allProviders.isEmpty() ? Optional.empty() : Optional.of(allProviders.get(0));
    }

    @Override
    public Map<String, Object> providerById(EntityType entityType, String id) {
        LOG.debug(String.format("providerById (%s) %s", entityType, id));

        String queryUrl = String.format("%s/manage/api/internal/metadata/%s/%s", url, entityType.collectionName(), id);
        return transformProvider(restTemplate.getForEntity(queryUrl, Map.class).getBody());
    }

    @Override
    public List<Map<String, Object>> provisioning(Collection<String> applicationIdentifiers) {
        LOG.debug("provisionings for identifiers");

        if (CollectionUtils.isEmpty(applicationIdentifiers)) {
            LOG.debug("No applicationIdentifiers in provisioning");
            return emptyList();
        }
        String queryUrl = String.format("%s/manage/api/internal/provisioning", url);
        return transformProvider(restTemplate.postForObject(queryUrl, applicationIdentifiers, List.class));
    }

    @Override
    public List<Map<String, Object>> providersAllowedByIdPs(List<Map<String, Object>> identityProviders) {
        LOG.debug("providersAllowedByIdPs");
        if (identityProviders.isEmpty()) {
            LOG.debug("No identityProviders in providersAllowedByIdPs");
            return emptyList();
        }
        if (identityProviders.stream()
                .anyMatch(idp -> (Boolean) idp.getOrDefault("allowedall", Boolean.FALSE))) {
            return this.providers(EntityType.SAML20_SP, EntityType.OIDC10_RP);
        }
        String split = identityProviders.stream()
                .map(idp -> (List<Map<String, String>>) idp.getOrDefault("allowedEntities", emptyList()))
                .flatMap(Collection::stream)
                .map(m -> "\"" + m.get("name") + "\"")
                .distinct()
                .collect(joining(","));

        String body = String.format("{\"data.entityid\":{\"$in\":[%s]}}", split);
        List<Map<String, Object>> results = new ArrayList<>();
        List.of(EntityType.SAML20_SP, EntityType.OIDC10_RP).forEach(entityType -> {
            String manageUrl = String.format("%s/manage/api/internal/rawSearch/%s", url,
                    entityType.collectionName());
            List<Map<String, Object>> providers = restTemplate.postForObject(manageUrl, body, List.class);
            List<Map<String, Object>> transformedProviders = transformProvider(providers);
            results.addAll(transformedProviders);
        });
        LOG.debug(String.format("Got %d results for providersAllowedByIdPs", results.size()));
        return results;
    }

    @Override
    public List<Map<String, Object>> providersAllowedByIdP(Map<String, Object> identityProvider) {
        LOG.debug("providersAllowedByIdP for : " + identityProvider.get("type"));

        return this.providersAllowedByIdPs(List.of(identityProvider));
    }

    @Override
    public List<Map<String, Object>> identityProvidersByInstitutionalGUID(String organisationGUID) {
        LOG.debug("identityProviderByInstitutionalGUID for : " + organisationGUID);

        Map<String, Object> baseQuery = getBaseQuery();
        baseQuery.put("metaDataFields.coin:institution_guid", organisationGUID);

        List<String> requestedAttributes = (List<String>) baseQuery.get("REQUESTED_ATTRIBUTES");
        requestedAttributes.add("allowedEntities");
        requestedAttributes.add("allowedall");
        requestedAttributes.remove("arp");

        List<Map<String, Object>> identityProviders = restTemplate.postForObject(
                String.format("%s/manage/api/internal/search/%s", this.url, EntityType.SAML20_IDP.collectionName()),
                baseQuery, List.class);
        if (identityProviders != null) {
            LOG.debug(String.format("Got %d results for identityProvidersByInstitutionalGUID", identityProviders.size()));
        }
        return transformProvider(identityProviders);
    }

    private List<Map<String, Object>> getRemoteMetaData(String type) {
        Map<String, Object> baseQuery = getBaseQuery();
        String url = String.format("%s/manage/api/internal/search/%s", this.url, type);
        return transformProvider(restTemplate.postForObject(url, baseQuery, List.class));
    }

    private Map<String, Object> getBaseQuery() {
        //Must be mutable, both the baseQuery as the requested attributes
        HashMap<String, Object> baseQuery = new HashMap<>();
        List<String> requestedAttributes = new ArrayList<>();
        requestedAttributes.add("arp");
        requestedAttributes.add("metaDataFields.logo:0:url");
        requestedAttributes.add("metaDataFields.coin:application_url");
        requestedAttributes.add("metaDataFields.coin:institution_guid");
        baseQuery.put("REQUESTED_ATTRIBUTES", requestedAttributes);
        return baseQuery;
    }


}
