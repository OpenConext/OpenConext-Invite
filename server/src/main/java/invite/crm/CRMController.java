package invite.crm;

import invite.aggregation.AttributeAggregatorController;
import invite.repository.UserRepository;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = {"/api/external/v1/crm"}, produces = MediaType.APPLICATION_JSON_VALUE)
public class CRMController {

    private static final Log LOG = LogFactory.getLog(CRMController.class);

    private final UserRepository userRepository;

    public CRMController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostMapping("")
    @PreAuthorize("hasRole('CRM')")
    public ResponseEntity<String> contact(@RequestBody CRMContact crmContact) {
        userRepository.findBySubIgnoreCase()
    }
