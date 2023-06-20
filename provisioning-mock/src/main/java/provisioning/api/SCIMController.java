package provisioning.api;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import provisioning.config.Database;
import provisioning.config.ProvisioningType;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/scim/v2", produces = MediaType.APPLICATION_JSON_VALUE)
public class SCIMController {

    private static final Log LOG = LogFactory.getLog(SCIMController.class);

    private final Map<String, Map<String, Object>> users ;
    private final Map<String, Map<String, Object>> groups;

    @Autowired
    public SCIMController(Database database) {
        this.users = database.users(ProvisioningType.scim);
        this.groups = database.groups(ProvisioningType.scim);
    }

    @PostMapping("/users")
    public ResponseEntity<Map<String, String>> createUser(@RequestBody Map<String, Object> user) {
        LOG.info("/api/scim/v2/users POST " + user);

        String id = UUID.randomUUID().toString();
        users.put(id, user);
        return ResponseEntity.ok(Collections.singletonMap("id", id));
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<Map<String, String>> updateUser(@PathVariable("id") String id, @RequestBody Map<String, Object> user) {
        LOG.info("/api/scim/v2/users/" + id + " PUT " + user);

        users.put(id, user);
        return ResponseEntity.ok(Collections.singletonMap("id", id));
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<Map<String, Object>> getUser(@PathVariable("id") String id) {
        LOG.info("/api/scim/v2/users/" + id + " GET");

        return ResponseEntity.ok( users.get(id));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable("id") String id) {
        LOG.info("/api/scim/v2/users/" + id + " DELETE");
        users.remove(id);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/groups")
    public ResponseEntity<Map<String, String>> createGroup(@RequestBody Map<String, Object> group) {
        LOG.info("/api/scim/v2/groups POST " + group);
        String id = UUID.randomUUID().toString();
        groups.put(id, group);
        return ResponseEntity.ok(Collections.singletonMap("id", id));
    }

    @PatchMapping("/groups/{id}")
    public ResponseEntity<Map<String, String>> patchGroup(@PathVariable("id") String id, @RequestBody Map<String, Object> group) {
        LOG.info("/api/scim/v2/groups/" + id + " PATCH " + group);
        Map<String, Object> groupFromDB = groups.get(id);
//        groupFromDB.merge("members", group.get("members"))
        groups.put(id, group);
        return ResponseEntity.ok(Collections.singletonMap("id", id));
    }

    @PutMapping("/groups/{id}")
    public ResponseEntity<Map<String, String>> updateGroup(@PathVariable("id") String id, @RequestBody Map<String, Object> group) {
        LOG.info("/api/scim/v2/groups/" + id + " PUT " + group);
        groups.put(id, group);
        return ResponseEntity.ok(Collections.singletonMap("id", id));
    }

    @GetMapping("/groups/{id}")
    public ResponseEntity<Map<String, Object>> getGroup(@PathVariable("id") String id) {
        LOG.info("/api/scim/v2/groups/" + id + " GET");
        return ResponseEntity.ok(groups.get(id));
    }

    @DeleteMapping("/groups/{id}")
    public ResponseEntity<Void> deleteGroup(@PathVariable("id") String id) {
        LOG.info("/api/scim/v2/groups/" + id + " DELETE");
        groups.remove(id);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/groups")
    public ResponseEntity<Map<String, Map<String, Object>>> allGroups() {
        LOG.info("/api/scim/v2/groups GET");
        return ResponseEntity.ok(groups);
    }

    @GetMapping("/users")
    public ResponseEntity<Map<String, Map<String, Object>>> allUsers() {
        LOG.info("/api/scim/v2/users GET");
        return ResponseEntity.ok(users);
    }
}
