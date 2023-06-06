package access.model;

import java.util.List;
import java.util.Map;

public record MetaInvitation(Invitation invitation, List<Map<String, Object>> providers) {
}
