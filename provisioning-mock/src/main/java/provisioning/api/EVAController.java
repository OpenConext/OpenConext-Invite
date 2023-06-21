package provisioning.api;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import provisioning.config.Database;
import provisioning.config.ProvisioningType;
import provisioning.model.GuestAccount;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping(value = "/eva")
public class EVAController {

    private static final Log LOG = LogFactory.getLog(EVAController.class);

    private final Map<String, Map<String, Object>> users;
    private final ObjectMapper objectMapper;

    @Autowired
    public EVAController(Database database, ObjectMapper objectMapper) {
        this.users = database.users(ProvisioningType.eva);
        this.objectMapper = objectMapper;
    }

    @PostMapping(value = "/api/v1/guest/create", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Map<String, Object>> createUser(GuestAccount guestAccount) throws JsonProcessingException {
        LOG.info("eva/api/v1/guest/create POST " + guestAccount);

        String id = UUID.randomUUID().toString();
        guestAccount.setId(id);
        Map<String, Object> user = objectMapper.readValue(objectMapper.writeValueAsString(guestAccount), new TypeReference<>() {
        });

        users.put(id, user);
        return ResponseEntity.ok(user);
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
