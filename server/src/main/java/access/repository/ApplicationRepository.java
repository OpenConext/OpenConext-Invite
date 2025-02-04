package access.repository;

import access.manage.EntityType;
import access.model.Application;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {

    Optional<Application> findByManageIdAndManageType(String manageId, EntityType manageType);

    @Query(value = """
            SELECT count(r.id) as role_count, apps.manage_id as manage_id FROM roles r
            INNER JOIN application_usages au ON au.role_id = r.id
            INNER JOIN applications apps ON apps.id = au.application_id
            GROUP BY apps.manage_id
            """,
            nativeQuery = true)
    List<Map<String, Object>> countByApplications();

}
