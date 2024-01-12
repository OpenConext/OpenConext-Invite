package access.api;

import access.config.Config;
import access.cron.ResourceCleaner;
import access.manage.Manage;
import access.model.Role;
import access.model.User;
import access.model.UserRole;
import access.repository.RoleRepository;
import access.repository.UserRoleRepository;
import access.security.UserPermissions;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.io.Serializable;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static access.SwaggerOpenIdConfig.OPEN_ID_SCHEME_NAME;

@RestController
@RequestMapping(value = {"/api/v1/system", "/api/external/v1/system"}, produces = MediaType.APPLICATION_JSON_VALUE)
@Transactional
@SecurityRequirement(name = OPEN_ID_SCHEME_NAME, scopes = {"openid"})
@EnableConfigurationProperties(Config.class)
public class SystemController {

    private static final Log LOG = LogFactory.getLog(SystemController.class);

    private final ResourceCleaner resourceCleaner;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final Manage manage;

    public SystemController(ResourceCleaner resourceCleaner,
                            RoleRepository roleRepository,
                            UserRoleRepository userRoleRepository,
                            Manage manage) {
        this.resourceCleaner = resourceCleaner;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.manage = manage;
    }

    @GetMapping("/cron/cleanup")
    public ResponseEntity<Map<String, List<? extends Serializable>>> cronCleanup(@Parameter(hidden = true) User user) {
        LOG.debug("/cron/cleanup");
        UserPermissions.assertSuperUser(user);
        Map<String, List<? extends Serializable>> body = resourceCleaner.doClean();
        return ResponseEntity.ok(body);
    }

    @GetMapping("/expiry-user-roles")
    public ResponseEntity<List<UserRole>> expiryUserRoles(@Parameter(hidden = true) User user) {
        LOG.debug("/cron/notifications");
        UserPermissions.assertSuperUser(user);
        Instant instant = Instant.now().plus(30, ChronoUnit.DAYS);
        List<UserRole> userRoles = userRoleRepository.findByEndDateBefore(instant);
        userRoles.forEach(userRole -> userRole.setUserInfo(userRole.getUser().asMap()));
        return ResponseEntity.ok(userRoles);
    }

    @GetMapping("/unknown-roles")
    public ResponseEntity<List<Role>> unknownRoles(@Parameter(hidden = true) User user) {
        LOG.debug("/unknown-roles");
        UserPermissions.assertSuperUser(user);
        List<Role> roles = manage.addManageMetaData(roleRepository.findAll());
        List<Role> unknownManageRoles = roles.stream().filter(role -> role.getApplicationMaps().stream().anyMatch(applicationMap -> applicationMap.containsKey("unknown"))).toList();
        return ResponseEntity.ok(unknownManageRoles);
    }

}
