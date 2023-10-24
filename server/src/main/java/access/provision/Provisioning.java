package access.provision;

import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Getter
@SuppressWarnings("unchecked")
public class Provisioning {

    private final String id;
    private final String entityId;
    private final ProvisioningType provisioningType;
    private final String scimUrl;
    private final String scimUser;
    private final String scimPassword;
    private final String evaToken;
    private final boolean scimUpdateRolePutMethod;
    private final String evaUrl;
    private final int evaGuestAccountDuration;
    private final String graphUrl;
    private final String graphClientId;
    private final String graphSecret;
    private final String graphTenant;

    public Provisioning(Map<String, Object> provider) {
        this.id = (String) provider.get("id");

        this.entityId = (String) provider.get("entityid");

        this.provisioningType = ProvisioningType.valueOf((String) provider.get("provisioning_type"));
        this.scimUrl = (String) provider.get("scim_url");
        this.scimUser = (String) provider.get("scim_user");
        this.scimPassword = (String) provider.get("scim_password");
        this.scimUpdateRolePutMethod = (boolean) provider.getOrDefault("scim_update_role_put_method", false);
        this.evaUrl = (String) provider.get("eva_url");
        this.evaToken = (String) provider.get("eva_token");
        this.evaGuestAccountDuration = (int) provider.getOrDefault("eva_guest_account_duration", 30);
        this.graphUrl = (String) provider.get("graph_url");
        this.graphClientId = (String) provider.get("graph_client_id");
        this.graphSecret = (String) provider.get("graph_secret");
        this.graphTenant = (String) provider.getOrDefault("graph_tenant", "common");
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
                assert scimUser != null : "scimUser is null";
                assert scimPassword != null : "scimPassword is null";
            }
            case graph -> {
                assert graphClientId != null: "graphClientId is null";
                assert graphSecret != null: "graphSecret is null";
            }
        }
    }

    public boolean isApplicableForGroupRequest() {
        return ProvisioningType.scim.equals(this.provisioningType);

    }
}

