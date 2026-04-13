package invite.crm;

import java.util.List;

public record CRMConfigEntry(String code, String name, List<CRMManageIdentifier> crmManageIdentifiers) {
}
