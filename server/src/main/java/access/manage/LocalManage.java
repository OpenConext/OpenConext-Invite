package access.manage;

import access.exception.NotFoundException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.springframework.core.io.ClassPathResource;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("unchecked")
public final class LocalManage implements Manage {

    private final Map<EntityType, List<Map<String, Object>>> allProviders;

    public LocalManage(ObjectMapper objectMapper) {
        this.allProviders = Stream.of(EntityType.values()).collect(Collectors.toMap(
                entityType -> entityType,
                entityType -> this.initialize(objectMapper, entityType)));
    }

    @SneakyThrows
    private List<Map<String, Object>> initialize(ObjectMapper objectMapper, EntityType entityType) {
        return objectMapper.readValue(new ClassPathResource("/manage/" + entityType.collectionName() + ".json").getInputStream(), new TypeReference<>() {
        });
    }

    @SneakyThrows
    @Override
    public List<Map<String, Object>> providers(EntityType entityType) {
        return addIdentifierAlias(this.allProviders.get(entityType));
    }

    @Override
    public List<Map<String, Object>> providersByIdIn(EntityType entityType, List<String> identifiers) {
        return addIdentifierAlias(this.allProviders.get(entityType).stream()
                .filter(provider -> identifiers.contains(provider.get("_id")))
                .collect(Collectors.toList()));
    }

    @Override
    public Map<String, Object> providerById(EntityType entityType, String id) {
        return addIdentifierAlias(providers(entityType).stream()
                .filter(provider -> provider.get("_id").equals(id))
                .findFirst()
                .orElseThrow(NotFoundException::new));
    }

    @Override
    public List<Map<String, Object>> provisioning(List<String> ids) {
        return addIdentifierAlias(providers(EntityType.PROVISIONING).stream()
                .filter(map -> {
                    List<Map<String, String>> applications = (List<Map<String, String>>) ((Map) map.get("data")).get("applications");
                    return applications.stream().anyMatch(m -> ids.contains(m.get("id")));
                })
                .collect(Collectors.toList()));
    }

    @Override
    public List<Map<String, Object>> allowedEntries(EntityType entityType, String id) {
        Map<String, Object> provider = providers(entityType).stream()
                .filter(prov -> prov.get("_id").equals(id))
                .findFirst()
                .orElseThrow(NotFoundException::new);
        String entityId = (String) ((Map) provider.get("data")).get("entityid");
        return addIdentifierAlias(providers(EntityType.SAML20_IDP).stream()
                .filter(prov -> {
                    Map data = (Map) prov.get("data");
                    return (boolean) data.get("allowedall") ||
                            ((List<Map<String, String>>) data.get("allowedEntities")).stream().anyMatch(entity -> entity.get("name").equals(entityId));
                })
                .collect(Collectors.toList()));
    }
}
