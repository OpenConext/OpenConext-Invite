package access.api;

import access.config.Config;
import access.manage.EntityType;
import access.manage.Manage;
import access.manage.ManageIdentifier;
import access.model.Authority;
import access.model.User;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
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
    private final RoleRepository roleRepository;

    @Autowired
    public ManageController(Manage manage, RoleRepository roleRepository) {
        this.manage = manage;
        this.roleRepository = roleRepository;
    }

    @GetMapping("provider/{type}/{id}")
    public ResponseEntity<Map<String, Object>> providerById(@PathVariable("type") EntityType type,
                                                            @PathVariable("id") String id,
                                                            @Parameter(hidden = true) User user) {
        LOG.debug("/provider");
        UserPermissions.assertSuperUser(user);
        Map<String, Object> provider = manage.providerById(type, id);
        return ResponseEntity.ok(provider);
    }

    @GetMapping("providers")
    public ResponseEntity<List<Map<String, Object>>> providers(@Parameter(hidden = true) User user) {
        LOG.debug("/providers");
        UserPermissions.assertAuthority(user, Authority.SUPER_USER);
        List<Map<String, Object>> serviceProviders = manage.providers(EntityType.SAML20_SP);
        List<Map<String, Object>> relyingParties = manage.providers(EntityType.OIDC10_RP);
        List<Map<String, Object>> results = new ArrayList<>();
        results.addAll(serviceProviders);
        results.addAll(relyingParties);
        return ResponseEntity.ok(results);
    }

    @GetMapping("applications")
    public ResponseEntity<Map<String, List<Map<String, Object>>>> applications(@Parameter(hidden = true) User user) {
        UserPermissions.assertSuperUser(user);
        Set<ManageIdentifier> manageIdentifiers = roleRepository.findDistinctManageIdentifiers();
        Map<EntityType, List<ManageIdentifier>> groupedByManageType = manageIdentifiers.stream().collect(Collectors.groupingBy(ManageIdentifier::manageType));
        List<Map<String, Object>> providers = groupedByManageType.entrySet().stream()
                .map(entry -> manage.providersByIdIn(
                        entry.getKey(),
                        entry.getValue().stream().map(ManageIdentifier::manageId).collect(Collectors.toList())))
                .flatMap(Collection::stream)
                .toList();
        List<Map<String, Object>> provisionings = manage.provisioning(manageIdentifiers.stream()
                .map(ManageIdentifier::manageId)
                .toList());
        return ResponseEntity.ok(Map.of(
                "providers", providers,
                "provisionings", provisionings
        ));
    }

}
