package access.repository;

import access.manage.ManageIdentifier;
import access.model.DistinctManageIdentifiers;
import access.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    @Query(value = "SELECT *, (SELECT COUNT(*) FROM user_roles ur WHERE ur.role_id=r.id) as userRoleCount " +
            "FROM roles r WHERE MATCH (name, description) against (?1  IN BOOLEAN MODE) AND id > 0 LIMIT ?2",
            nativeQuery = true)
    List<Role> search(String keyWord, int limit);


    @Query(value = "SELECT *, (SELECT COUNT(*) FROM user_roles ur WHERE ur.role_id=r.id) AS userRoleCount " +
            "FROM roles r WHERE JSON_CONTAINS(applications->'$[*].manageId', json_array(?1))",
            nativeQuery = true)
    List<Role> findByApplicationsManageId(String manageId);

    @Query(value = "SELECT DISTINCT applications FROM roles", nativeQuery = true)
    List<DistinctManageIdentifiers> doFindDistinctManageIdentifiers();

    @Query(value = "SELECT 1", nativeQuery = true)
    default Set<ManageIdentifier> findDistinctManageIdentifiers() {
        return this.doFindDistinctManageIdentifiers()
                .stream()
                .map(DistinctManageIdentifiers::manageIdentifiers)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }

    @Query(value = "SELECT *, (SELECT COUNT(*) FROM user_roles ur WHERE ur.role_id=r.id) as userRoleCount " +
            "FROM roles r WHERE JSON_CONTAINS(applications->'$[*].manageId', json_array(?1)) and short_name = ?2",
            nativeQuery = true)
    Optional<Role> findByShortNameIgnoreCaseAndApplicationsManageId(String managerId, String name);

    List<Role> findByName(String name);

}
