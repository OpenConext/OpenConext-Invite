package access.manage;

import java.util.List;
import java.util.Map;

public interface Manage {

    List<Map<String, Object>> providers(EntityType entityType);

    Map<String, Object> providerById(EntityType entityType, String id);

    List<Map<String, Object>> providersByIdIn(EntityType entityType, List<String> identifiers);

    List<Map<String, Object>> provisioning(List<String> ids);

    List<Map<String, Object>> allowedEntries(EntityType entityType, String id);

    //Due to the different API's we are using, the result sometimes contains an "_id" and sometimes an "id"
    default List<Map<String, Object>> addIdentifierAlias(List<Map<String, Object>> providers) {
        providers.forEach(this::addIdentifierAlias);
        return providers;
    }

    default Map<String, Object> addIdentifierAlias(Map<String, Object> provider) {
        Object id = provider.get("id");
        if (id != null) {
            provider.put("_id", id);
        } else {
            provider.put("id", provider.get("_id"));
        }
        return provider;
    }

}
