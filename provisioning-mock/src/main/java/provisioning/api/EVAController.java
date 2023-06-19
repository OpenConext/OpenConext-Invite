package provisioning.api;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping(value = "/eva", produces = MediaType.APPLICATION_JSON_VALUE)
public class EVAController {

    private static final Log LOG = LogFactory.getLog(EVAController.class);

    private final Map<String, Map<String, Object>> users = new HashMap<>();

    @PostMapping(value = "/api/v1/guest/create", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Map<String, String>> createUser(@RequestBody Map<String, Object> user) {
        LOG.info("eva/api/v1/guest/create POST " + user);

        String id = UUID.randomUUID().toString();
        users.put(id, user);
        return ResponseEntity.ok(Collections.singletonMap("id", id));
    }

    @PostMapping(value = "/api/v1/guest/disable/{id}", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Void> disableUser(@PathVariable("id") String id) {
        LOG.info("eva/api/v1/guest/disable POST " + id);
        users.remove(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping(value = "/users")
    public ResponseEntity<Map<String, Map<String, Object>>> users() {
        return ResponseEntity.ok(users);
    }
}
