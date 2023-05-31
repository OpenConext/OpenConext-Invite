package access.model;

public enum Authority {

    SUPER_USER(3), MANAGER(2), INVITER(1), GUEST(0);

    private final int rights;

    Authority(int rights) {
        this.rights = rights;
    }

    public boolean hasEqualOrHigherRights(Authority requiredAuthority) {
        return rights >= requiredAuthority.rights;
    }

}
