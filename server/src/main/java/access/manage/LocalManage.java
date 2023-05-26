package access.manage;

import access.exception.NotFoundException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.springframework.core.io.ClassPathResource;

import java.util.List;
import java.util.Map;

public record LocalManage(ObjectMapper objectMapper) implements Manage {

    @SneakyThrows
    @Override
    public List<Map<String, Object>> providers(EntityType entityType) {
        return objectMapper.readValue(new ClassPathResource("/manage/" + entityType.getType() + ".json").getInputStream(), new TypeReference<>() {
        });
    }

    @Override
    public Map<String, Object> providerById(EntityType entityType, String id) {
        return providers(entityType).stream()
                .filter(provider -> provider.get("_id").equals(id))
                .findFirst()
                .orElseThrow(NotFoundException::new);
    }
}
