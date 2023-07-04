package access.aggregation;

import access.manage.EntityType;
import access.manage.Manage;
import access.model.Role;
import access.model.User;
import access.model.UserRole;
import access.provision.scim.GroupURN;
import access.repository.UserRepository;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static access.SwaggerOpenIdConfig.VOOT_SCHEME_NAME;

@RestController
@RequestMapping(value = {"/api/aa", "/api/external/v1/aa"}, produces = MediaType.APPLICATION_JSON_VALUE)
@SecurityRequirement(name = VOOT_SCHEME_NAME)
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
        List<Map<String, String>> roles = user.getUserRoles().stream().map(this::parseUserRole).toList();
        return ResponseEntity.ok(roles);
    }

    private Map<String, String> parseUserRole(UserRole userRole) {
        return Map.of("id", GroupURN.urnFromRole(groupUrnPrefix, userRole.getRole()));
    }

}
