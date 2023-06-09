package access.api;

import access.config.Config;
import access.manage.ManageIdentifier;
import access.manage.Manage;
import access.model.User;
import access.repository.UserRepository;
import access.secuirty.UserPermissions;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
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
import java.util.stream.Collectors;

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
    private final Manage manage;


    @Autowired
    public UserController(Config config, UserRepository userRepository, ObjectMapper objectMapper, Manage manage) {
        this.config = config;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
        this.manage = manage;
    }

    @GetMapping("config")
    public ResponseEntity<Config> config(User user) {
        LOG.debug("/config");
        boolean authenticated = user != null && user.getId() != null;
        config.withName(user != null ? user.getName() : null);
        return ResponseEntity.ok(config.withAuthenticated(authenticated));
    }

    @GetMapping("me")
    public ResponseEntity<User> me(@Parameter(hidden = true) User user) {
        LOG.debug("/me");
        List<Map<String, Object>> providers = user.getUserRoles().stream()
                .map(userRole -> new ManageIdentifier(userRole.getRole().getManageId(), userRole.getRole().getManageType()))
                //Prevent unnecessary round-trips to Manage
                .collect(Collectors.toSet())
                .stream()
                .map(identity -> manage.providerById(identity.entityType(), identity.id()))
                .toList();
        user.setProviders(providers);

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
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
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
