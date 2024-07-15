package access.model;

import lombok.Getter;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Getter
public class UserRoles {

    private final Long id;
    private final String name;
    private final String email;
    private final String schacHomeOrganization;
    private final Instant createdAt;
    private final Instant lastActivity;
    private final List<RoleSummary> roleSummaries;

    public UserRoles(List<Map<String, Object>> roles) {
        assert !roles.isEmpty();
        Map<String, Object> map = roles.get(0);
        this.id = (Long) map.get("id");
        this.name = (String) map.get("name");
        this.email = (String) map.get("email");
        this.schacHomeOrganization = (String) map.get("schac_home_organization");
        this.createdAt = timeStampToInstant(map, "created_at");
        this.lastActivity = timeStampToInstant(map, "last_activity");
        this.roleSummaries = roles.stream()
                .map(summary -> new RoleSummary(
                        (Long) summary.get("role_id"),
                        (String) summary.get("role_name"),
                        (String) summary.get("authority"),
                        timeStampToInstant(summary, "end_date"))
                ).toList();
    }

    private Instant timeStampToInstant(Map<String, Object> map, String key) {
        Timestamp timeStamp = (Timestamp) map.get(key);
        return timeStamp != null ? timeStamp.toInstant() : null;
    }
}
