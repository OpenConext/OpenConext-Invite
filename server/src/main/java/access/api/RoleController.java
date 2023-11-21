package access.api;

import access.config.Config;
import access.exception.InvalidInputException;
import access.exception.NotAllowedException;
import access.exception.NotFoundException;
import access.logging.AccessLogger;
import access.logging.Event;
import access.manage.Manage;
import access.model.*;
import access.provision.ProvisioningService;
import access.provision.scim.GroupURN;
import access.repository.RoleRepository;
import access.security.UserPermissions;
import access.validation.URLFormatValidator;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

import static access.SwaggerOpenIdConfig.API_TOKENS_SCHEME_NAME;
import static access.SwaggerOpenIdConfig.OPEN_ID_SCHEME_NAME;

@RestController
@RequestMapping(value = {"/api/v1/roles", "/api/external/v1/roles", }, produces = MediaType.APPLICATION_JSON_VALUE)
@Transactional
@SecurityRequirement(name = OPEN_ID_SCHEME_NAME, scopes = {"openid"})
@SecurityRequirement(name = API_TOKENS_SCHEME_NAME)
@EnableConfigurationProperties(Config.class)
public class RoleController {
    private static final Log LOG = LogFactory.getLog(RoleController.class);

    private final Config config;
    private final RoleRepository roleRepository;
    private final Manage manage;
    private final ProvisioningService provisioningService;
    private final URLFormatValidator urlFormatValidator = new URLFormatValidator();

    public RoleController(Config config,
                          RoleRepository roleRepository,
                          Manage manage,
                          ProvisioningService provisioningService) {
        this.config = config;
        this.roleRepository = roleRepository;
        this.manage = manage;
        this.provisioningService = provisioningService;
    }

    @GetMapping("")
    public ResponseEntity<List<Role>> rolesByApplication(@Parameter(hidden = true) User user) {
        LOG.debug("/roles");
        if (user.isSuperUser() && !config.isRoleSearchRequired()) {
            return ResponseEntity.ok(manage.addManageMetaData(roleRepository.findAll()));
        }
        UserPermissions.assertAuthority(user, Authority.MANAGER);
        List<Role> roles = new ArrayList<>();
        if (user.isInstitutionAdmin()) {
            roles.addAll(roleRepository.findByApplicationsManageIdIn(user.getApplications().stream().map(m -> (String) m.get("id")).collect(Collectors.toSet())));
        }
        Set<String> manageIdentifiers = user.getUserRoles().stream()
                //If the user has an userRole as Inviter, then we must exclude those
                .filter(userRole -> userRole.getAuthority().hasEqualOrHigherRights(Authority.MANAGER))
                .map(userRole -> userRole.getRole().getApplications())
                .flatMap(Collection::stream)
                .map(application -> application.getManageId())
                .collect(Collectors.toSet());
        roles.addAll(roleRepository.findByApplicationsManageIdIn(manageIdentifiers));
        return ResponseEntity.ok(manage.addManageMetaData(roles));
    }

    @GetMapping("{id}")
    public ResponseEntity<Role> role(@PathVariable("id") Long id,@Parameter(hidden = true) User user) {
        LOG.debug("/role");
        Role role = roleRepository.findById(id).orElseThrow(NotFoundException::new);
        UserPermissions.assertRoleAccess(user, role, Authority.INVITER);
        manage.addManageMetaData(List.of(role));
        return ResponseEntity.ok(role);
    }

    @GetMapping("search")
    public ResponseEntity<List<Role>> search(@RequestParam(value = "query") String query,
                                             @Parameter(hidden = true) User user) {
        LOG.debug("/search");
        UserPermissions.assertSuperUser(user);
        List<Role> roles = roleRepository.search(query + "*", 15);
        return ResponseEntity.ok(manage.addManageMetaData(roles));
    }

    @PostMapping("validation/short_name")
    public ResponseEntity<Map<String, Boolean>> shortNameExists(@RequestBody RoleExists roleExists,
                                                                @Parameter(hidden = true) User user) {
        UserPermissions.assertAuthority(user, Authority.MANAGER);
        String shortName = GroupURN.sanitizeRoleShortName(roleExists.shortName());
        Optional<Role> optionalRole = roleRepository.findByShortNameIgnoreCaseAndApplicationsManageId(roleExists.manageId(), shortName);
        Map<String, Boolean> result = optionalRole
                .map(role -> Map.of("exists", roleExists.id() == null || !role.getId().equals(roleExists.id())))
                .orElse(Map.of("exists", false));
        return ResponseEntity.ok(result);
    }

    @PostMapping("")
    public ResponseEntity<Role> newRole(@Validated @RequestBody Role role, @Parameter(hidden = true) User user) {
        LOG.debug("/newRole");
        String shortName = GroupURN.sanitizeRoleShortName(role.getShortName());
        Optional<Application> applicationOptional = role.getApplications().stream()
                .filter(application -> roleRepository.findByShortNameIgnoreCaseAndApplicationsManageId(application.getManageId(), shortName).isPresent())
                .findFirst();
        if (applicationOptional.isPresent()) {
            Application application = applicationOptional.get();
            throw new NotAllowedException(
                    String.format("Duplicate name: '%s' for manage entity:'%s'", shortName, application.getManageId()));
        }
        return saveOrUpdate(role, user);
    }

    @PutMapping("")
    public ResponseEntity<Role> updateRole(@Validated @RequestBody Role role, @Parameter(hidden = true) User user) {
        LOG.debug("/updateRole");
        return saveOrUpdate(role, user);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRole(@PathVariable("id") Long id, @Parameter(hidden = true) User user) {
        LOG.debug("/deleteRole");
        Role role = roleRepository.findById(id).orElseThrow(NotFoundException::new);
        manage.addManageMetaData(List.of(role));
        UserPermissions.assertManagerRole(role.getApplicationMaps(), user);
        provisioningService.deleteGroupRequest(role);
        roleRepository.delete(role);
        AccessLogger.role(LOG, Event.Deleted, user, role);
        return Results.deleteResult();
    }

    private void addApplicationData(Role role) {

    }

    private ResponseEntity<Role> saveOrUpdate(Role role, User user) {
        if (StringUtils.hasText(role.getLandingPage()) && !urlFormatValidator.isValid(role.getLandingPage())) {
            throw new InvalidInputException();
        }
        manage.addManageMetaData(List.of(role));
        UserPermissions.assertManagerRole(role.getApplicationMaps(), user);
        boolean isNew = role.getId() == null;
        if (!isNew) {
            role.setShortName(roleRepository.findById(role.getId()).orElseThrow(NotFoundException::new).getShortName());
        }
        Role saved = roleRepository.save(role);
        if (isNew) {
            provisioningService.newGroupRequest(saved);
        }
        AccessLogger.role(LOG, isNew ? Event.Created : Event.Updated, user, role);

        return ResponseEntity.ok(saved);
    }

}
