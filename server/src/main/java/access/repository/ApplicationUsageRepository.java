package access.repository;

import access.manage.EntityType;
import access.model.ApplicationUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ApplicationUsageRepository extends JpaRepository<ApplicationUsage, Long> {

    Optional<ApplicationUsage> findByRoleIdAndApplicationManageIdAndApplicationManageType(
            Long roleId, String manageId, EntityType manageType);

}
