package access.api;

import access.config.Config;
import access.exception.NotFoundException;
import access.manage.EntityType;
import access.manage.Manage;
import access.model.*;
import access.provision.Provisioning;
import access.provision.graph.GraphClient;
import access.repository.InvitationRepository;
import access.repository.RemoteProvisionedUserRepository;
import access.repository.UserRepository;
import access.security.UserPermissions;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import crypto.KeyStore;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.RedirectView;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static access.SwaggerOpenIdConfig.API_TOKENS_SCHEME_NAME;
import static access.SwaggerOpenIdConfig.OPEN_ID_SCHEME_NAME;

@RestController
@RequestMapping(value = {"/api/v1/users", "/api/external/v1/users"}, produces = MediaType.APPLICATION_JSON_VALUE)
@Transactional
@SecurityRequirement(name = OPEN_ID_SCHEME_NAME, scopes = {"openid"})
@EnableConfigurationProperties(Config.class)
@SecurityRequirement(name = API_TOKENS_SCHEME_NAME)
public class UserController {

    private static final Log LOG = LogFactory.getLog(UserController.class);

    private final Config config;
    private final UserRepository userRepository;
    private final InvitationRepository invitationRepository;
    private final Manage manage;
    private final ObjectMapper objectMapper;
    private final RemoteProvisionedUserRepository remoteProvisionedUserRepository;
    private final GraphClient graphClient;

    @Autowired
    public UserController(Config config,
                          UserRepository userRepository,
                          InvitationRepository invitationRepository,
                          Manage manage,
                          ObjectMapper objectMapper,
                          RemoteProvisionedUserRepository remoteProvisionedUserRepository,
                          KeyStore keyStore,
                          @Value("${config.eduid-idp-schac-home-organization}") String eduidIdpSchacHomeOrganization,
                          @Value("${config.server-url}") String serverBaseURL,
                          @Value("${voot.group_urn_domain}") String groupUrnPrefix) {
        this.invitationRepository = invitationRepository;
        this.config = config.withGroupUrnPrefix(groupUrnPrefix);
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
        this.manage = manage;
        this.remoteProvisionedUserRepository = remoteProvisionedUserRepository;
        this.graphClient = new GraphClient(serverBaseURL, eduidIdpSchacHomeOrganization, keyStore);
    }

    @GetMapping("config")
    public ResponseEntity<Config> config(User user) {
        LOG.debug("/config");
        Config result = new Config(this.config);
        result
                .withAuthenticated(user != null && user.getId() != null)
                .withName(user != null ? user.getName() : null);
        if (user != null && user.getId() == null) {
            verifyMissingAttributes(user, result);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("me")
    public ResponseEntity<User> me(@Parameter(hidden = true) User user) {
        LOG.debug("/me");
        List<Role> roles = user.getUserRoles().stream().map(UserRole::getRole).toList();
        manage.addManageMetaData(roles);
        return ResponseEntity.ok(user);
    }

    @GetMapping("other/{id}")
    public ResponseEntity<User> details(@PathVariable("id") Long id, @Parameter(hidden = true) User user) {
        LOG.debug("/me");
        UserPermissions.assertSuperUser(user);
        User other = userRepository.findById(id).orElseThrow(NotFoundException::new);

        List<Role> roles = other.getUserRoles().stream().map(UserRole::getRole).toList();
        manage.addManageMetaData(roles);
        return ResponseEntity.ok(other);
    }

    @GetMapping("search")
    public ResponseEntity<List<User>> search(@RequestParam(value = "query") String query,
                                             @Parameter(hidden = true) User user) {
        LOG.debug("/search");
        UserPermissions.assertSuperUser(user);
        List<User> users = query.equals("owl") ? userRepository.findAll() :
                userRepository.search(query.replaceAll("@", " ") + "*", 15);
        return ResponseEntity.ok(users);
    }

    @GetMapping("login")
    public View login(@RequestParam(value = "app", required = false, defaultValue = "client") String app) {
        LOG.debug("/login");
        return new RedirectView(app.equals("client") ? config.getClientUrl() : config.getWelcomeUrl(), false);
    }

    @GetMapping("logout")
    public ResponseEntity<Map<String, Integer>> logout(HttpServletRequest request) {
        LOG.debug("/logout");
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return Results.okResult();
    }

    @GetMapping("ms-accept-return/{manageId}/{userId}")
    public View msAcceptReturn(@PathVariable("manageId") String manageId, @PathVariable("userId") Long userId) {
        User user = userRepository.findById(userId).orElseThrow(NotFoundException::new);
        Map<String, Object> provisioningMap = manage.providerById(EntityType.PROVISIONING, manageId);
        Provisioning provisioning = new Provisioning(provisioningMap);
        AtomicReference<String> redirectReference = new AtomicReference<>(this.config.getWelcomeUrl());
        Optional<RemoteProvisionedUser> remoteProvisionedUserOptional = remoteProvisionedUserRepository
                .findByManageProvisioningIdAndUser(manageId, user);
        remoteProvisionedUserOptional.ifPresent(remoteProvisionedUser -> {
            graphClient.updateUserRequest(user, provisioning, remoteProvisionedUser.getRemoteIdentifier());
            String invitationHash = invitationRepository.findTopBySubInviteeOrderByCreatedAtDesc(user.getSub())
                    .map(Invitation::getHash).orElse("");
            String redirectUrl = String.format("%s/proceed?hash=%s&isRedirect=true", config.getWelcomeUrl(), invitationHash);
            redirectReference.set(redirectUrl);
        });
        return new RedirectView(redirectReference.get());
    }

    @PostMapping("error")
    public ResponseEntity<Map<String, Integer>> error(@RequestBody Map<String, Object> payload,
                                                      @Parameter(hidden = true) User user) throws
            JsonProcessingException, UnknownHostException {
        payload.put("dateTime", new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(new Date()));
        payload.put("machine", InetAddress.getLocalHost().getHostName());
        payload.put("user", user);
        String msg = objectMapper.writeValueAsString(payload);
        LOG.error(msg, new IllegalArgumentException(msg));
        return Results.createResult();
    }

    private void verifyMissingAttributes(User user, Config result) {
        List<String> missingAttributes = new ArrayList<>();
        if (!StringUtils.hasText(user.getSub())) {
            missingAttributes.add("sub");
        }
        if (!StringUtils.hasText(user.getEmail())) {
            missingAttributes.add("email");
        }
        if (!StringUtils.hasText(user.getSchacHomeOrganization())) {
            missingAttributes.add("schacHomeOrganization");
        }
        if (!missingAttributes.isEmpty()) {
            result.withMissingAttributes(missingAttributes);
        }
    }


}
