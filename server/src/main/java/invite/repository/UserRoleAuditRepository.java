package invite.repository;

import invite.model.UserRoleAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRoleAuditRepository extends JpaRepository<UserRoleAudit, Long> {

}
