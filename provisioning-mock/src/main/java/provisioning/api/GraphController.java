package provisioning.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.RedirectView;
import provisioning.model.HttpMethod;
import provisioning.model.Provisioning;
import provisioning.model.ProvisioningType;
import provisioning.model.ResourceType;
import provisioning.repository.ProvisioningRepository;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping(value = "/graph", produces = MediaType.APPLICATION_JSON_VALUE)
public class GraphController {

    private static final Log LOG = LogFactory.getLog(GraphController.class);

    private final ProvisioningRepository provisioningRepository;
    private final ObjectMapper objectMapper;
    private final String inviteServerMSRedirectUrl;
    private final String mockServerBaseUrl;

    @Autowired
    public GraphController(ProvisioningRepository provisioningRepository,
                           ObjectMapper objectMapper,
                           @Value("${invite-server-ms-redirect-url}") String inviteServerMSRedirectUrl,
                           @Value("${mock-server-base-url}") String mockServerBaseUrl) {
        this.provisioningRepository = provisioningRepository;
        this.objectMapper = objectMapper;
        this.inviteServerMSRedirectUrl = inviteServerMSRedirectUrl;
        this.mockServerBaseUrl = mockServerBaseUrl;
    }

    @PostMapping("/users")
    public ResponseEntity<Map<String, Object>> createUser(@RequestBody Map<String, Object> user) {
        LOG.info("/graph/users POST " + user);

        provisioningRepository.save(new Provisioning(
                ProvisioningType.graph,
                objectMapper.valueToTree(user),
                HttpMethod.POST,
                ResourceType.USERS,
                "/graph/users"
        ));
        String inviteRedirectUrl = (String) user.get("inviteRedirectUrl");
        List<String> parts = List.of(inviteRedirectUrl.split("/"));
        String manageId = parts.get(parts.size() - 2);
        String userId = parts.get(parts.size() - 1);
        String id = UUID.randomUUID().toString();
        return ResponseEntity.ok(Map.of(
                "invitedUser", Map.of("id", id),
                "inviteRedeemUrl", String.format("%s/graph/accept/%s/%s", this.mockServerBaseUrl, manageId, userId),
                "id", id));
    }

    @GetMapping("/users")
    public ResponseEntity<Map<String, Object>> getUser() {
        LOG.info("/graph/users GET ");

        provisioningRepository.save(new Provisioning(
                ProvisioningType.graph,
                objectMapper.valueToTree(null),
                HttpMethod.GET,
                ResourceType.USERS,
                "/graph/users"
        ));

        return ResponseEntity.ok(Map.of("id", UUID.randomUUID().toString()));
    }

    @DeleteMapping("/users")
    public ResponseEntity<Void> deleteUser() {
        LOG.info("/graph/users DELETE ");

        provisioningRepository.save(new Provisioning(
                ProvisioningType.graph,
                objectMapper.valueToTree(null),
                HttpMethod.DELETE,
                ResourceType.USERS,
                "/graph/users"
        ));

        return ResponseEntity.status(201).build();
    }

    @PatchMapping("/users")
    public ResponseEntity<Map<String, Object>> updateUser(@RequestBody Map<String, Object> user) {
        LOG.info("/graph/users PATCH " + user);

        provisioningRepository.save(new Provisioning(
                ProvisioningType.graph,
                objectMapper.valueToTree(user),
                HttpMethod.PATCH,
                ResourceType.USERS,
                "/graph/users"
        ));

        return ResponseEntity.ok(user);
    }

    @GetMapping("/accept/{manageId}/{userId}")
    public View accept(@PathVariable("manageId") String manageId, @PathVariable("userId") String userId) {
        LOG.info("/graph/accept GET ");

        return new RedirectView(String.format("%s/%s/%s", this.inviteServerMSRedirectUrl, manageId, userId));
    }

}
