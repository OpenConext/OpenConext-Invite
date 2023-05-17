package access.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication;

import java.io.Serializable;
import java.time.Instant;

@Embeddable
@Getter
public class Auditable implements Serializable {

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "updated_by")
    private String updatedBy;

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
        createdBy = currentUser();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
        updatedBy = currentUser();
    }

    @JsonIgnore
    private String currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof BearerTokenAuthentication bearerTokenAuthentication) {
            return (String) bearerTokenAuthentication.getTokenAttributes().get("eduperson_principal_name");
        }
//        if (authentication instanceof )
        return authentication != null ? authentication.getName() : "ResourceCleaner";
    }

}
