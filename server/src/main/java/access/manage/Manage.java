package access.manage;

import access.model.Role;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

public interface Manage {

    List<Map<String, Object>> providers(EntityType entityType);

    Map<String, Object> providerById(EntityType entityType, String id);

    Optional<Map<String, Object>> providerByEntityID(EntityType entityType, String entityID);

    List<Map<String, Object>> providersByIdIn(EntityType entityType, List<String> identifiers);

    List<Map<String, Object>> provisioning(List<String> ids);

    List<Map<String, Object>> allowedEntries(EntityType entityType, String id);

    //Due to the different API's we are using, the result sometimes contains an "_id" and sometimes an "id"
    default List<Map<String, Object>> addIdentifierAlias(List<Map<String, Object>> providers) {
        providers.forEach(this::addIdentifierAlias);
        return providers;
    }

    default Map<String, Object> addIdentifierAlias(Map<String, Object> provider) {
        //Defensive mostly because of tests
        if (CollectionUtils.isEmpty(provider)) {
            return provider;
        }
        Object id = provider.get("id");
        if (id != null) {
            provider.put("_id", id);
        } else {
            provider.put("id", provider.get("_id"));
        }
        return provider;
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
        roles.forEach(role -> role.setApplication(addIdentifierAlias(remoteApplications.get(role.getManageId()))));
        return roles;
    }

}
