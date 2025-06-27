package provisioning.api;


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
import provisioning.model.ProvisioningType;
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

    @PostMapping("/Users")
    public ResponseEntity<Map<String, String>> createUser(@RequestBody Map<String, Object> user) {
        LOG.info("/api/scim/v2/Users POST " + user);
        provisioningRepository.save(new Provisioning(
                ProvisioningType.scim,
                objectMapper.valueToTree(user),
                HttpMethod.POST,
                ResourceType.USERS,
                "/api/scim/v2/Users"
        ));
        String id = UUID.randomUUID().toString();
        Map<String, String> results = Collections.singletonMap("id", id);
        LOG.info("/api/scim/v2/Users POST Results: " + results);
        return ResponseEntity.ok(results);
    }

    @PutMapping("/Users/{id}")
    public ResponseEntity<Map<String, String>> updateUser(@PathVariable("id") String id, @RequestBody Map<String, Object> user) {
        LOG.info("/api/scim/v2/Users/" + id + " PUT " + user);
        provisioningRepository.save(new Provisioning(
                ProvisioningType.scim,
                objectMapper.valueToTree(user),
                HttpMethod.PUT,
                ResourceType.USERS,
                "/api/scim/v2/Users/" + id
        ));
        return ResponseEntity.ok(Collections.singletonMap("id", id));
    }

    @DeleteMapping("/Users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable("id") String id) {
        LOG.info("/api/scim/v2/Users/" + id + " DELETE");
        provisioningRepository.save(new Provisioning(
                ProvisioningType.scim,
                objectMapper.valueToTree(Map.of("id", id)),
                HttpMethod.DELETE,
                ResourceType.USERS,
                "/api/scim/v2/Users/" + id
        ));
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/Groups")
    public ResponseEntity<Map<String, String>> createGroup(@RequestBody Map<String, Object> group) {
        LOG.info("/api/scim/v2/Groups POST " + group);
        provisioningRepository.save(new Provisioning(
                ProvisioningType.scim,
                objectMapper.valueToTree(group),
                HttpMethod.POST,
                ResourceType.GROUPS,
                "/api/scim/v2/Groups"
        ));
        Map<String, String> results = Collections.singletonMap("id", UUID.randomUUID().toString());
        LOG.info("/api/scim/v2/Groups POST Results: " + results);
        return ResponseEntity.ok(results);
    }

    @PatchMapping("/Groups/{id}")
    public ResponseEntity<Map<String, String>> patchGroup(@PathVariable("id") String id, @RequestBody Map<String, Object> group) {
        LOG.info("/api/scim/v2/Groups/" + id + " PATCH " + group);
        provisioningRepository.save(new Provisioning(
                ProvisioningType.scim,
                objectMapper.valueToTree(group),
                HttpMethod.PATCH,
                ResourceType.GROUPS,
                "/api/scim/v2/Groups/" + id
        ));
        return ResponseEntity.ok(Collections.singletonMap("id", id));
    }

    @PutMapping("/Groups/{id}")
    public ResponseEntity<Map<String, String>> updateGroup(@PathVariable("id") String id, @RequestBody Map<String, Object> group) {
        LOG.info("/api/scim/v2/Groups/" + id + " PUT " + group);
        provisioningRepository.save(new Provisioning(
                ProvisioningType.scim,
                objectMapper.valueToTree(group),
                HttpMethod.PUT,
                ResourceType.GROUPS,
                "/api/scim/v2/Groups/" + id
        ));

        return ResponseEntity.ok(Collections.singletonMap("id", id));
    }


    @DeleteMapping("/Groups/{id}")
    public ResponseEntity<Void> deleteGroup(@PathVariable("id") String id) {
        LOG.info("/api/scim/v2/Groups/" + id + " DELETE");
        provisioningRepository.save(new Provisioning(
                ProvisioningType.scim,
                objectMapper.valueToTree(Map.of("id", id)),
                HttpMethod.DELETE,
                ResourceType.GROUPS,
                "/api/scim/v2/Groups/" + id
        ));
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }


}
