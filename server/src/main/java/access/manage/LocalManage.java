package access.manage;

import access.exception.NotFoundException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;

@SuppressWarnings("unchecked")
public final class LocalManage implements Manage {

    private static final Log LOG = LogFactory.getLog(LocalManage.class);

    private final Map<EntityType, List<Map<String, Object>>> allProviders;
    private final DefaultResourceLoader defaultResourceLoader = new DefaultResourceLoader();

    public LocalManage(ObjectMapper objectMapper) {
        this(objectMapper, "classpath:/manage");
    }

    public LocalManage(ObjectMapper objectMapper, String staticManageDirectory) {
        this.allProviders = Stream.of(EntityType.values()).collect(Collectors.toMap(
                entityType -> entityType,
                entityType -> this.initialize(objectMapper, entityType, staticManageDirectory)));
    }

    @SneakyThrows
    private List<Map<String, Object>> initialize(ObjectMapper objectMapper, EntityType entityType, String staticManageDirectory) {
        String resourceName = String.format("%s/%s.json", staticManageDirectory, entityType.collectionName());
        Resource resource = defaultResourceLoader.getResource(resourceName);
        return objectMapper.readValue(resource.getInputStream(), new TypeReference<>() {
        });
    }

    @Override
    public List<Map<String, Object>> providers(EntityType... entityTypes) {
        LOG.debug("providers for : " + List.of(entityTypes));

        //Ensure it is immutable
        return transformProvider(Stream.of(entityTypes).map(entityType -> this.allProviders.get(entityType).stream().toList())
                .flatMap(List::stream)
                .toList());
    }

    @Override
    public List<Map<String, Object>> providersByIdIn(EntityType entityType, List<String> identifiers) {
        LOG.debug("providersByIdIn for : " + entityType);

        List<Map<String, Object>> providers = this.allProviders.get(entityType);
        return transformProvider(providers.stream()
                .filter(provider -> identifiers.contains(provider.get("_id")))
                .collect(Collectors.toList()));
    }

    @Override
    public Optional<Map<String, Object>> providerByEntityID(EntityType entityType, String entityID) {
        LOG.debug("providerByEntityID for : " + entityType);

        return this.allProviders.get(entityType).stream()
                .filter(provider -> entityID.equals(((Map<String, Object>) provider.get("data")).get("entityid")))
                .map(this::transformProvider)
                .findFirst();
    }

    @Override
    public Map<String, Object> providerById(EntityType entityType, String id) {
        LOG.debug("providerById for : " + entityType);

        List<Map<String, Object>> providers = providers(entityType);
        return providers.stream()
                .filter(provider -> provider.get("_id").equals(id))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Provider not found"));
    }

    @Override
    public List<Map<String, Object>> provisioning(Collection<String> ids) {
        LOG.debug("provisioning for : " + ids);

        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyList();
        }
        return providers(EntityType.PROVISIONING).stream()
                .filter(map -> {
                    List<Map<String, String>> applications = (List<Map<String, String>>) map.get("applications");
                    return applications.stream().anyMatch(m -> ids.contains(m.get("id")));
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> providersAllowedByIdP(Map<String, Object> identityProvider) {
        LOG.debug("providersAllowedByIdP for : " + identityProvider.get("type"));

        Boolean allowedAll = (Boolean) identityProvider.getOrDefault("allowedall", Boolean.FALSE);
        List<Map<String, Object>> allProviders = this.providers(EntityType.SAML20_SP, EntityType.OIDC10_RP);
        if (allowedAll) {
            return allProviders;
        }
        List<Map<String, String>> allowedEntities = (List<Map<String, String>>) identityProvider.getOrDefault("allowedEntities", emptyList());
        List<String> entityIdentifiers = allowedEntities.stream().map(m -> m.get("name")).toList();
        return allProviders.stream().filter(provider -> entityIdentifiers.contains((String) provider.get("entityid"))).toList();
    }

    @Override
    public Optional<Map<String, Object>> identityProviderByInstitutionalGUID(String organisationGUID) {
        LOG.debug("identityProviderByInstitutionalGUID for : " + organisationGUID);

        List<Map<String, Object>> providers = providers(EntityType.SAML20_IDP);
        return providers
                .stream()
                .filter(provider -> Objects.equals(provider.get("institutionGuid"), organisationGUID))
                .findAny();
    }
}
