package access.api;

import access.config.Config;
import access.cron.ResourceCleaner;
import access.cron.RoleExpirationNotifier;
import access.exception.NotAllowedException;
import access.manage.Manage;
import access.model.Role;
import access.model.User;
import access.model.UserRole;
import access.repository.RoleRepository;
import access.repository.UserRoleRepository;
import access.security.UserPermissions;
import access.seed.PerformanceSeed;
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
    private final RoleExpirationNotifier roleExpirationNotifier;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final Manage manage;
    private final PerformanceSeed performanceSeed;
    private final Config config;

    public SystemController(ResourceCleaner resourceCleaner,
                            RoleExpirationNotifier roleExpirationNotifier,
                            RoleRepository roleRepository,
                            UserRoleRepository userRoleRepository,
                            Manage manage,
                            PerformanceSeed performanceSeed,
                            Config config) {
        this.resourceCleaner = resourceCleaner;
        this.roleExpirationNotifier = roleExpirationNotifier;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.manage = manage;
        this.performanceSeed = performanceSeed;
        this.config = config;
    }

    @GetMapping("/cron/cleanup")
    public ResponseEntity<Map<String, List<? extends Serializable>>> cronCleanup(@Parameter(hidden = true) User user) {
        LOG.debug("/cron/cleanup");
        UserPermissions.assertSuperUser(user);
        Map<String, List<? extends Serializable>> body = resourceCleaner.doClean();
        return ResponseEntity.ok(body);
    }

    @GetMapping("/cron/expiry-notifications")
    public ResponseEntity<Map<String, List<String>>> expiryNotifications(@Parameter(hidden = true) User user) {
        LOG.debug("/cron/expiry-notifications");
        UserPermissions.assertSuperUser(user);
        return ResponseEntity.ok(Map.of("mails", roleExpirationNotifier.doSweep()));
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

    @PutMapping("/performance-seed")
    public ResponseEntity<Map<String, Object>> performanceSeed(@Parameter(hidden = true) User user,
                                                               @RequestParam(value = "numberOfRole", required = false, defaultValue = "500") int numberOfRole,
                                                               @RequestParam(value = "numberOfUsers", required = false, defaultValue = "75000") int numberOfUsers) {
        LOG.debug("/performance-seed");
        if (!config.isPerformanceSeedAllowed()) {
            throw new NotAllowedException("performance-seed not allowed");
        }
        UserPermissions.assertSuperUser(user);
        return ResponseEntity.ok(performanceSeed.go(numberOfRole, numberOfUsers));
    }

}
