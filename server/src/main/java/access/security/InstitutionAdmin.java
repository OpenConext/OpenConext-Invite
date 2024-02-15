package access.security;

import access.model.User;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.StringUtils;

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

    private InstitutionAdmin() {
    }

    public static boolean isInstitutionAdmin(Map<String, Object> attributes,
                                             String requiredEntitlement) {
        if (attributes.containsKey("eduperson_entitlement")) {
            List<String> entitlements = (List<String>) attributes.get("eduperson_entitlement");
            return entitlements.stream().anyMatch(entitlement -> entitlement.equalsIgnoreCase(requiredEntitlement));
        }
        return false;
    }

    public static boolean isInstitutionAdmin(User user) {
        return user.isInstitutionAdmin() &&
                StringUtils.hasText(user.getOrganizationGUID()) &&
                user.isInstitutionAdminByInvite();
    }

    public static Optional<String> getOrganizationGuid(Map<String, Object> attributes,
                                                       String organizationGuidPrefix,
                                                       Optional<User> optionalUser) {

        if (attributes.containsKey("eduperson_entitlement")) {
            List<String> entitlements = (List<String>) attributes.get("eduperson_entitlement");
            final String organizationGuidPrefixLower = organizationGuidPrefix.toLowerCase();
            Optional<String> optionalOrganizationGuid = entitlements.stream()
                    .filter(entitlement -> entitlement.toLowerCase().startsWith(organizationGuidPrefixLower))
                    .map(entitlement -> entitlement.substring(organizationGuidPrefix.length()))
                    .filter(StringUtils::hasText)
                    .findFirst();
            if (optionalOrganizationGuid.isPresent()) {
                return optionalOrganizationGuid.filter(StringUtils::hasText);
            }
        }
        return optionalUser.map(User::getOrganizationGUID)
                .filter(StringUtils::hasText);
    }

}