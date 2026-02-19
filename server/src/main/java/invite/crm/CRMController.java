package invite.crm;

import invite.repository.UserRepository;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
//        userRepository.findBySubIgnoreCase()
        return ResponseEntity.ok().body("created");
    }
}
