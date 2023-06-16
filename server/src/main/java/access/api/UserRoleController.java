package access.api;

import access.config.Config;
import access.exception.NotFoundException;
import access.model.*;
import access.repository.RoleRepository;
import access.repository.UserRoleRepository;
import access.secuirty.UserPermissions;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static access.SwaggerOpenIdConfig.OPEN_ID_SCHEME_NAME;

@RestController
@RequestMapping(value = {"/api/v1/user_roles", "/api/external/v1/user_roles"}, produces = MediaType.APPLICATION_JSON_VALUE)
@Transactional
@SecurityRequirement(name = OPEN_ID_SCHEME_NAME, scopes = {"openid"})
@EnableConfigurationProperties(Config.class)
public class UserRoleController {
    private static final Log LOG = LogFactory.getLog(UserRoleController.class);

    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;

    public UserRoleController(UserRoleRepository userRoleRepository, RoleRepository roleRepository) {
        this.userRoleRepository = userRoleRepository;
        this.roleRepository = roleRepository;
    }

    @GetMapping("roles/{roleId}")
    public ResponseEntity<List<UserRole>> byRole(@PathVariable("roleId") Long roleId, @Parameter(hidden = true) User user) {
        LOG.debug("/me");
        Role role = roleRepository.findById(roleId).orElseThrow(NotFoundException::new);
        UserPermissions.assertRoleAccess(user, role, Authority.INVITER);
        List<UserRole> userRoles = userRoleRepository.findByRole(role);
        userRoles.forEach(userRole -> userRole.setUserInfo(userRole.getUser().asMap()));
        return ResponseEntity.ok(userRoles);
    }

}
