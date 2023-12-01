package access.voot;

import access.model.Role;
import access.model.User;
import access.model.UserRole;
import access.provision.scim.GroupURN;
import access.repository.UserRepository;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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

import static access.SwaggerOpenIdConfig.VOOT_SCHEME_NAME;

@RestController
@RequestMapping(value = {"/api/voot", "/api/external/v1/voot"}, produces = MediaType.APPLICATION_JSON_VALUE)
@SecurityRequirement(name = VOOT_SCHEME_NAME)
public class VootController {

    private final UserRepository userRepository;
    private final String groupUrnPrefix;
    private final String teamsNameContext;

    public VootController(UserRepository userRepository,
                          @Value("${voot.group_urn_domain}") String groupUrnPrefix,
                          @Value("${teams.group-name-context}") String teamsNameContext) {
        this.userRepository = userRepository;
        this.groupUrnPrefix = groupUrnPrefix;
        this.teamsNameContext = teamsNameContext;
    }

    @GetMapping("/{unspecified_id}")
    @PreAuthorize("hasRole('VOOT')")
    public ResponseEntity<List<Map<String, String>>> getGroupMemberships(@PathVariable("unspecified_id") String unspecifiedId) {
        Optional<User> optionalUser = userRepository.findBySubIgnoreCase(unspecifiedId);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            user.setLastActivity(Instant.now());
            userRepository.save(user);
            List<Map<String, String>> roles = user.getUserRoles().stream().map(this::parseUserRole).collect(Collectors.toList());
            return ResponseEntity.ok(roles);
        }
        return ResponseEntity.ok(Collections.emptyList());
    }

    private Map<String, String> parseUserRole(UserRole userRole) {
        Map<String, String> res = new HashMap<>();
        Role role = userRole.getRole();
        String urn = role.isTeamsOrigin() ? GroupURN.teamsUrnFromRole(teamsNameContext, role) : GroupURN.urnFromRole(groupUrnPrefix, role);
        res.put("urn", urn);
        res.put("name", role.getName());
        return res;
    }

}
