package access.api;

import access.exception.NotAllowedException;
import access.exception.NotFoundException;
import access.manage.Manage;
import access.model.Authority;
import access.model.Role;
import access.model.RoleExists;
import access.model.User;
import access.repository.RoleRepository;
import access.secuirty.UserPermissions;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
public class RoleController {
    private static final Log LOG = LogFactory.getLog(RoleController.class);

    private final RoleRepository roleRepository;
    private final Manage manage;

    public RoleController(RoleRepository roleRepository, Manage manage) {
        this.roleRepository = roleRepository;
        this.manage = manage;
    }

    @GetMapping("")
    public ResponseEntity<List<Role>> rolesByApplication(@Parameter(hidden = true) User user) {
        LOG.debug("/role");
        UserPermissions.assertAuthority(user, Authority.MANAGER);
        Set<String> manageIdentifiers = user.getUserRoles().stream()
                .map(userRole -> userRole.getRole().getManageId())
                .collect(Collectors.toSet());
        return ResponseEntity.ok(roleRepository.findByManageIdIn(manageIdentifiers));
    }

    @GetMapping("{id}")
    public ResponseEntity<Role> role(@PathVariable("id") Long id, User user) {
        LOG.debug("/role");
        return ResponseEntity.ok(roleRepository.findById(id).orElseThrow(NotFoundException::new));
    }

    @GetMapping("search")
    public ResponseEntity<List<Role>> search(@RequestParam(value = "query") String query,
                                             @Parameter(hidden = true) User user) {
        LOG.debug("/search");
        UserPermissions.assertSuperUser(user);
        List<Role> roles = roleRepository.search(query + "*", 15);
        return ResponseEntity.ok(roles);
    }

    @PostMapping("validation/name")
    public ResponseEntity<Map<String, Boolean>> nameExists(@RequestBody RoleExists roleExists, @Parameter(hidden = true) User user) {
        UserPermissions.assertAuthority(user, Authority.MANAGER);

        String roleName = roleExists.name().trim().replaceAll(" +", " ");
        Optional<Role> optionalRole = roleRepository.findByManageIdAndNameIgnoreCase(roleExists.manageId(), roleName);
        Map<String, Boolean> result = optionalRole
                .map(role -> Map.of("exists", roleExists.id() == null || role.getId().equals(roleExists.id())))
                .orElse(Map.of("exists", false));
        return ResponseEntity.ok(result);
    }

    @PostMapping("")
    public ResponseEntity<Role> newRole(@Validated @RequestBody Role role, @Parameter(hidden = true) User user) {
        LOG.debug("/newRole");
        return saveOrUpdate(role, user);
    }

    @PutMapping("")
    public ResponseEntity<Role> updateRole(@Validated @RequestBody Role role, @Parameter(hidden = true) User user) {
        LOG.debug("/updateRole");
        return saveOrUpdate(role, user);
    }

    private ResponseEntity<Role> saveOrUpdate(@RequestBody @Validated Role role, @Parameter(hidden = true) User user) {
        ResponseEntity<Map<String, Boolean>> exists = this.nameExists(new RoleExists(role.getName(), role.getManageId(), role.getId()), user);
        if (exists.getBody().get("exists")) {
            throw new NotAllowedException("Duplicate name: '" + role.getName() + "' for manage entity:'" + role.getManageId() + "'");
        }
        Map<String, Object> provider = manage.providerById(role.getManageType(), role.getManageId());
        UserPermissions.assertManagerRole(provider, user);
        return ResponseEntity.ok(roleRepository.save(role));
    }

}
