package access.repository;

import access.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findBySubIgnoreCase(String sub);

    List<User> findByUserRoles_role_id(Long roleId);

    List<User> findByLastActivityBefore(Instant instant);

    @Query(value = "SELECT * FROM users WHERE MATCH (given_name, family_name, email) against (?1  IN BOOLEAN MODE) " +
            "AND id > 0 LIMIT ?2",
            nativeQuery = true)
    List<User> search(String keyWord, int limit);
}