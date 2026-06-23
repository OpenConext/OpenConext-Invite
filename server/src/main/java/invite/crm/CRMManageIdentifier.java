package invite.crm;


import invite.manage.EntityType;

import java.util.Map;


public record CRMManageIdentifier(EntityType manageType, String manageEntityID) {

    public Map<String, String> toProvider() {
        return Map.of(
                "manageType", manageType.collectionName(),
                "manageEntityID", manageEntityID
        );
    }

}