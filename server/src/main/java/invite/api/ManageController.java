package invite.api;

import invite.config.Config;
import invite.exception.NotFoundException;
import invite.manage.EntityType;
import invite.manage.Manage;
import invite.model.Application;
import invite.model.Authority;
import invite.model.RequestedAuthnContext;
import invite.model.User;
import invite.repository.ApplicationRepository;
import invite.repository.RoleRepository;
import invite.security.UserPermissions;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static invite.SwaggerOpenIdConfig.API_TOKENS_SCHEME_NAME;
import static invite.SwaggerOpenIdConfig.OPEN_ID_SCHEME_NAME;

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
    private final String eduIDEntityID;

    @Autowired
    public ManageController(Manage manage,
                            ApplicationRepository applicationRepository,
                            RoleRepository roleRepository,
                            @Value("${config.eduid-entity-id}") String eduIDEntityID) {
        this.manage = manage;
        this.applicationRepository = applicationRepository;
        this.roleRepository = roleRepository;
        this.eduIDEntityID = eduIDEntityID;

    }

    @GetMapping("/provider/{type}/{id}")
    public ResponseEntity<Map<String, Object>> providerById(@PathVariable("type") EntityType type,
                                                            @PathVariable("id") String id,
                                                            @Parameter(hidden = true) User user) {
        LOG.debug(String.format("GET /manage/provider type: %s, id: %s for user %s", type, id, user.getEduPersonPrincipalName()));
        UserPermissions.assertSuperUser(user);
        Map<String, Object> provider = manage.providerById(type, id);
        return ResponseEntity.ok(provider);
    }

    @GetMapping("/eduid-identity-provider")
    public ResponseEntity<Map<String, Object>> eduIDIdentityProvider(@Parameter(hidden = true) User user) {
        LOG.debug(String.format("GET /manage/eduIDIdentityProvider type: %s", user.getEduPersonPrincipalName()));
        UserPermissions.assertAuthority(user, Authority.INVITER);
        Map<String, Object> eduIDIdP = manage.providerByEntityID(EntityType.SAML20_IDP, eduIDEntityID)
                .orElseThrow(() -> new NotFoundException("EduID IdP not found: " + eduIDEntityID));
        return ResponseEntity.ok(eduIDIdP);
    }

    @GetMapping("/requested-authn-context-values")
    public ResponseEntity<Map<String, String>> requestedAuthnContextValues() {
        LOG.debug("GET /manage/requestedAuthnContextValues");
        Map<String, String> values = Stream.of(RequestedAuthnContext.values()).collect(Collectors.toMap(rac -> rac.name(), rac -> rac.getUrl()));
        return ResponseEntity.ok(values);
    }

    @GetMapping("/providers")
    public ResponseEntity<List<Map<String, Object>>> providers(@Parameter(hidden = true) User user) {
        LOG.debug(String.format("GET /manage/providers for user %s", user.getEduPersonPrincipalName()));
        UserPermissions.assertAuthority(user, Authority.SUPER_USER);
        List<Map<String, Object>> providers = manage.providers(EntityType.SAML20_SP, EntityType.OIDC10_RP);
        return ResponseEntity.ok(providers);
    }

    @GetMapping("/identity-providers")
    public ResponseEntity<List<Map<String, Object>>> identityProviders(@Parameter(hidden = true) User user) {
        LOG.debug(String.format("GET /manage/identity-providers for user %s", user.getEduPersonPrincipalName()));
        UserPermissions.assertAuthority(user, Authority.SUPER_USER);
        List<Map<String, Object>> providers = manage.providers(EntityType.SAML20_IDP)
                .stream()
                .filter(provider -> StringUtils.hasText((String) provider.get("institutionGuid")))
                .toList();
        return ResponseEntity.ok(providers);
    }

    @GetMapping("/organization-guid-validation/{organizationGUID}")
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

    @GetMapping("/applications")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, List<Map<String, Object>>>> applications(@Parameter(hidden = true) User user) {
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
                "provisionings", sanitizeProvisionings(provisionings)
        ));
    }

    private List<Map<String, Object>> sanitizeProvisionings(List<Map<String, Object>> provisionings) {
        List<String> allowedAttributes = List.of("applications", "name:en", "provisioning_type", "name:nl");
        return provisionings.stream().map(provisioning -> {
                    Map<String, Object> sanitized = new HashMap<>();
                    allowedAttributes.forEach(attr -> sanitized.put(attr, provisioning.get(attr)));
                    return sanitized;
                }
        ).toList();
    }

    @GetMapping("/provisionings/{id}")
    public ResponseEntity<Boolean> provisionings(@PathVariable("id") String id,
                                                 @Parameter(hidden = true) User user) {
        LOG.debug(String.format("GET /manage/provisionings for user %s", user.getEduPersonPrincipalName()));
        UserPermissions.assertInstitutionAdmin(user);
        List<Map<String, Object>> provisionings = manage.provisioning(List.of(id));
        return ResponseEntity.ok(!provisionings.isEmpty());
    }
}
