package access.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

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
    @Setter
    private List<RoleSummary> roleSummaries;

    public UserRoles(Map<String, Object> userMap) {
        this.id = (Long) userMap.get("id");
        this.name = (String) userMap.get("name");
        this.email = (String) userMap.get("email");
        this.schacHomeOrganization = (String) userMap.get("schac_home_organization");
        this.createdAt = timeStampToInstant(userMap, "created_at");
        this.lastActivity = timeStampToInstant(userMap, "last_activity");
    }

    @JsonIgnore
    public static Instant timeStampToInstant(Map<String, Object> map, String key) {
        Timestamp timeStamp = (Timestamp) map.get(key);
        return timeStamp != null ? timeStamp.toInstant() : null;
    }
}
