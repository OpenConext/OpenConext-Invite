package access.api;

import access.config.Config;
import access.cron.ResourceCleaner;
import access.model.User;
import access.security.UserPermissions;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

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
        Map<String, List<? extends Serializable>> body = resourceCleaner.doClean();
        return ResponseEntity.ok(body);
    }

}
