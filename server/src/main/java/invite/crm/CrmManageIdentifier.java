package invite.crm;


import invite.manage.EntityType;


public record CrmManageIdentifier(EntityType manageType, String manageEntityID) {
}