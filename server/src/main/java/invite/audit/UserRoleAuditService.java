package invite.audit;

import invite.model.UserRole;
import invite.model.UserRoleAudit;
import invite.repository.UserRoleAuditRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class UserRoleAuditService {

    private final UserRoleAuditRepository userRoleAuditRepository;

    public UserRoleAuditService(UserRoleAuditRepository userRoleAuditRepository) {
        this.userRoleAuditRepository = userRoleAuditRepository;
    }


    public void logAction(UserRole userRole, UserRoleAudit.ActionType action) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String authority = authentication != null ? authentication.getName() : "system";

        UserRoleAudit audit = new UserRoleAudit(action, userRole, authority);
        userRoleAuditRepository.save(audit);
    }
}
