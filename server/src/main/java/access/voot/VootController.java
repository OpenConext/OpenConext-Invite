package access.voot;

import access.model.Authority;
import access.model.Role;
import access.model.User;
import access.model.UserRole;
import access.provision.scim.GroupURN;
import access.repository.UserRepository;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static access.SwaggerOpenIdConfig.BASIC_AUTHENTICATION_SCHEME_NAME;

@RestController
@RequestMapping(value = {"/api/voot", "/api/external/v1/voot"}, produces = MediaType.APPLICATION_JSON_VALUE)
@SecurityRequirement(name = BASIC_AUTHENTICATION_SCHEME_NAME)
public class VootController {

    private static final Log LOG = LogFactory.getLog(VootController.class);

    private final UserRepository userRepository;
    private final String groupUrnPrefix;

    public VootController(UserRepository userRepository,
                          @Value("${voot.group_urn_domain}") String groupUrnPrefix) {
        this.userRepository = userRepository;
        this.groupUrnPrefix = groupUrnPrefix;
    }

    @GetMapping("/{unspecified_id}")
    @PreAuthorize("hasRole('VOOT')")
    public ResponseEntity<List<Map<String, String>>> getGroupMemberships(@PathVariable("unspecified_id") String unspecifiedId) {
        Optional<User> optionalUser = userRepository.findBySubIgnoreCase(unspecifiedId);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            user.setLastActivity(Instant.now());
            userRepository.save(user);
            List<Map<String, String>> roles = user.getUserRoles().stream()
                    .filter(userRole -> userRole.getAuthority().equals(Authority.GUEST) || userRole.isGuestRoleIncluded())
                    .map(this::parseUserRole).collect(Collectors.toList());

            LOG.debug(String.format("Returning %o roles for VOOT request for user: %s", roles.size(), unspecifiedId));

            return ResponseEntity.ok(roles);
        } else {
            LOG.debug(String.format("VOOT request for unknown user: %s", unspecifiedId));

            return ResponseEntity.ok(Collections.emptyList());
        }
    }

    private Map<String, String> parseUserRole(UserRole userRole) {
        Map<String, String> res = new HashMap<>();
        Role role = userRole.getRole();
        String urn = role.isTeamsOrigin() ? role.getUrn() : GroupURN.urnFromRole(groupUrnPrefix, role);
        res.put("urn", urn);
        res.put("name", role.getName());
        return res;
    }

}
