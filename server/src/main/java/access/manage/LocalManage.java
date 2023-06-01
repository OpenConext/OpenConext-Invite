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
    public List<Map<String, Object>> provisioning(String providerId) {
        return addIdentifierAlias(providers(EntityType.PROVISIONING).stream()
                .filter(map -> {
                    List<Map<String, String>> applications = (List<Map<String, String>>) ((Map) map.get("data")).get("applications");
                    return applications.stream().anyMatch(m -> m.get("id").equals(providerId));
                })
                .collect(Collectors.toList()));
    }

}
