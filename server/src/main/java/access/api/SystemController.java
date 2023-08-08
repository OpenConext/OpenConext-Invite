package access.api;

import access.config.Config;
import access.cron.ResourceCleaner;
import access.exception.NotAllowedException;
import access.exception.NotFoundException;
import access.logging.AccessLogger;
import access.logging.Event;
import access.model.Authority;
import access.model.Role;
import access.model.RoleExists;
import access.model.User;
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

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static access.SwaggerOpenIdConfig.OPEN_ID_SCHEME_NAME;

@RestController
@RequestMapping(value = {"/api/v1/system", "/api/external/v1/system"}, produces = MediaType.APPLICATION_JSON_VALUE)
@Transactional
@SecurityRequirement(name = OPEN_ID_SCHEME_NAME, scopes = {"openid"})
@EnableConfigurationProperties(Config.class)
public class SystemController {
    private static final Log LOG = LogFactory.getLog(SystemController.class);

    private final ResourceCleaner resourceCleaner;

    public SystemController(ResourceCleaner resourceCleaner) {
        this.resourceCleaner = resourceCleaner;
    }

    @GetMapping("/cron")
    public ResponseEntity<Map<String, List<? extends Serializable>>> cron(@Parameter(hidden = true) User user) {
        LOG.debug("/cron");
        UserPermissions.assertSuperUser(user);
        return ResponseEntity.ok(resourceCleaner.doClean());
    }

}
