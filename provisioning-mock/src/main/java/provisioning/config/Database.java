package provisioning.config;

import java.util.Map;

public interface Database {

    Map<String, Map<String, Object>> users(ProvisioningType provisioningType) ;

    Map<String, Map<String, Object>> groups(ProvisioningType provisioningType) ;

    void clear(ProvisioningType provisioningType);
}
