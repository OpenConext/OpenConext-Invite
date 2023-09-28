package access.security;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Getter
@Setter
@SuppressWarnings("unchecked")
public class InstitutionAdmin {

    public static final String INSTITUTION_ADMIN = "INSTITUTION_ADMIN";
    public static final String ORGANIZATION_GUID = "ORGANIZATION_GUID";
    public static final String APPLICATIONS = "APPLICATIONS";
    public static final String INSTITUTION = "INSTITUTION";

    public static boolean isInstitutionAdmin(Map<String, Object> attributes, String requiredEntitlement) {
        if (attributes.containsKey("eduperson_entitlement")) {
            List<String> entitlements = (List<String>) attributes.get("eduperson_entitlement");
            return entitlements.stream().anyMatch(entitlement -> entitlement.equalsIgnoreCase(requiredEntitlement));
        }
        return false;
    }

    public static Optional<String> getOrganizationGuid(Map<String, Object> attributes, String organizationGuidPrefix) {
        if (attributes.containsKey("eduperson_entitlement")) {
            List<String> entitlements = (List<String>) attributes.get("eduperson_entitlement");
            final String organizationGuidPrefixLower = organizationGuidPrefix.toLowerCase();
            return entitlements.stream()
                    .filter(entitlement -> entitlement.toLowerCase().startsWith(organizationGuidPrefixLower))
                    .map(entitlement -> entitlement.substring(organizationGuidPrefix.length()))
                    .findFirst();
        }
        return Optional.empty();
    }
}