package access.manage;

import access.model.Role;
import access.provision.ProvisioningType;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public interface Manage {

    List<Map<String, Object>> providers(EntityType... entityTypes);

    Map<String, Object> providerById(EntityType entityType, String id);

    Optional<Map<String, Object>> providerByEntityID(EntityType entityType, String entityID);

    List<Map<String, Object>> providersByIdIn(EntityType entityType, List<String> identifiers);

    List<Map<String, Object>> provisioning(List<String> ids);

    List<Map<String, Object>> providersByInstitutionalGUID(String organisationGUID);

    Optional<Map<String, Object>> identityProviderByInstitutionalGUID(String organisationGUID);

    default List<Map<String, Object>> transformProvider(List<Map<String, Object>> providers) {
        return providers.stream().map(this::transformProvider).toList();
    }

    @SuppressWarnings("unchecked")
    default Map<String, Object> transformProvider(Map<String, Object> provider) {
        //Defensive mostly because of tests
        if (CollectionUtils.isEmpty(provider)) {
            return provider;
        }
        Map data = (Map) provider.get("data");
        //When mocking - using the results of LocalManage - the provider may already be transformed
        if (CollectionUtils.isEmpty(data)) {
            return provider;
        }
        Map metaDataFields = (Map) data.get("metaDataFields");
        //Can't use Map.of as values can be null
        Map application = new HashMap<>();
        //Due to the different API's we are using, the result sometimes contains an "_id" and sometimes an "id"
        Object id = provider.get("id");
        if (id != null) {
            application.put("id", id);
            application.put("_id", id);
        } else {
            Object underscoreId = provider.get("_id");
            application.put("id", underscoreId);
            application.put("_id", underscoreId);
        }
        //This data won't leave the application, but is needed by the provisioning service
        if (provider.get("type").equals(EntityType.PROVISIONING.collectionName())) {
            application.put("provisioning_type", metaDataFields.get("provisioning_type"));
            application.put("scim_url", metaDataFields.get("scim_url"));
            application.put("scim_user", metaDataFields.get("scim_user"));
            application.put("scim_password", metaDataFields.get("scim_password"));
            application.put("scim_update_role_put_method", metaDataFields.getOrDefault("scim_update_role_put_method", false));
            application.put("eva_url", metaDataFields.get("eva_url"));
            application.put("eva_token", metaDataFields.get("eva_token"));
            application.put("eva_guest_account_duration", metaDataFields.getOrDefault("eva_guest_account_duration", 30));
            application.put("graph_url", metaDataFields.get("graph_url"));
            application.put("graph_client_id", metaDataFields.get("graph_client_id"));
            application.put("graph_secret", metaDataFields.get("graph_secret"));
            application.put("graph_tenant", metaDataFields.getOrDefault("graph_tenant", "common"));
        }
        application.put("type", provider.get("type"));
        application.put("applications", data.get("applications"));
        application.put("entityid", data.get("entityid"));
        application.put("logo", metaDataFields.get("logo:0:url"));
        application.put("OrganizationName:en", metaDataFields.get("OrganizationName:en"));
        application.put("OrganizationName:nl", metaDataFields.get("OrganizationName:nl"));
        application.put("name:en", metaDataFields.get("name:en"));
        application.put("name:nl", metaDataFields.get("name:nl"));
        application.put("institutionGuid", metaDataFields.get("coin:institution_guid"));
        return application;
    }

    default List<Role> deriveRemoteApplications(List<Role> roles) {
        //First get all unique remote manage entities and group them by entityType
        Map<EntityType, List<ManageIdentifier>> groupedManageIdentifiers = roles.stream()
                .map(role -> new ManageIdentifier(role.getManageId(), role.getManageType()))
                .collect(Collectors.toSet())
                .stream()
                .collect(Collectors.groupingBy(ManageIdentifier::entityType));
        //Now for each entityType (hopefully one, maximum two) we call manage and create a map with as key
        Map<String, Map<String, Object>> remoteApplications = groupedManageIdentifiers.entrySet().stream()
                .map(entry -> this.providersByIdIn(entry.getKey(), entry.getValue().stream().map(ManageIdentifier::id).toList()))
                .flatMap(List::stream)
                .collect(Collectors.toMap(map -> (String) map.get("id"), map -> map));
        //Add the metadata to the role
        roles.forEach(role -> role.setApplication(transformProvider(remoteApplications.get(role.getManageId()))));
        return roles;
    }

}
