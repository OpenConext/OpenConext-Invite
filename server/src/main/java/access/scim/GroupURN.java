package access.scim;


import access.model.Role;

import java.text.Normalizer;

public class GroupURN {

    private GroupURN() {
    }

    private static String sanitizeRoleName(String name) {
        return Normalizer.normalize(name, Normalizer.Form.NFKD)
                .trim()
                .replaceAll(" ", "_")
                .replaceAll("[^A-Za-z0-9_.]", "")
                .toLowerCase();
    }

    public static String urnFromRole(String groupUrnPrefix, Role role) {
        return String.format("%s:%s:%s",
                groupUrnPrefix,
                role.getManageId(),
                sanitizeRoleName(role.getName()));
    }

}
