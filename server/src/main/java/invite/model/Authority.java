package invite.model;

import java.util.Map;

public enum Authority {

    SUPER_USER(4), INSTITUTION_ADMIN(3), MANAGER(2), INVITER(1), GUEST(0);

    private Map<String, Map<String, String>> translations = Map.of(
            "SUPER_USER", Map.of("en", "Super user", "nl", "Super user"),
            "INSTITUTION_ADMIN", Map.of("en", "Institution admin", "nl", "Instellings-admin"),
            "MANAGER", Map.of("en", "Manager", "nl", "Beheerder"),
            "INVITER", Map.of("en", "Inviter", "nl", "Uitnodiger"),
            "GUEST", Map.of("en", "Guest", "nl", "Gast")
    );

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

    public String translate(String lang) {
        return translations.get(this.name()).get(lang);
    }
}
