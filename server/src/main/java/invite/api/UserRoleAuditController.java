package invite.api;

import invite.config.Config;
import invite.exception.NotFoundException;
import invite.exception.UserRestrictionException;
import invite.model.Role;
import invite.model.User;
import invite.model.UserRoleAudit;
import invite.repository.RoleRepository;
import invite.repository.UserRoleAuditRepository;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

import static invite.SwaggerOpenIdConfig.API_TOKENS_SCHEME_NAME;
import static invite.SwaggerOpenIdConfig.OPEN_ID_SCHEME_NAME;
import static invite.security.InstitutionAdmin.isInstitutionAdmin;
import static invite.security.UserPermissions.assertInstitutionAdmin;

@RestController
@RequestMapping(value = {
        "/api/v1/user_roles_audit",
        "/api/external/v1/user_roles_audit"},
        produces = MediaType.APPLICATION_JSON_VALUE)
@Transactional
@SecurityRequirement(name = OPEN_ID_SCHEME_NAME, scopes = {"openid"})
@SecurityRequirement(name = API_TOKENS_SCHEME_NAME)
@EnableConfigurationProperties(Config.class)
public class UserRoleAuditController {

    private static final Log LOG = LogFactory.getLog(UserRoleAuditController.class);

    private final UserRoleAuditRepository userRoleAuditRepository;
    private final RoleRepository roleRepository;

    public UserRoleAuditController(UserRoleAuditRepository userRoleAuditRepository,
                                   RoleRepository roleRepository) {
        this.userRoleAuditRepository = userRoleAuditRepository;
        this.roleRepository = roleRepository;
    }

    @GetMapping("/search")
    @Transactional(readOnly = true)
    public ResponseEntity<Page<UserRoleAudit>> search(
            @Parameter(hidden = true) User user,
            @RequestParam(value = "query", required = false, defaultValue = "") String query,
            @RequestParam(value = "roleId", required = false, defaultValue = "") String roleIdString,
            @RequestParam(value = "pageNumber", required = false, defaultValue = "0") int pageNumber,
            @RequestParam(value = "pageSize", required = false, defaultValue = "10") int pageSize,
            @RequestParam(value = "sort", required = false, defaultValue = "userEmail") String sort,
            @RequestParam(value = "sortDirection", required = false, defaultValue = "ASC") String sortDirection) {

        LOG.debug(String.format("/search for user %s", user.getEduPersonPrincipalName()));

        assertInstitutionAdmin(user);

        Long roleId = parseRoleId(roleIdString);
        boolean isRoleIdPresent = roleId != null;

        if (isRoleIdPresent && user.isInstitutionAdmin()) {
            Role role = roleRepository.findById(roleId)
                    .orElseThrow(() -> new NotFoundException("Role not found: " + roleId));
            if (!user.getOrganizationGUID().equals(role.getOrganizationGUID())) {
                throw new UserRestrictionException(String.format("User %s has no access to role %s",
                        user.getEmail(), role.getName()));
            }
        }

        // Resolve the scope of accessible role IDs once, upfront, can bu NULL.
        List<Long> roleIds = resolveRoleIds(user, roleId, isRoleIdPresent);

        Pageable pageable = PageRequest.of(pageNumber, pageSize,
                Sort.by(Sort.Direction.fromString(sortDirection), sort));

        Page<UserRoleAudit> userRoleAudits;
        if (StringUtils.hasText(query)) {
            String likeQuery = "%" + query + "%";
            userRoleAudits = roleIds != null
                    ? userRoleAuditRepository.findAllByEmailLikeAndRoleIdIn(likeQuery, roleIds, pageable)
                    : userRoleAuditRepository.findAllByEmailLike(likeQuery, pageable);
        } else {
            userRoleAudits = roleIds != null
                    ? userRoleAuditRepository.findAllByRoleIdIn(roleIds, pageable)
                    : userRoleAuditRepository.findAll(pageable);
        }

        return ResponseEntity.ok(userRoleAudits);
    }

    @GetMapping("/roles")
    @Transactional(readOnly = true)
    public List<Map<String, Object>> fetchAllRoles(@Parameter(hidden = true) User user) {
        assertInstitutionAdmin(user);

        if (isInstitutionAdmin(user)) {
            return roleRepository.fetchAllRolesByOrganizanGUID(user.getOrganizationGUID());
        }
        return roleRepository.fetchAllRoles();
    }

    private List<Long> resolveRoleIds(User user, Long roleId, boolean isRoleIdPresent) {
        if (isRoleIdPresent) {
            return List.of(roleId);
        }
        if (!user.isSuperUser()) {
            return roleRepository.findByOrganizationGUID(user.getOrganizationGUID())
                    .stream()
                    .map(Role::getId)
                    .toList();
        }
        return null; // superuser with no roleId filter → unrestricted
    }

    private Long parseRoleId(String roleIdString) {
        try {
            return Long.parseLong(roleIdString);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
