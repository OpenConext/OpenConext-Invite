package access.api;

import access.config.Config;
import access.exception.InvalidInputException;
import access.exception.NotFoundException;
import access.logging.AccessLogger;
import access.logging.Event;
import access.manage.Manage;
import access.model.*;
import access.provision.ProvisioningService;
import access.provision.scim.GroupURN;
import access.repository.ApplicationRepository;
import access.repository.ApplicationUsageRepository;
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
import org.springframework.util.CollectionUtils;
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
    private final ApplicationRepository applicationRepository;
    private final ApplicationUsageRepository applicationUsageRepository;
    private final Manage manage;
    private final ProvisioningService provisioningService;
    private final URLFormatValidator urlFormatValidator = new URLFormatValidator();

    public RoleController(Config config,
                          RoleRepository roleRepository,
                          ApplicationRepository applicationRepository,
                          ApplicationUsageRepository applicationUsageRepository,
                          Manage manage,
                          ProvisioningService provisioningService) {
        this.config = config;
        this.roleRepository = roleRepository;
        this.applicationRepository = applicationRepository;
        this.applicationUsageRepository = applicationUsageRepository;
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
            Set<String> manageIdentifiers = user.getApplications().stream().map(m -> (String) m.get("id")).collect(Collectors.toSet());
            //This is a shortcoming of the json_array
            manageIdentifiers.forEach(manageId -> roles.addAll(roleRepository.findByApplicationUsagesApplicationManageId(manageId)));
        }
        Set<String> manageIdentifiers = user.getUserRoles().stream()
                //If the user has an userRole as Inviter, then we must exclude those
                .filter(userRole -> userRole.getAuthority().hasEqualOrHigherRights(Authority.MANAGER))
                .map(userRole -> userRole.getRole().getApplications())
                .flatMap(Collection::stream)
                .map(Application::getManageId)
                .collect(Collectors.toSet());
        manageIdentifiers.forEach(manageId -> roles.addAll(roleRepository.findByApplicationUsagesApplicationManageId(manageId)));
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

    @PostMapping("")
    public ResponseEntity<Role> newRole(@Validated @RequestBody Role role, @Parameter(hidden = true) User user) {
        LOG.debug("/newRole");
        role.setShortName(GroupURN.sanitizeRoleShortName(role.getShortName()));
        role.setIdentifier(UUID.randomUUID().toString());
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

    private ResponseEntity<Role> saveOrUpdate(Role role, User user) {
        if (StringUtils.hasText(role.getLandingPage()) && !urlFormatValidator.isValid(role.getLandingPage())) {
            throw new InvalidInputException();
        }
        if (CollectionUtils.isEmpty(role.getClientApplications())) {
            throw new InvalidInputException();
        }
        manage.addManageMetaData(List.of(role));

        UserPermissions.assertManagerRole(role.getApplicationMaps(), user);

        boolean isNew = role.getId() == null;
        List<String> previousApplicationIdentifiers = new ArrayList<>();
        if (!isNew) {
            Role previousRole = roleRepository.findById(role.getId()).orElseThrow(NotFoundException::new);
            //We don't allow shortName changes after creation
            role.setShortName(previousRole.getShortName());
            previousApplicationIdentifiers.addAll(previousRole.applicationIdentifiers());
        }
        //This is the disadvantage of having to save references from Manage
        Set<Application> applications = role.getClientApplications();
        Set<ApplicationUsage> applicationUsages = applications.stream()
                .map(applicationFromClient -> {
                    Application applicationFromDB = applicationRepository
                            .findByManageIdAndManageType(applicationFromClient.getManageId(), applicationFromClient.getManageType())
                            .orElseGet(() -> applicationRepository.save(applicationFromClient));
                    ApplicationUsage applicationUsage = applicationUsageRepository.findByRoleIdAndApplicationManageIdAndApplicationManageType(
                            role.getId(),
                            applicationFromDB.getManageId(),
                            applicationFromDB.getManageType()
                    ).orElseGet(() -> new ApplicationUsage(applicationFromDB, applicationFromClient.getLandingPage()));
                    applicationUsage.setLandingPage(applicationFromClient.getLandingPage());
                    return applicationUsage;
                })
                .collect(Collectors.toSet());
        role.setApplicationUsages(applicationUsages);
        Role saved = roleRepository.save(role);
        if (isNew) {
            provisioningService.newGroupRequest(saved);
        } else {
            provisioningService.updateGroupRequest(previousApplicationIdentifiers, saved);
        }
        AccessLogger.role(LOG, isNew ? Event.Created : Event.Updated, user, role);
        return ResponseEntity.ok(saved);
    }

}
