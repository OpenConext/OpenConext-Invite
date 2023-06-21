package provisioning.config;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class InMemoryDatabase implements Database {

    private final Map<ProvisioningType, Map<String, Map<String, Object>>> users = new HashMap<>();
    private final Map<ProvisioningType, Map<String, Map<String, Object>>> groups = new HashMap<>();

    @Override
    public Map<String, Map<String, Object>> users(ProvisioningType provisioningType) {
        return users.computeIfAbsent(provisioningType, type -> new HashMap<>());
    }

    @Override
    public Map<String, Map<String, Object>> groups(ProvisioningType provisioningType) {
        return groups.computeIfAbsent(provisioningType, type -> new HashMap<>());
    }

    @Override
    public void clear(ProvisioningType provisioningType) {
        groups.getOrDefault(provisioningType, new HashMap<>()).clear();
        users.getOrDefault(provisioningType, new HashMap<>()).clear();
    }
}
