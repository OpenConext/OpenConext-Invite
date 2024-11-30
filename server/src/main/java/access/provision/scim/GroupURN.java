package access.provision.scim;


import access.model.Role;

import java.text.Normalizer;

public class GroupURN {

    private GroupURN() {
    }

    public static String sanitizeRoleShortName(String shortName) {
        return Normalizer.normalize(shortName, Normalizer.Form.NFKD)
                .trim()
                .replaceAll(" +", " ")
                .replaceAll(" ", "_")
                .replaceAll("[^A-Za-z0-9_.]", "")
                .replaceAll("_+", "_")
                .toLowerCase();
    }

    public static String urnFromRole(String groupUrnPrefix, Role role) {
        return role.isTeamsOrigin() ? role.getUrn() :String.format("%s:%s:%s",
                groupUrnPrefix,
                role.getIdentifier(),
                role.getShortName());
    }

}
