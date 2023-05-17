package access.model;

public enum Authority {

    SUPER_ADMIN(3), INSTITUTION_ADMINISTRATOR(2), INVITER(1), GUEST(0);

    private final int rights;

    Authority(int rights) {
        this.rights = rights;
    }

    public boolean hasEqualOrHigherRights(Authority requiredAuthority) {
        return rights >= requiredAuthority.rights;
    }

    public boolean hasHigherRights(Authority requiredAuthority) {
        return rights > requiredAuthority.rights;
    }

    public String friendlyName() {
        return this.name().replaceAll("_", " ").toLowerCase();
    }

    public int compareRights(Authority authority) {
        return this.rights - authority.rights;
    }
}
