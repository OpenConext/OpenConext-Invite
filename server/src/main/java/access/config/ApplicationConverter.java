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

    @SneakyThrows
    @Override
    public String convertToDatabaseColumn(Set<Application> attribute) {
        return ObjectMapperHolder.objectMapper.writeValueAsString(attribute);
    }

    @SneakyThrows
    @Override
    @SuppressWarnings("unchecked")
    public Set<Application> convertToEntityAttribute(String dbData) {
        Set<Map<String, String>> applications =  ObjectMapperHolder.objectMapper.readValue(dbData, Set.class);
        return applications.stream()
                .map(m -> new Application(m.get("manageId"), EntityType.valueOf(m.get("manageType"))))
                .collect(Collectors.toSet());
    }
}
