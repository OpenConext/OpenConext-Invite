package invite.model;

import java.io.Serializable;

public record RoleExists(String shortName, String manageId, Long id) implements Serializable {
}
