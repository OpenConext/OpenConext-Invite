package access.model;


import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.validation.constraints.NotNull;

public class NameHolderListener {

    @PrePersist
    @PreUpdate
    public void beforeSave(@NotNull NameHolder nameHolder) {
        nameHolder.nameUrnCompatibilityCheck();
    }
}
