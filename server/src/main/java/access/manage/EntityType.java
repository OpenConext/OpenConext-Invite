package access.manage;

public enum EntityType {
    SAML20_SP, OIDC10_RP, PROVISIONING;

    public String getType() {
        return name().toLowerCase();
    }
}
