package access.api;

import access.model.Application;
import access.model.User;
import access.repository.ApplicationRepository;
import access.secuirty.UserPermissions;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static access.SwaggerOpenIdConfig.OPEN_ID_SCHEME_NAME;

@RestController
@RequestMapping(value = {"/api/v1/applications", "/api/external/v1/applications"}, produces = MediaType.APPLICATION_JSON_VALUE)
@Transactional
@SecurityRequirement(name = OPEN_ID_SCHEME_NAME, scopes = {"openid"})
public class ApplicationController {

    private static final Log LOG = LogFactory.getLog(ApplicationController.class);

    private final ApplicationRepository applicationRepository;

    @Autowired
    public ApplicationController(ApplicationRepository applicationRepository) {
        this.applicationRepository = applicationRepository;
    }

    @PostMapping()
    public ResponseEntity<Application> create(User user, @Validated @RequestBody Application application) {
        LOG.debug("Create application " + application + " by " + user);

        UserPermissions.assertSuperUser(user);

        return ResponseEntity.ok(applicationRepository.save(application));
    }

}
