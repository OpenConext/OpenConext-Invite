package access.api;

import access.config.Config;
import access.model.User;
import access.model.UserRole;
import access.repository.UserRepository;
import access.secuirty.UserPermissions;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.RedirectView;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
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
    private final ObjectMapper objectMapper;

    @Autowired
    public UserController(Config config, UserRepository userRepository, ObjectMapper objectMapper) {
        this.config = config;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
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

    @PostMapping("error")
    public void error(@RequestBody Map<String, Object> payload, User user) throws
            JsonProcessingException, UnknownHostException {
        payload.put("dateTime", new SimpleDateFormat("yyyyy-mm-dd hh:mm:ss").format(new Date()));
        payload.put("machine", InetAddress.getLocalHost().getHostName());
        payload.put("user", user);
        String msg = objectMapper.writeValueAsString(payload);
        LOG.error(msg, new IllegalArgumentException(msg));
    }

}
