package access.manage;

import access.exception.NotFoundException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("unchecked")
public final class LocalManage implements Manage {

    private final Map<EntityType, List<Map<String, Object>>> allProviders;
    private final boolean local;

    public LocalManage(ObjectMapper objectMapper, boolean local) {
        this.local = local;
        this.allProviders = Stream.of(EntityType.values()).collect(Collectors.toMap(
                entityType -> entityType,
                entityType -> this.initialize(objectMapper, entityType)));
    }

    @SneakyThrows
    private List<Map<String, Object>> initialize(ObjectMapper objectMapper, EntityType entityType) {
        String collectionName = entityType.collectionName();
        if (this.local && collectionName.equals(EntityType.PROVISIONING.collectionName())) {
            collectionName += ".local";
        }
        return objectMapper.readValue(new ClassPathResource("/manage/" + collectionName + ".json").getInputStream(), new TypeReference<>() {
        });
    }

    @Override
    public List<Map<String, Object>> providers(EntityType... entityTypes) {
        //Ensure it is immutable
        return transformProvider(Stream.of(entityTypes).map(entityType -> this.allProviders.get(entityType).stream().toList())
                .flatMap(List::stream)
                .toList());
    }

    @Override
    public List<Map<String, Object>> providersByIdIn(EntityType entityType, List<String> identifiers) {
        List<Map<String, Object>> providers = this.allProviders.get(entityType);
        return transformProvider(providers.stream()
                .filter(provider -> identifiers.contains(provider.get("_id")))
                .collect(Collectors.toList()));
    }

    @Override
    public Optional<Map<String, Object>> providerByEntityID(EntityType entityType, String entityID) {
        return this.allProviders.get(entityType).stream()
                .filter(provider -> entityID.equals(((Map<String, Object>) provider.get("data")).get("entityid")))
                .map(this::transformProvider)
                .findFirst();
    }

    @Override
    public Map<String, Object> providerById(EntityType entityType, String id) {
        List<Map<String, Object>> providers = providers(entityType);
        return providers.stream()
                .filter(provider -> provider.get("_id").equals(id))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Provider not found"));
    }

    @Override
    public List<Map<String, Object>> provisioning(Collection<String> ids) {
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
    public List<Map<String, Object>> providersByInstitutionalGUID(String organisationGUID) {
        List<Map<String, Object>> providers = providers(EntityType.SAML20_SP, EntityType.OIDC10_RP);
        return providers
                .stream()
                .filter(provider -> Objects.equals(provider.get("institutionGuid"), organisationGUID))
                .toList();
    }

    @Override
    public Optional<Map<String, Object>> identityProviderByInstitutionalGUID(String organisationGUID) {
        List<Map<String, Object>> providers = providers(EntityType.SAML20_IDP);
        return providers
                .stream()
                .filter(provider -> Objects.equals(provider.get("institutionGuid"), organisationGUID))
                .findAny();
    }
}
