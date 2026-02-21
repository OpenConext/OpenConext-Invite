package invite.crm;

import invite.model.Role;
import invite.model.User;
import invite.provision.ProvisioningService;
import invite.repository.RoleRepository;
import invite.repository.UserRepository;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@RestController
@RequestMapping(value = {"/api/internal/v1/crm"}, produces = MediaType.APPLICATION_JSON_VALUE)
public class CRMController {

    private static final Log LOG = LogFactory.getLog(CRMController.class);

    private final String collabPersonPrefix;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ProvisioningService provisioningService;

    public CRMController(@Value("${crm.collab-person-prefix}") String collabPersonPrefix,
                         UserRepository userRepository,
                         RoleRepository roleRepository,
                         ProvisioningService provisioningService) {
        this.userRepository = userRepository;
        this.collabPersonPrefix = collabPersonPrefix;
        this.roleRepository = roleRepository;
        this.provisioningService = provisioningService;
    }

    @PostMapping("")
    public ResponseEntity<String> contact(@RequestBody CRMContact crmContact) {
        LOG.debug("POST /api/external/v1/crm: " + crmContact);

        AtomicReference<String> response = new AtomicReference<>();
        Optional<User> userOptional = userRepository.findByCrmContactId(crmContact.getContactId());
        userOptional.ifPresentOrElse(user -> {
                    List<Role> currentRoles = user.getUserRoles().stream()
                            .map(userRole -> userRole.getRole())
                            .toList();
                    List<CRMRole> newCrmRoles = crmContact.getRoles().stream()
                            .filter(crmRole -> currentRoles.stream()
                                    .noneMatch(role -> role.getCrmRoleId().equalsIgnoreCase(crmRole.getRoleId())))
                            .toList();
                    //Ensure not to delete regular non-CRM roles
                    List<Role> deletedRoles = currentRoles.stream()
                            .filter(role -> StringUtils.hasText(role.getCrmRoleId()) &&
                                    crmContact.getRoles().stream()
                                            .noneMatch(crmRole -> crmRole.getRoleId().equalsIgnoreCase(role.getCrmRoleId())))
                            .toList();
                    newCrmRoles.forEach(crmRole -> {
                        roleRepository.findByCrmRoleId(crmRole.getRoleId())
                                .ifPresentOrElse();
                    });
                    response.set("updated");
                },
                () -> {
            //Based on the crmContact.
                    response.set("created");
                });

        return ResponseEntity.ok().body(response.get());
    }

    private Role createRole(CRMOrganisation crmOrganisation, CRMRole crmRole) {
        Role role = new Role();
    }
}
