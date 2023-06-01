package access.api;

import access.config.Config;
import access.exception.NotFoundException;
import access.manage.Manage;
import access.model.Role;
import access.model.User;
import access.repository.RoleRepository;
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

import java.util.Map;

import static access.SwaggerOpenIdConfig.OPEN_ID_SCHEME_NAME;

@RestController
@RequestMapping(value = {"/api/v1/roles", "/api/external/v1/roles"}, produces = MediaType.APPLICATION_JSON_VALUE)
@Transactional
@SecurityRequirement(name = OPEN_ID_SCHEME_NAME, scopes = {"openid"})
@EnableConfigurationProperties(Config.class)
public class RoleController {

    private static final Log LOG = LogFactory.getLog(RoleController.class);

    private final RoleRepository roleRepository;
    private final Manage manage;

    public RoleController(RoleRepository roleRepository, Manage manage) {
        this.roleRepository = roleRepository;
        this.manage = manage;
    }

    @GetMapping("{id}")
    public ResponseEntity<Role> role(@PathVariable("id") Long id, User user) {
        LOG.debug("/role");
        return ResponseEntity.ok(roleRepository.findById(id).orElseThrow(NotFoundException::new));
    }

    @PostMapping("")
    public ResponseEntity<Role> newRole(@Validated @RequestBody Role role, @Parameter(hidden = true) User user) {
        LOG.debug("/me");
        Map<String, Object> provider = manage.providerById(role.getManageType(), role.getManageId());
        UserPermissions.assertManageRole(provider, user);
        return ResponseEntity.ok(roleRepository.save(role));
    }
}
