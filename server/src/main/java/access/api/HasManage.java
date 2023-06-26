package access.api;

import access.manage.Manage;
import access.manage.ManageIdentifier;
import access.model.GroupedProviders;
import access.model.Role;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public interface HasManage {

    Manage getManage() ;

    default List<GroupedProviders> getGroupedProviders(List<Role> requestedRoles) {
        //We need to display the roles per manage application with the logo
        List<GroupedProviders> groupedProviders = requestedRoles.stream()
                .collect(Collectors.groupingBy(role -> new ManageIdentifier(role.getManageId(), role.getManageType())))
                .entrySet().stream()
                .map(entry -> new GroupedProviders(
                        getManage().providerById(entry.getKey().entityType(), entry.getKey().id()),
                        entry.getValue(),
                        UUID.randomUUID().toString())
                ).toList();
        return groupedProviders;
    }

}
