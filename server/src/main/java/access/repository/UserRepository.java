package access.repository;

import access.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findBySubIgnoreCase(String sub);

    List<User> findByUserRoles_role_id(Long roleId);

    List<User> findByLastActivityBefore(Instant instant);
}
