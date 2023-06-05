package access.api;

import access.config.Config;
import access.model.User;
import access.model.UserRole;
import access.repository.UserRepository;
import access.secuirty.UserPermissions;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.RedirectView;

import java.util.List;
import java.util.Map;

import static access.SwaggerOpenIdConfig.OPEN_ID_SCHEME_NAME;

@RestController
@RequestMapping(value = {"/api/v1/users", "/api/external/v1/users"}, produces = MediaType.APPLICATION_JSON_VALUE)
@Transactional
@SecurityRequirement(name = OPEN_ID_SCHEME_NAME, scopes = {"openid"})
@EnableConfigurationProperties(Config.class)
public class UserController {

    private static final Log LOG = LogFactory.getLog(UserController.class);

    private final Config config;
    private final UserRepository userRepository;

    @Autowired
    public UserController(Config config, UserRepository userRepository) {
        this.config = config;
        this.userRepository = userRepository;
    }

    @GetMapping("config")
    public ResponseEntity<Config> config(User user) {
        LOG.debug("/config");
        boolean authenticated = user != null;
        return ResponseEntity.ok(config.withAuthenticated(authenticated));
    }

    @GetMapping("me")
    public ResponseEntity<User> me(@Parameter(hidden = true) User user) {
        LOG.debug("/me");
        user.getUserRoles().forEach(UserRole::getRole);
        return ResponseEntity.ok(user);
    }

    @GetMapping("search")
    public ResponseEntity<List<User>> search(@RequestParam(value = "query") String query,
                                       @Parameter(hidden = true) User user) {
        LOG.debug("/search");
        UserPermissions.assertSuperUser(user);
        List<User> users = userRepository.search(query + "*", 15);
        return ResponseEntity.ok(users);
    }

    @GetMapping("login")
    public View login() {
        LOG.debug("/login");
        return new RedirectView(config.getClientUrl(), false);
    }

    @GetMapping("logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletRequest request) {
        LOG.debug("/logout");
        SecurityContextHolder.clearContext();
        request.getSession(false).invalidate();
        return ResponseEntity.ok(Map.of("result", "ok"));
    }
}
