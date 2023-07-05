package provisioning.api;


import access.provision.ProvisioningType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import provisioning.model.GuestAccount;
import provisioning.model.HttpMethod;
import provisioning.model.Provisioning;
import provisioning.model.ResourceType;
import provisioning.repository.ProvisioningRepository;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping(value = "/eva")
public class EVAController {

    private static final Log LOG = LogFactory.getLog(EVAController.class);

    private final ProvisioningRepository provisioningRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    public EVAController(ProvisioningRepository provisioningRepository, ObjectMapper objectMapper) {
        this.provisioningRepository = provisioningRepository;
        this.objectMapper = objectMapper;
    }

    @PostMapping(value = "/api/v1/guest/create", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Map<String, Object>> createUser(GuestAccount guestAccount) throws JsonProcessingException {
        LOG.info("eva/api/v1/guest/create POST " + guestAccount);

        String id = UUID.randomUUID().toString();
        guestAccount.setId(id);
        Map<String, Object> user = objectMapper.convertValue(guestAccount, new TypeReference<>() {
        });
        provisioningRepository.save(new Provisioning(
                ProvisioningType.scim,
                objectMapper.valueToTree(user),
                HttpMethod.DELETE,
                ResourceType.USERS,
                "eva/api/v1/guest/create"
        ));
        return ResponseEntity.ok(user);
    }

    @PostMapping(value = "/api/v1/guest/disable/{id}", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Void> disableUser(@PathVariable("id") String id) {
        LOG.info("eva/api/v1/guest/disable POST " + id);
        provisioningRepository.save(new Provisioning(
                ProvisioningType.graph,
                objectMapper.valueToTree(Map.of("id", id)),
                HttpMethod.POST,
                ResourceType.USERS,
                "/api/v1/guest/disable/" + id
        ));
        return ResponseEntity.ok().build();
    }

}
