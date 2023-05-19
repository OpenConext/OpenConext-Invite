package access.api;

import access.model.User;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.RedirectView;

import java.util.Map;

import static access.SwaggerOpenIdConfig.OPEN_ID_SCHEME_NAME;

@RestController
@RequestMapping(value = {"/api/v1/users", "/api/external/v1/users"}, produces = MediaType.APPLICATION_JSON_VALUE)
@Transactional
@SecurityRequirement(name = OPEN_ID_SCHEME_NAME, scopes = {"openid"})
public class UserController {

    private static final Log LOG = LogFactory.getLog(UserController.class);

    @Autowired
    public UserController() {
    }

    @GetMapping("config")
    public ResponseEntity<Map<String, Object>> config(User user) {
        LOG.debug("/config");
        return ResponseEntity.ok(Map.of("authenticated", user != null));
    }

    @GetMapping("me")
    public ResponseEntity<User> me(@Parameter(hidden = true) User user) {
        LOG.debug("/me");
        return ResponseEntity.ok(user);
    }

    @GetMapping("login")
    public View login(@Parameter(hidden = true) User user) {
        LOG.debug("/me");
        return new RedirectView("/client");
    }
}
