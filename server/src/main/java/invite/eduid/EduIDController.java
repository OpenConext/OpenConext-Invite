package invite.eduid;

import invite.model.Authority;
import invite.model.Role;
import invite.model.UserRole;
import invite.repository.RoleRepository;
import invite.repository.UserRoleRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static invite.SwaggerOpenIdConfig.BASIC_AUTHENTICATION_SCHEME_NAME;

@RestController
@RequestMapping(value = {"/api/external/v1/eduid"}, produces = MediaType.APPLICATION_JSON_VALUE)
@SecurityRequirement(name = BASIC_AUTHENTICATION_SCHEME_NAME)
public class EduIDController {

    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;

    public EduIDController(RoleRepository roleRepository, UserRoleRepository userRoleRepository) {
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
    }

    @GetMapping("/guests/{organizationGUID}")
    @PreAuthorize("hasRole('EDUID')")
    @Transactional(readOnly = true)
    @Operation(summary = "Get all guests of the roles of an organization",
            description = "Returns for every user with the GUEST authority in a role of the organization the " +
                    "eduperson_principal_name and the names of the roles")
    public ResponseEntity<List<Map<String, Object>>> guestsByOrganizationGUID(@PathVariable("organizationGUID") String organizationGUID) {
        List<Role> roles = roleRepository.findByOrganizationGUID(organizationGUID);
        if (roles.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }
        List<UserRole> userRoles = userRoleRepository.findByRoleInAndAuthority(roles, Authority.GUEST);
        Map<String, List<String>> roleNamesByEppn = userRoles.stream()
                .collect(Collectors.groupingBy(
                        userRole -> userRole.getUser().getEduPersonPrincipalName(),
                        LinkedHashMap::new,
                        Collectors.mapping(userRole -> userRole.getRole().getName(), Collectors.toList())));
        List<Map<String, Object>> result = new ArrayList<>();
        roleNamesByEppn.forEach((eppn, roleNames) -> {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("eduperson_principal_name", eppn);
            entry.put("roles", roleNames);
            result.add(entry);
        });
        return ResponseEntity.ok(result);
    }
}
