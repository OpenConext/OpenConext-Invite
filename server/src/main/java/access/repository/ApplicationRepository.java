package access.repository;

import access.model.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {

    Optional<Application> findByManageIdIgnoreCase(String manageId);

    @Query(
            value = "select count(*) from users u inner join user_roles ur on ur.user_id = u.id " +
                    "inner join roles r on r.id = ur.role_id inner join applications a on a.id = r.application_id " +
                    "where a.id = ?1",
            nativeQuery = true
    )
    Long countUsers(Long applicationId);

}
