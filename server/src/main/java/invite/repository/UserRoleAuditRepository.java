package invite.repository;

import invite.model.UserRoleAudit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRoleAuditRepository extends JpaRepository<UserRoleAudit, Long> {

    @Query(
            value = "SELECT u FROM user_roles_audit u",
            countQuery = "SELECT COUNT(u) FROM user_roles_audit u"
    )
    Page<UserRoleAudit> findAll(Pageable pageable);

    @Query(
            value = "SELECT u FROM user_roles_audit u WHERE u.userEmail LIKE %:email%",
            countQuery = "SELECT COUNT(u) FROM user_roles_audit u WHERE u.userEmail LIKE %:email%"
    )
    Page<UserRoleAudit> findAllByEmailLike(@Param("email") String email, Pageable pageable);

    @Query(
            value = "SELECT u FROM user_roles_audit u WHERE u.userEmail LIKE %:email% AND u.roleId IN :roleIds",
            countQuery = "SELECT COUNT(u) FROM user_roles_audit u WHERE u.userEmail LIKE %:email% AND u.roleId IN :roleIds"
    )
    Page<UserRoleAudit> findAllByEmailLikeAndRoleIdIn(@Param("email") String email,
                                                      @Param("roleIds") List<Long> roleIds,
                                                      Pageable pageable);

    @Query(
            value = "SELECT u FROM user_roles_audit u WHERE u.roleId IN :roleIds",
            countQuery = "SELECT COUNT(u) FROM user_roles_audit u WHERE u.roleId IN :roleIds"
    )
    Page<UserRoleAudit> findAllByRoleIdIn(@Param("roleIds") List<Long> roleIds, Pageable pageable);
}
