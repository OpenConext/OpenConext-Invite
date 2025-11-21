package invite.provision.scim;


import invite.model.Role;

import java.text.Normalizer;

public class GroupURN {

    private GroupURN() {
    }

    public static String sanitizeRoleShortName(String shortName) {
        String normalizedShortName = Normalizer.normalize(shortName, Normalizer.Form.NFKD)
                .trim()
                .replaceAll(" +", " ")
                .replaceAll(" ", "_")
                .replaceAll("[^A-Za-z0-9_./-]", "")
                .replaceAll("_+", "_")
                .toLowerCase();
        return normalizedShortName.endsWith("_") ? normalizedShortName.substring(0, normalizedShortName.length() - 1) : normalizedShortName;
    }

    public static String urnFromRole(String groupUrnPrefix, Role role) {
        return role.isTeamsOrigin() ? role.getUrn() : String.format("%s:%s:%s",
                groupUrnPrefix,
                role.getIdentifier(),
                role.getShortName());
    }

}
