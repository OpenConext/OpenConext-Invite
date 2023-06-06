package access.model;

import java.io.Serializable;

public record RoleExists(String name, String manageId, Long id) implements Serializable {
}
