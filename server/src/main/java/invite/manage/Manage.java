package invite.manage;

import invite.model.GroupedProviders;
import invite.model.Role;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static invite.security.InstitutionAdmin.*;

public interface Manage {

    List<Map<String, Object>> providers(EntityType... entityTypes);

    Map<String, Object> providerById(EntityType entityType, String id);

    List<String> idpEntityIdentifiersByServiceEntityId(List<String> serviceEntityIdentifiers);

    Optional<Map<String, Object>> providerByEntityID(EntityType entityType, String entityID);

    List<Map<String, Object>> providersByIdIn(EntityType entityType, List<String> identifiers);

    List<Map<String, Object>> provisioning(Collection<String> applicationIdentifiers);

    List<Map<String, Object>> providersAllowedByIdP(Map<String, Object> identityProvider);

    List<Map<String, Object>> providersAllowedByIdPs(List<Map<String, Object>> identityProviders);

    List<Map<String, Object>> identityProvidersByInstitutionalGUID(String organisationGUID);

    default List<Map<String, Object>> transformProvider(List<Map<String, Object>> providers) {
        //Defensive because of Manage misbehavior
        if (CollectionUtils.isEmpty(providers)) {
            return new ArrayList<>();
        }
        return providers.stream().map(this::transformProvider).toList();
    }

    @SuppressWarnings("unchecked")
    default Map<String, Object> transformProvider(Map<String, Object> provider) {
        //Defensive mostly because of tests
        if (CollectionUtils.isEmpty(provider)) {
            return new HashMap<>();
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
            List.of(
                    "provisioning_type",
                    "scim_url",
                    "scim_user",
                    "scim_password",
                    "scim_bearer_token",
                    "scim_update_role_put_method",
                    "scim_user_identifier",
                    "scim_user_provisioning_only",
                    "eva_url",
                    "eva_token",
                    "eva_guest_account_duration",
                    "graph_url",
                    "graph_client_id",
                    "graph_secret",
                    "graph_tenant",
                    "user_wait_time"
            ).forEach(attribute -> application.put(attribute, metaDataFields.get(attribute)));
        }
        String scimUrl = (String) application.get("scim_url");
        if (StringUtils.hasText(scimUrl) && scimUrl.endsWith("/")) {
            application.put("scim_url", scimUrl.substring(0, scimUrl.length() - 1));
        }
        application.put("type", provider.get("type"));
        application.put("applications", data.get("applications"));
        application.put("allowedEntities", data.get("allowedEntities"));
        application.put("mfaEntities", data.get("mfaEntities"));
        application.put("allowedall", data.get("allowedall"));
        application.put("entityid", data.get("entityid"));
        application.put("logo", metaDataFields.get("logo:0:url"));
        application.put("url", metaDataFields.get("coin:application_url"));
        application.put("OrganizationName:en", metaDataFields.get("OrganizationName:en"));
        application.put("OrganizationName:nl", metaDataFields.get("OrganizationName:nl"));
        application.put("name:en", metaDataFields.get("name:en"));
        application.put("name:nl", metaDataFields.get("name:nl"));
        application.put("institutionGuid", metaDataFields.get("coin:institution_guid"));
        Map<String, Object> arp = (Map<String, Object>) data.get("arp");
        if (!CollectionUtils.isEmpty(arp)) {
            boolean enabled = (boolean) arp.getOrDefault("enabled", false);
            if (!enabled) {
                //Will receive all attributes, but not from any other source than idp
                application.put("receivesMemberships", false);
            } else {
                Map<String, Object> attributes = (Map<String, Object>) arp.getOrDefault("attributes", Map.of());
                List<Map<String, Object>> isMemberOf = (List<Map<String, Object>>) attributes.get("urn:mace:dir:attribute-def:isMemberOf");
                if (CollectionUtils.isEmpty(isMemberOf)) {
                    application.put("receivesMemberships", false);
                } else {
                    boolean receivesVootMemberships = isMemberOf.stream()
                            .map(m -> m.getOrDefault("source", "nope"))
                            .anyMatch(source -> source.equals("voot") || source.equals("invite"));
                    application.put("receivesMemberships", receivesVootMemberships);
                }
            }
        }
        return application;
    }

    default List<Role> addManageMetaData(List<Role> roles) {
        //First get all unique remote manage entities and group them by manageType
        Map<EntityType, List<ManageIdentifier>> groupedManageIdentifiers = roles.stream()
                .map(Role::applicationsUsed)
                .flatMap(Collection::stream)
                .map(application -> new ManageIdentifier(application.getManageId(), application.getManageType()))
                .collect(Collectors.toSet())
                .stream()
                .collect(Collectors.groupingBy(ManageIdentifier::manageType));
        //Now for each manageType (hopefully one, maximum two) we call manage and create a map with as key the manageId in manage
        Map<String, Map<String, Object>> remoteApplications = groupedManageIdentifiers.entrySet().stream()
                .map(entry -> this.providersByIdIn(entry.getKey(), entry.getValue().stream().map(ManageIdentifier::manageId).toList()))
                .flatMap(List::stream)
                .collect(Collectors.toMap(map -> (String) map.get("id"), map -> map));
        //Add the metadata to the role
        roles.forEach(role -> role.setApplicationMaps(
                role.getApplicationUsages().stream()
                        .map(applicationUsage -> {
                            Map<String, Object> applicationMap =
                                    transformProvider(remoteApplications.get(applicationUsage.getApplication().getManageId()));
                            if (CollectionUtils.isEmpty(applicationMap)) {
                                //If remote manage is not behaving
                                applicationMap = new HashMap<>();
                                applicationMap.put("unknown", true);
                            } else {
                                //Bugfix for overwrite of map reference value in case roles are linked to same application, need new Map
                                applicationMap = new HashMap<>(applicationMap);
                            }
                            applicationMap.put("landingPage", applicationUsage.getLandingPage());
                            return applicationMap;
                        })
                        .toList()));
        return roles;
    }

    default List<GroupedProviders> getGroupedProviders(List<Role> requestedRoles) {
        //We need to display the roles per manage application with the logo
        return requestedRoles.stream()
                .map(Role::applicationsUsed)
                .flatMap(Collection::stream)
                .map(application -> new ManageIdentifier(application.getManageId(), application.getManageType()))
                .collect(Collectors.toSet())
                .stream()
                .map(manageIdentifier -> providerById(manageIdentifier.manageType(), manageIdentifier.manageId()))
                .filter(provider -> !CollectionUtils.isEmpty(provider))
                .map(provider -> {
                    String id = (String) provider.get("id");
                    return new GroupedProviders(
                            provider,
                            requestedRoles.stream().filter(role -> role.applicationsUsed().stream()
                                    .anyMatch(application -> application.getManageId().equals(id))).toList(), UUID.randomUUID().toString());
                })
                .toList();
    }

    default Map<String, Object> enrichInstitutionAdmin(String organizationGUID, Map<String, Object> userClaims) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(INSTITUTION_ADMIN, true);
        claims.put(ORGANIZATION_GUID, organizationGUID);
        List<Map<String, Object>> identityProviders = identityProvidersByInstitutionalGUID(organizationGUID);
        if (identityProviders.size() > 1) {
            //try to find the IdP which entityID equals the authenticating authority of the user login
            String authenticatingAuthority = (String) userClaims.get("authenticating_authority");
            Map<String, Object> identityProvider = identityProviders.stream()
                    .filter(idp -> idp.get("entityid").equals(authenticatingAuthority))
                    .findFirst()
                    .orElse(identityProviders.getFirst());
            claims.put(INSTITUTION, identityProvider);
        } else {
            claims.put(INSTITUTION, identityProviders.isEmpty() ? null : identityProviders.getFirst());
        }
        List<Map<String, Object>> applications = this.providersAllowedByIdPs(identityProviders);
        claims.put(APPLICATIONS, applications);
        return claims;
    }


}
