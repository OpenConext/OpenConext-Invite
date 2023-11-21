package access.api;

import access.manage.Manage;
import access.manage.ManageIdentifier;
import access.model.GroupedProviders;
import access.model.Role;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public interface HasManage {

    Manage getManage() ;

    default List<GroupedProviders> getGroupedProviders(List<Role> requestedRoles) {
        //We need to display the roles per manage application with the logo
        return requestedRoles.stream()
                .map(Role::getApplications)
                .flatMap(Collection::stream)
                .map(application -> new ManageIdentifier(application.getManageId(), application.getManageType()))
                .collect(Collectors.toSet())
                .stream()
                .map(manageIdentifier -> {
                    Map<String, Object> provider = getManage().providerById(manageIdentifier.entityType(), manageIdentifier.id());
                    String id = (String) provider.get("id");
                    return new GroupedProviders(
                            provider,
                            requestedRoles.stream().filter(role -> role.getApplications().stream()
                                    .anyMatch(application -> application.getManageId().equals(id))).toList(), UUID.randomUUID().toString());
                })
                .toList();

    }

}
