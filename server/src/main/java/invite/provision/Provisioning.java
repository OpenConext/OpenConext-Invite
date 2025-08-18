package invite.provision;

import invite.manage.EntityType;
import invite.manage.ManageIdentifier;
import lombok.Getter;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;

@Getter
@SuppressWarnings("unchecked")
public class Provisioning {

    private final String id;
    private final String entityId;
    private final ProvisioningType provisioningType;
    private final String scimUrl;
    private final String scimUser;
    private final String scimPassword;
    private final String scimBearerToken;
    private final ScimUserIdentifier scimUserIdentifier;
    private final String evaToken;
    private final boolean scimUpdateRolePutMethod;
    private final boolean scimUserProvisioningOnly;
    private final String evaUrl;
    private final String graphUrl;
    private final String graphClientId;
    private final String graphSecret;
    private final String graphTenant;
    private final String institutionGUID;
    private final List<ManageIdentifier> remoteApplications;
    private final Integer userWaitTime;

    public Provisioning(Map<String, Object> provider) {
        this.id = (String) provider.get("id");

        this.entityId = (String) provider.get("entityid");

        this.provisioningType = ProvisioningType.valueOf((String) provider.get("provisioning_type"));
        this.scimUrl = (String) provider.get("scim_url");
        this.scimUser = (String) provider.get("scim_user");
        this.scimPassword = (String) provider.get("scim_password");
        this.scimBearerToken = (String) provider.get("scim_bearer_token");
        String optionalScimUserIdentifier = (String) provider.get("scim_user_identifier");
        this.scimUserIdentifier = StringUtils.hasText(optionalScimUserIdentifier) ? ScimUserIdentifier.valueOf(optionalScimUserIdentifier) :
                ScimUserIdentifier.eduperson_principal_name;
        Object updateRolePutMethod = provider.get("scim_update_role_put_method");
        this.scimUpdateRolePutMethod = updateRolePutMethod != null && (boolean) updateRolePutMethod;
        Object userProvisioningOnly = provider.get("scim_user_provisioning_only");
        this.scimUserProvisioningOnly = userProvisioningOnly != null && (boolean) userProvisioningOnly;
        this.evaUrl = (String) provider.get("eva_url");
        this.evaToken = (String) provider.get("eva_token");
        this.graphUrl = (String) provider.get("graph_url");
        this.graphClientId = (String) provider.get("graph_client_id");
        this.graphSecret = (String) provider.get("graph_secret");
        this.graphTenant = (String) provider.getOrDefault("graph_tenant", "common");
        this.institutionGUID = (String) provider.get("institutionGuid");
        this.userWaitTime = (Integer) provider.get("user_wait_time");
        List<Map<String, String>> applicationMaps = (List<Map<String, String>>) provider.getOrDefault("applications", emptyList());
        this.remoteApplications = applicationMaps.stream().map(m -> new ManageIdentifier(m.get("id"), EntityType.valueOf(m.get("type").toUpperCase()))).toList();
        this.invariant();
    }

    private void invariant() {
        switch (this.provisioningType) {
            case eva -> {
                assert evaUrl != null : "evaUrl is null";
                assert evaToken != null : "evaToken is null";
            }
            case scim -> {
                assert scimUrl != null : "scimUrl is null";
                if (scimBearerToken == null) {
                    assert scimUser != null: "scimUser is null";
                    assert scimPassword != null: "scimPassword or scimBearerToken is null";
                }
            }
            case graph -> {
                assert graphClientId != null : "graphClientId is null";
                assert graphSecret != null : "graphSecret is null";
            }
        }
        switch (this.scimUserIdentifier) {
            case eduID -> {
                assert institutionGUID != null : "institutionGUID is null";
            }
        }
    }

}

