package access.manage;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.springframework.core.io.ClassPathResource;

import java.util.Map;

public record LocalManage(ObjectMapper objectMapper) implements Manage {

    @SneakyThrows
    @Override
    public Map<String, Object> serviceProviders() {
        return objectMapper.readValue(new ClassPathResource("/manage/service_providers.json").getInputStream(), new TypeReference<>() {
        });
    }

    @SneakyThrows
    @Override
    public Map<String, Object> identityProviders() {
        return objectMapper.readValue(new ClassPathResource("/manage/identity_providers.json").getInputStream(), new TypeReference<>() {
        });
    }
}
