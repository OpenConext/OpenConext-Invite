package access.model;

import access.config.ObjectMapperHolder;
import access.manage.ManageIdentifier;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.SneakyThrows;

import java.util.List;
import java.util.Set;

public interface DistinctManageIdentifiers {

    String getApplications();

    @SneakyThrows
    default List<ManageIdentifier> manageIdentifiers() {
        String applications = getApplications();
        return ObjectMapperHolder.objectMapper.readValue(applications, new TypeReference<>() {
        });
    }

}
