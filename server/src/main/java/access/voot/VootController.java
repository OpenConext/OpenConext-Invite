package access.voot;

import access.model.*;
import access.provision.scim.GroupURN;
import access.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/api/voot", produces = MediaType.APPLICATION_JSON_VALUE)
@Transactional
public class VootController {

    private final UserRepository userRepository;
    private final String groupUrnPrefix;

    public VootController(UserRepository userRepository, @Value("${voot.group_urn_domain}") String groupUrnPrefix) {
        this.userRepository = userRepository;
        this.groupUrnPrefix = groupUrnPrefix;
    }

    @GetMapping("/{unspecified_id}")
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
        res.put("urn", GroupURN.urnFromRole(groupUrnPrefix, userRole.getRole()));
        res.put("name", role.getName());
        return res;
    }

}
