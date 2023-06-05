package access.manage;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;


public record Identity(String id, EntityType entityType) {
}