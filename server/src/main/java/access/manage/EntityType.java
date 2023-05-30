package access.manage;

public enum EntityType {
    IDP("saml20_idp"),
    SP("saml20_sp"),
    RP("oidc10_rp"),
    PROV("provisioning");

    private final String type;

    EntityType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
