package access.api;

import access.config.Config;
import access.exception.NotFoundException;
import access.manage.EntityType;
import access.manage.Manage;
import access.model.Application;
import access.model.Authority;
import access.model.User;
import access.repository.ApplicationRepository;
import access.repository.RoleRepository;
import access.security.UserPermissions;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static access.SwaggerOpenIdConfig.API_TOKENS_SCHEME_NAME;
import static access.SwaggerOpenIdConfig.OPEN_ID_SCHEME_NAME;

@RestController
@RequestMapping(value = {"/api/v1/manage", "/api/external/v1/manage"}, produces = MediaType.APPLICATION_JSON_VALUE)
@Transactional
@SecurityRequirement(name = OPEN_ID_SCHEME_NAME, scopes = {"openid"})
@SecurityRequirement(name = API_TOKENS_SCHEME_NAME)
@EnableConfigurationProperties(Config.class)
@SuppressWarnings("unchecked")
public class ManageController {

    private static final Log LOG = LogFactory.getLog(ManageController.class);

    private final Manage manage;
    private final ApplicationRepository applicationRepository;
    private final RoleRepository roleRepository;

    @Autowired
    public ManageController(Manage manage,
                            ApplicationRepository applicationRepository,
                            RoleRepository roleRepository) {
        this.manage = manage;
        this.applicationRepository = applicationRepository;
        this.roleRepository = roleRepository;
    }

    @GetMapping("provider/{type}/{id}")
    public ResponseEntity<Map<String, Object>> providerById(@PathVariable("type") EntityType type,
                                                            @PathVariable("id") String id,
                                                            @Parameter(hidden = true) User user) {
        LOG.debug(String.format("GET /manage/provider type: %s, id: %s for user %s", type, id, user.getEduPersonPrincipalName()));
        UserPermissions.assertSuperUser(user);
        Map<String, Object> provider = manage.providerById(type, id);
        return ResponseEntity.ok(provider);
    }

    @GetMapping("providers")
    public ResponseEntity<List<Map<String, Object>>> providers(@Parameter(hidden = true) User user) {
        LOG.debug(String.format("GET /manage/providers for user %s", user.getEduPersonPrincipalName()));
        UserPermissions.assertAuthority(user, Authority.SUPER_USER);
        List<Map<String, Object>> providers = manage.providers(EntityType.SAML20_SP, EntityType.OIDC10_RP);
        return ResponseEntity.ok(providers);
    }

    @GetMapping("organization-guid-validation/{organizationGUID}")
    public ResponseEntity<Map<String, Object>> organizationGUIDValidation(@Parameter(hidden = true) User user,
                                                                          @PathVariable("organizationGUID") String organizationGUID) {
        LOG.debug(String.format("GET /manage/organization-guid-validation guid: %s for user %s", organizationGUID, user.getEduPersonPrincipalName()));

        UserPermissions.assertSuperUser(user);
        List<Map<String, Object>> identityProviders = manage.identityProvidersByInstitutionalGUID(organizationGUID);
        if (CollectionUtils.isEmpty(identityProviders)) {
            new NotFoundException("No identity provider with organizationGUID: " + organizationGUID);
        }
        return ResponseEntity.ok(identityProviders.getFirst());
    }

    @GetMapping("applications")
    public ResponseEntity<Map<String, List<Map<String, Object>>>> applications(@Parameter(hidden = true) User user) {
        LOG.debug("/applications");
        LOG.debug(String.format("GET /manage/applications for user %s", user.getEduPersonPrincipalName()));

        UserPermissions.assertInstitutionAdmin(user);
        List<Application> applications;
        if (user.isSuperUser()) {
            applications = applicationRepository.findAll();
        } else {
            applications = roleRepository.findByOrganizationGUID(user.getOrganizationGUID())
                    .stream()
                    .map(role -> role.getApplicationUsages())
                    .flatMap(Set::stream)
                    .map(applicationUsage -> applicationUsage.getApplication())
                    .toList();
        }
        Map<EntityType, List<Application>> groupedByManageType = applications.stream().collect(Collectors.groupingBy(Application::getManageType));

        List<Map<String, Object>> providers = groupedByManageType.entrySet().stream()
                .map(entry -> manage.providersByIdIn(
                        entry.getKey(),
                        entry.getValue().stream().map(Application::getManageId).collect(Collectors.toList())))
                .flatMap(Collection::stream)
                .toList();
        //Convert to map with key = manage_id and value = role_count
        Map<String, Long> applicationsPerManageId = applicationRepository.countByApplications().stream()
                .collect(Collectors.toMap(m -> (String) m.get("manage_id"), m -> (Long) m.get("role_count")));
        providers.forEach(provider -> {
            Long roleCount = applicationsPerManageId.getOrDefault(provider.get("id"), 0L);
            provider.put("roleCount", roleCount);
        });
        List<Map<String, Object>> provisionings = manage.provisioning(applications.stream()
                .map(Application::getManageId)
                .toList());
        return ResponseEntity.ok(Map.of(
                "providers", providers,
                "provisionings", provisionings
        ));
    }

}
