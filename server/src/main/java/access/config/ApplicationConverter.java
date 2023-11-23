package access.config;

import access.manage.EntityType;
import access.model.Application;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import lombok.SneakyThrows;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ApplicationConverter implements AttributeConverter<Set<Application>, String> {

    private final static ObjectMapper objectMapper = new ObjectMapper();

    @SneakyThrows
    @Override
    public String convertToDatabaseColumn(Set<Application> attribute) {
        return objectMapper.writeValueAsString(attribute);
    }

    @SneakyThrows
    @Override
    @SuppressWarnings("unchecked")
    public Set<Application> convertToEntityAttribute(String dbData) {
        Set<Map<String, String>> set = objectMapper.readValue(dbData, Set.class);
        return set.stream().map(m -> new Application(m.get("manageId"), EntityType.valueOf(m.get("manageType"))))
                .collect(Collectors.toSet());
    }
}
