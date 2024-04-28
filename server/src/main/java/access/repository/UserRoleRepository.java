package access.repository;

import access.model.Authority;
import access.model.Role;
import access.model.UserRole;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

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

    @Modifying
    @Query("DELETE FROM user_roles WHERE id = ?1")
    @Transactional(isolation = Isolation.SERIALIZABLE)
    void deleteUserRoleById(Long id);
}
