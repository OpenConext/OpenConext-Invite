package provisioning.api;

import provisioning.model.ProvisioningType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import provisioning.model.HttpMethod;
import provisioning.model.Provisioning;
import provisioning.model.ResourceType;
import provisioning.repository.ProvisioningRepository;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping(value = "/graph", produces = MediaType.APPLICATION_JSON_VALUE)
public class GraphController {

    private static final Log LOG = LogFactory.getLog(GraphController.class);

    private final ProvisioningRepository provisioningRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    public GraphController(ProvisioningRepository provisioningRepository, ObjectMapper objectMapper) {
        this.provisioningRepository = provisioningRepository;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/users")
    public ResponseEntity<Map<String, String>> createUser(@RequestBody Map<String, Object> user) {
        LOG.info("/graph/users POST " + user);

        String id = UUID.randomUUID().toString();
        provisioningRepository.save(new Provisioning(
                ProvisioningType.graph,
                objectMapper.valueToTree(user),
                HttpMethod.POST,
                ResourceType.USERS,
                "/graph/users"
        ));

        return ResponseEntity.ok(Collections.singletonMap("id", id));
    }

}
