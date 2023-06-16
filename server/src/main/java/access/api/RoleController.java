package access.api;

import access.config.Config;
import access.exception.NotAllowedException;
import access.exception.NotFoundException;
import access.manage.Manage;
import access.model.*;
import access.repository.RoleRepository;
import access.provision.scim.GroupURN;
import access.secuirty.UserPermissions;
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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static access.SwaggerOpenIdConfig.OPEN_ID_SCHEME_NAME;

@RestController
@RequestMapping(value = {"/api/v1/roles", "/api/external/v1/roles"}, produces = MediaType.APPLICATION_JSON_VALUE)
@Transactional
@SecurityRequirement(name = OPEN_ID_SCHEME_NAME, scopes = {"openid"})
@EnableConfigurationProperties(Config.class)
public class RoleController {
    private static final Log LOG = LogFactory.getLog(RoleController.class);

    private final Config config;
    private final RoleRepository roleRepository;
    private final Manage manage;

    public RoleController(Config config, RoleRepository roleRepository, Manage manage) {
        this.config = config;
        this.roleRepository = roleRepository;
        this.manage = manage;
    }

    @GetMapping("")
    public ResponseEntity<List<Role>> rolesByApplication(@Parameter(hidden = true) User user) {
        LOG.debug("/roles");
        if (user.isSuperUser() && !config.isRoleSearchRequired()) {
            return ResponseEntity.ok(roleRepository.findAll());
        }
        UserPermissions.assertAuthority(user, Authority.MANAGER);
        Set<String> manageIdentifiers = user.getUserRoles().stream()
                .map(userRole -> userRole.getRole().getManageId())
                .collect(Collectors.toSet());
        return ResponseEntity.ok(roleRepository.findByManageIdIn(manageIdentifiers));
    }

    @GetMapping("mine")
    public ResponseEntity<List<Role>> myRoles(@Parameter(hidden = true) User user) {
        LOG.debug("/roles/mine");
        if (user.isSuperUser() && !config.isRoleSearchRequired()) {
            return ResponseEntity.ok(roleRepository.findAll());
        }
        if (user.getUserRoles().stream().anyMatch(userRole -> userRole.getAuthority().hasEqualOrHigherRights(Authority.MANAGER))) {
            return rolesByApplication(user) ;
        }
        return ResponseEntity.ok(user.getUserRoles().stream().map(UserRole::getRole).toList());
    }

    @GetMapping("{id}")
    public ResponseEntity<Role> role(@PathVariable("id") Long id, User user) {
        LOG.debug("/role");
        Role role = roleRepository.findById(id).orElseThrow(NotFoundException::new);
        Map<String, Object> provider = manage.providerById(role.getManageType(), role.getManageId());
        role.setApplication(provider);
        UserPermissions.assertRoleAccess(user, role, Authority.INVITER);
        return ResponseEntity.ok(role);
    }

    @GetMapping("search")
    public ResponseEntity<List<Role>> search(@RequestParam(value = "query") String query,
                                             @Parameter(hidden = true) User user) {
        LOG.debug("/search");
        UserPermissions.assertSuperUser(user);
        List<Role> roles = roleRepository.search(query + "*", 15);
        return ResponseEntity.ok(roles);
    }

    @PostMapping("validation/short_name")
    public ResponseEntity<Map<String, Boolean>> shortNameExists(@RequestBody RoleExists roleExists, @Parameter(hidden = true) User user) {
        UserPermissions.assertAuthority(user, Authority.MANAGER);
        String shortName = GroupURN.sanitizeRoleShortName(roleExists.shortName());
        Optional<Role> optionalRole = roleRepository.findByManageIdAndShortNameIgnoreCase(roleExists.manageId(), shortName);
        Map<String, Boolean> result = optionalRole
                .map(role -> Map.of("exists", roleExists.id() == null || !role.getId().equals(roleExists.id())))
                .orElse(Map.of("exists", false));
        return ResponseEntity.ok(result);
    }

    @PostMapping("")
    public ResponseEntity<Role> newRole(@Validated @RequestBody Role role, @Parameter(hidden = true) User user) {
        LOG.debug("/newRole");
        String shortName = GroupURN.sanitizeRoleShortName( role.getShortName());
        ResponseEntity<Map<String, Boolean>> exists = this.shortNameExists(new RoleExists(shortName, role.getManageId(), role.getId()), user);
        if (exists.getBody().get("exists")) {
            throw new NotAllowedException("Duplicate name: '" + shortName + "' for manage entity:'" + role.getManageId() + "'");
        }
        return saveOrUpdate(role, user);
    }

    @PutMapping("")
    public ResponseEntity<Role> updateRole(@Validated @RequestBody Role role, @Parameter(hidden = true) User user) {
        LOG.debug("/updateRole");
        return saveOrUpdate(role, user);
    }

    private ResponseEntity<Role> saveOrUpdate(@RequestBody @Validated Role role, @Parameter(hidden = true) User user) {
        Map<String, Object> provider = manage.providerById(role.getManageType(), role.getManageId());
        UserPermissions.assertManagerRole(provider, user);
        if (role.getId() != null) {
            role.setShortName(roleRepository.findById(role.getId()).orElseThrow(NotFoundException::new).getShortName());
        }
        return ResponseEntity.ok(roleRepository.save(role));
    }

}
