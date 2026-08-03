package invite.manage;

public enum EntityType {

    SAML20_SP, OIDC10_RP, SAML20_IDP, PROVISIONING, POLICY;

    public String collectionName() {
        return name().toLowerCase();
    }
}
