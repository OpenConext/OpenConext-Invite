package invite.crm;

import invite.manage.ManageIdentifier;

import java.util.List;

public record CrmConfigEntry(String code, String name, List<CrmManageIdentifier> crmManageIdentifiers) {
}
