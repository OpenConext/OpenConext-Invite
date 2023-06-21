package provisioning.api;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import provisioning.config.Database;
import provisioning.config.ProvisioningType;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping(value = "/graph", produces = MediaType.APPLICATION_JSON_VALUE)
public class GraphController {

    private static final Log LOG = LogFactory.getLog(GraphController.class);

    private final Map<String, Map<String, Object>> users;

    @Autowired
    public GraphController(Database database) {
        this.users = database.users(ProvisioningType.scim);
    }

    @PostMapping("/users")
    public ResponseEntity<Map<String, String>> createUser(@RequestBody Map<String, Object> user) {
        LOG.info("/graph/users POST " + user);

        String id = UUID.randomUUID().toString();
        users.put(id, user);
        return ResponseEntity.ok(Collections.singletonMap("id", id));
    }

    @GetMapping(value = "/users")
    public ResponseEntity<Map<String, Map<String, Object>>> users() {
        return ResponseEntity.ok(users);
    }

}
