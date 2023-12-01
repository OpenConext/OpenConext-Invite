package access.repository;

import access.model.Authority;
import access.model.Role;
import access.model.UserRole;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, Long> {

    List<UserRole> findByEndDateBefore(Instant instant);

    List<UserRole> findByEndDateBeforeAndExpiryNotifications(Instant instant, Integer expiryNotifications);

    List<UserRole> findByRole(Role role);

    List<UserRole> findByRoleName(String roleName);
}
