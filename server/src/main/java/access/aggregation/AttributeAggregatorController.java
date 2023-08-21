package access.aggregation;

import access.manage.EntityType;
import access.manage.Manage;
import access.model.User;
import access.model.UserRole;
import access.provision.scim.GroupURN;
import access.repository.UserRepository;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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

import static access.SwaggerOpenIdConfig.ATTRIBUTE_AGGREGATION_SCHEME_NAME;

@RestController
@RequestMapping(value = {"/api/aa", "/api/external/v1/aa"}, produces = MediaType.APPLICATION_JSON_VALUE)
@SecurityRequirement(name = ATTRIBUTE_AGGREGATION_SCHEME_NAME)
public class AttributeAggregatorController {

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
        Optional<Map<String, Object>> optionalProvider = manage
                .providerByEntityID(EntityType.SAML20_SP, spEntityId)
                .or(() -> manage.providerByEntityID(EntityType.OIDC10_RP, spEntityId));
        if (optionalProvider.isEmpty()) {
            return ResponseEntity.ok(Collections.emptyList());
        }
        Optional<User> optionalUser = userRepository.findBySubIgnoreCase(unspecifiedId);
        if (optionalUser.isEmpty()) {
            return ResponseEntity.ok(Collections.emptyList());
        }
        User user = optionalUser.get();
        user.setLastActivity(Instant.now());
        userRepository.save(user);

        Map<String, Object> provider = optionalProvider.get();
        List<Map<String, String>> roles = user.getUserRoles().stream()
                .filter(role -> role.getRole().getManageId().equals(provider.get("id")))
                .map(this::parseUserRole)
                .toList();
        return ResponseEntity.ok(roles);
    }

    private Map<String, String> parseUserRole(UserRole userRole) {
        return Map.of("id", GroupURN.urnFromRole(groupUrnPrefix, userRole.getRole()));
    }

}
