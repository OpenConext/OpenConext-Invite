package access.api;

import access.config.Config;
import access.manage.EntityType;
import access.manage.Manage;
import access.model.User;
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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static access.SwaggerOpenIdConfig.OPEN_ID_SCHEME_NAME;

@RestController
@RequestMapping(value = {"/api/v1/manage", "/api/external/v1/manage"}, produces = MediaType.APPLICATION_JSON_VALUE)
@Transactional
@SecurityRequirement(name = OPEN_ID_SCHEME_NAME, scopes = {"openid"})
@EnableConfigurationProperties(Config.class)
@SuppressWarnings("unchecked")
public class ManageController {

    private static final Log LOG = LogFactory.getLog(ManageController.class);

    private final Manage manage;

    @Autowired
    public ManageController(Manage manage) {
        this.manage = manage;
    }

    @GetMapping("provider/{type}/{id}")
    public ResponseEntity<Map<String, Object>> providers(@PathVariable("type") EntityType type,
                                                         @PathVariable("id") String id,
                                                         @Parameter(hidden = true) User user) {
        LOG.debug("/provider");
        Map<String, Object> provider = manage.providerById(type, id);
        return ResponseEntity.ok(provider);
    }

    @GetMapping("provisioning/{id}")
    public ResponseEntity<List<Map<String, Object>>> provisioning(@PathVariable("id") String id,
                                                                  @Parameter(hidden = true) User user) {
        LOG.debug("/provider");
        List<Map<String, Object>> provisioning = manage.provisioning(id);
        if (!user.isSuperUser()) {
            provisioning.forEach(prov -> {
                Map<String, Object> data = (Map<String, Object>) prov.get("data");
                Map<String, Object> metaDataFields = (Map<String, Object>) data.getOrDefault("metaDataFields", Collections.emptyMap());
                List.of("scim_url", "scim_user", "scim_password", "provisioning_mail", "eva_token").forEach(s -> metaDataFields.remove(s));
            });
        }
        return ResponseEntity.ok(provisioning);
    }

    @GetMapping("providers")
    public ResponseEntity<List<Map<String, Object>>> providers(@Parameter(hidden = true) User user) {
        LOG.debug("/providers");
        List<Map<String, Object>> serviceProviders = manage.providers(EntityType.SAML20_SP);
        List<Map<String, Object>> relyingParties = manage.providers(EntityType.OIDC10_RP);
        serviceProviders.addAll(relyingParties);
        return ResponseEntity.ok(serviceProviders);
    }


}
