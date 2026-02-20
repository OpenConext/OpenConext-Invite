package invite.crm;

import invite.model.User;
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

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@RestController
@RequestMapping(value = {"/api/internal/v1/crm"}, produces = MediaType.APPLICATION_JSON_VALUE)
public class CRMController {

    private static final Log LOG = LogFactory.getLog(CRMController.class);

    private final UserRepository userRepository;

    public CRMController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostMapping("")
    public ResponseEntity<String> contact(@RequestBody CRMContact crmContact) {
        LOG.debug("POST /api/external/v1/crm: " + crmContact);

        AtomicReference<String> response = new AtomicReference<>();
        Optional<User> userOptional = userRepository.findByCrmContactId(crmContact.getContactId());
        userOptional.ifPresentOrElse(user -> {
                    response.set("updated");
                },
                () -> {
                    response.set("created");
                });

        return ResponseEntity.ok().body(response.get());
    }
}
