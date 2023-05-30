package access.manage;

import java.util.List;
import java.util.Map;

public interface Manage {

    List<Map<String, Object>> providers(EntityType entityType);

    Map<String, Object> providerById(EntityType entityType, String id);

    List<Map<String, Object>> provisioning(String providerId);

}
