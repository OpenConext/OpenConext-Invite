package access.repository;

import access.model.UserRole;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, Long> {

    List<UserRole> findByEndDateBefore(Instant instant);

    @EntityGraph(value = "findByRoleId", type = EntityGraph.EntityGraphType.LOAD,
            attributePaths = {"role.application"})
    List<UserRole> findByRoleId(Long roleId);


}
