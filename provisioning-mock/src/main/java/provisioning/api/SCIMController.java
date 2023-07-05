package provisioning.api;


import access.provision.ProvisioningType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import provisioning.model.HttpMethod;
import provisioning.model.Provisioning;
import provisioning.model.ResourceType;
import provisioning.repository.ProvisioningRepository;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/scim/v2", produces = MediaType.APPLICATION_JSON_VALUE)
public class SCIMController {

    private static final Log LOG = LogFactory.getLog(SCIMController.class);

    private final ProvisioningRepository provisioningRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    public SCIMController(ProvisioningRepository provisioningRepository, ObjectMapper objectMapper) {
        this.provisioningRepository = provisioningRepository;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/users")
    public ResponseEntity<Map<String, String>> createUser(@RequestBody Map<String, Object> user) {
        LOG.info("/api/scim/v2/users POST " + user);
        provisioningRepository.save(new Provisioning(
                ProvisioningType.scim,
                objectMapper.valueToTree(user),
                HttpMethod.POST,
                ResourceType.USERS,
                "/api/scim/v2/users"
        ));
        String id = UUID.randomUUID().toString();
        return ResponseEntity.ok(Collections.singletonMap("id", id));
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<Map<String, String>> updateUser(@PathVariable("id") String id, @RequestBody Map<String, Object> user) {
        LOG.info("/api/scim/v2/users/" + id + " PUT " + user);
        provisioningRepository.save(new Provisioning(
                ProvisioningType.scim,
                objectMapper.valueToTree(user),
                HttpMethod.PUT,
                ResourceType.USERS,
                "/api/scim/v2/users/" + id
        ));
        return ResponseEntity.ok(Collections.singletonMap("id", id));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable("id") String id) {
        LOG.info("/api/scim/v2/users/" + id + " DELETE");
        provisioningRepository.save(new Provisioning(
                ProvisioningType.scim,
                objectMapper.valueToTree(Map.of("id", id)),
                HttpMethod.DELETE,
                ResourceType.USERS,
                "/api/scim/v2/users/" + id
        ));
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/groups")
    public ResponseEntity<Map<String, String>> createGroup(@RequestBody Map<String, Object> group) {
        LOG.info("/api/scim/v2/groups POST " + group);
        String id = UUID.randomUUID().toString();
        provisioningRepository.save(new Provisioning(
                ProvisioningType.scim,
                objectMapper.valueToTree(group),
                HttpMethod.POST,
                ResourceType.GROUPS,
                "/api/scim/v2/groups"
        ));
        return ResponseEntity.ok(Collections.singletonMap("id", id));
    }

    @PatchMapping("/groups/{id}")
    public ResponseEntity<Map<String, String>> patchGroup(@PathVariable("id") String id, @RequestBody Map<String, Object> group) {
        LOG.info("/api/scim/v2/groups/" + id + " PATCH " + group);
        provisioningRepository.save(new Provisioning(
                ProvisioningType.scim,
                objectMapper.valueToTree(group),
                HttpMethod.PATCH,
                ResourceType.GROUPS,
                "/api/scim/v2/groups/" + id
        ));
        return ResponseEntity.ok(Collections.singletonMap("id", id));
    }

    @PutMapping("/groups/{id}")
    public ResponseEntity<Map<String, String>> updateGroup(@PathVariable("id") String id, @RequestBody Map<String, Object> group) {
        LOG.info("/api/scim/v2/groups/" + id + " PUT " + group);
        provisioningRepository.save(new Provisioning(
                ProvisioningType.scim,
                objectMapper.valueToTree(group),
                HttpMethod.PUT,
                ResourceType.GROUPS,
                "/api/scim/v2/groups/" + id
        ));

        return ResponseEntity.ok(Collections.singletonMap("id", id));
    }


    @DeleteMapping("/groups/{id}")
    public ResponseEntity<Void> deleteGroup(@PathVariable("id") String id) {
        LOG.info("/api/scim/v2/groups/" + id + " DELETE");
        provisioningRepository.save(new Provisioning(
                ProvisioningType.scim,
                objectMapper.valueToTree(Map.of("id", id)),
                HttpMethod.DELETE,
                ResourceType.GROUPS,
                "/api/scim/v2/groups/" + id
        ));
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }


}
