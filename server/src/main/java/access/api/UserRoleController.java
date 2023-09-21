package access.api;

import access.config.Config;
import access.exception.NotAllowedException;
import access.exception.NotFoundException;
import access.logging.AccessLogger;
import access.logging.Event;
import access.model.*;
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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

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
    private final Config config;


    public UserRoleController(UserRoleRepository userRoleRepository, RoleRepository roleRepository, Config config) {
        this.userRoleRepository = userRoleRepository;
        this.roleRepository = roleRepository;
        this.config = config;
    }

    @GetMapping("roles/{roleId}")
    public ResponseEntity<List<UserRole>> byRole(@PathVariable("roleId") Long roleId,
                                                 @Parameter(hidden = true) User user) {
        LOG.debug("/me");
        Role role = roleRepository.findById(roleId).orElseThrow(NotFoundException::new);
        UserPermissions.assertRoleAccess(user, role, Authority.INVITER);
        List<UserRole> userRoles = userRoleRepository.findByRole(role);
        userRoles.forEach(userRole -> userRole.setUserInfo(userRole.getUser().asMap()));
        return ResponseEntity.ok(userRoles);
    }

    @PutMapping("")
    public ResponseEntity<Map<String, Integer>> updateUserRoleExpirationDate(@Validated @RequestBody UpdateUserRole updateUserRole,
                                                                             @Parameter(hidden = true) User user) {
        UserRole userRole = userRoleRepository.findById(updateUserRole.getUserRoleId()).orElseThrow(NotFoundException::new);
        if (updateUserRole.getEndDate() != null && !config.isPastDateAllowed() && Instant.now().isAfter(updateUserRole.getEndDate())) {
            throw new NotAllowedException("End date must be after now");
        }
        UserPermissions.assertValidInvitation(user, userRole.getAuthority(), List.of(userRole.getRole()));
        userRole.setEndDate(updateUserRole.getEndDate());
        userRoleRepository.save(userRole);
        return Results.createResult();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUserRole(@PathVariable("id") Long id, @Parameter(hidden = true) User user) {
        LOG.debug("/deleteUserRole");
        UserRole userRole = userRoleRepository.findById(id).orElseThrow(NotFoundException::new);
        Authority requiredAuthority = userRole.getAuthority().nextAuthorityInHierarchy();
        UserPermissions.assertValidInvitation(user, requiredAuthority, List.of(userRole.getRole()));
        userRoleRepository.delete(userRole);
        AccessLogger.userRole(LOG, Event.Deleted, user, userRole);
        return Results.deleteResult();
    }


}
