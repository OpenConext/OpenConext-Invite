package access.api;

import access.manage.Manage;
import access.manage.ManageIdentifier;
import access.model.GroupedProviders;
import access.model.Role;

import java.util.*;
import java.util.stream.Collectors;

import static access.security.InstitutionAdmin.*;

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
                    Map<String, Object> provider = getManage().providerById(manageIdentifier.manageType(), manageIdentifier.manageId());
                    String id = (String) provider.get("id");
                    return new GroupedProviders(
                            provider,
                            requestedRoles.stream().filter(role -> role.getApplications().stream()
                                    .anyMatch(application -> application.getManageId().equals(id))).toList(), UUID.randomUUID().toString());
                })
                .toList();
    }

    default Map<String, Object> enrichInstitutionAdmin(String organizationGUID) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(INSTITUTION_ADMIN, true);
        claims.put(ORGANIZATION_GUID, organizationGUID);
        List<Map<String, Object>> applications = getManage().providersByInstitutionalGUID(organizationGUID);
        claims.put(APPLICATIONS, applications);
        Optional<Map<String, Object>> identityProvider = getManage().identityProviderByInstitutionalGUID(organizationGUID);
        claims.put(INSTITUTION, identityProvider.orElse(null));
        return claims;
    }

}
