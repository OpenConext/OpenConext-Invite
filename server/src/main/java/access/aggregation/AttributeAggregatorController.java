package access.aggregation;

import access.manage.EntityType;
import access.manage.Manage;
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
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static access.SwaggerOpenIdConfig.BASIC_AUTHENTICATION_SCHEME_NAME;

@RestController
@RequestMapping(value = {"/api/external/v1/aa"}, produces = MediaType.APPLICATION_JSON_VALUE)
@SecurityRequirement(name = BASIC_AUTHENTICATION_SCHEME_NAME)
public class AttributeAggregatorController {

    private static final Log LOG = LogFactory.getLog(AttributeAggregatorController.class);

    private final UserRepository userRepository;
    private final Manage manage;
    private final String groupUrnPrefix;

    public AttributeAggregatorController(UserRepository userRepository,
                                         Manage manage,
                                         @Value("${voot.group_urn_domain}") String groupUrnPrefix) {
        this.userRepository = userRepository;
        this.manage = manage;
        this.groupUrnPrefix = groupUrnPrefix;
    }

    @GetMapping("/{unspecified_id}")
    @PreAuthorize("hasRole('ATTRIBUTE_AGGREGATION')")
    public ResponseEntity<List<Map<String, String>>> getGroupMemberships(@PathVariable("unspecified_id") String unspecifiedId,
                                                                         @RequestParam("SPentityID") String spEntityId) {
        Optional<Map<String, Object>> optionalProvider;
        try {
            optionalProvider = manage
                    .providerByEntityID(EntityType.SAML20_SP, spEntityId)
                    .or(() -> manage.providerByEntityID(EntityType.OIDC10_RP, spEntityId));
        } catch (RuntimeException e) {
            LOG.error("Error in communication with Manage", e);
            optionalProvider = Optional.empty();
        }

        if (optionalProvider.isEmpty()) {
            LOG.debug(String.format("AA request for unknown service: %s", spEntityId));
            return ResponseEntity.ok(Collections.emptyList());
        }
        Optional<User> optionalUser = userRepository.findBySubIgnoreCase(unspecifiedId);
        if (optionalUser.isEmpty()) {
            LOG.debug(String.format("AA request for unknown user: %s", unspecifiedId));
            return ResponseEntity.ok(Collections.emptyList());
        }
        User user = optionalUser.get();
        user.setLastActivity(Instant.now());
        userRepository.save(user);

        Map<String, Object> provider = optionalProvider.get();
        List<Map<String, String>> userRoles = user.getUserRoles().stream()
                .filter(userRole -> userRole.getRole().applicationsUsed().stream().anyMatch(application -> application.getManageId().equals(provider.get("id"))))
                .filter(userRole -> userRole.getAuthority().equals(Authority.GUEST) || userRole.isGuestRoleIncluded())
                .map(this::parseUserRole)
                .toList();
        LOG.debug(String.format("Returning %o roles for AA request for user: %s and service %s", userRoles.size(), unspecifiedId, spEntityId));
        return ResponseEntity.ok(userRoles);
    }

    private Map<String, String> parseUserRole(UserRole userRole) {
        Role role = userRole.getRole();
        String urn = role.isTeamsOrigin() ? role.getUrn() : GroupURN.urnFromRole(groupUrnPrefix, role);
        return Map.of("id", urn);
    }

}
