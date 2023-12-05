package access.repository;

import access.manage.EntityType;
import access.model.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {

    Optional<Application> findByManageIdAndManageType(String manageId, EntityType manageType);
}
