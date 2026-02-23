package invite.crm;

import java.util.List;

public record CrmConfigEntry(String code, String name, List<CrmManageIdentifier> crmManageIdentifiers) {
}
