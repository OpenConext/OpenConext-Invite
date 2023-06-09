package access.scim;

import lombok.Getter;

import java.util.Map;

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
    private final String provisioningMail;
    private final boolean scimUpdateRolePutMethod;

    public Provisioning(Map<String, Object> provider) {
        this.id = (String) provider.get("id");

        Map<String, Object> data = (Map<String, Object>) provider.get("data");
        this.entityId = (String) data.get("entityid");

        Map<String, Object> metaDataFields = (Map<String, Object>) data.get("metaDataFields");
        this.provisioningType = ProvisioningType.valueOf((String) metaDataFields.get("provisioning_type"));
        this.scimUrl = (String) metaDataFields.get("scim_url");
        this.scimUser = (String) metaDataFields.get("scim_user");
        this.scimPassword = (String) metaDataFields.get("scim_password");
        this.evaToken = (String) metaDataFields.get("eva_token");
        this.provisioningMail = (String) metaDataFields.get("provisioning_mail");
        this.scimUpdateRolePutMethod = (boolean) metaDataFields.getOrDefault("scimUpdateRolePutMethod", false);
        this.invariant();

    }

    private void invariant() {
        switch (this.provisioningType) {
            case eva -> {
                assert evaToken != null : "evaToken is null";
            }
            case scim -> {
                assert scimUrl != null : "scimUrl is null";
                assert scimUser != null : "scimUser is null";
                assert scimPassword != null : "scimPassword is null";
            }
            case mail -> {
                assert provisioningMail != null : "provisioningMail is null";
            }
        }
    }
}

