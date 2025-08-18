package invite.profile;

import invite.manage.Manage;
import invite.model.Authority;
import invite.model.Role;
import invite.model.User;
import invite.model.UserRole;
import invite.repository.UserRepository;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static invite.SwaggerOpenIdConfig.BASIC_AUTHENTICATION_SCHEME_NAME;

@RestController
@RequestMapping(value = {"/api/external/v1/profile"}, produces = MediaType.APPLICATION_JSON_VALUE)
@SecurityRequirement(name = BASIC_AUTHENTICATION_SCHEME_NAME)
public class ProfileController {

    private static final Log LOG = LogFactory.getLog(ProfileController.class);

    private final UserRepository userRepository;
    private final Manage manage;

    public ProfileController(UserRepository userRepository,
                             Manage manage) {
        this.userRepository = userRepository;
        this.manage = manage;
    }

    @GetMapping(value = "")
    @PreAuthorize("hasRole('PROFILE')")
    public ResponseEntity<List<UserRoleProfile>> roles(@RequestParam("collabPersonId") String collabPersonId) {
        LOG.debug("Fetch profile roles for:" + collabPersonId);

        Optional<User> optionalUser = userRepository.findBySubIgnoreCase(collabPersonId);
        Set<UserRole> userRoles = optionalUser.map(User::getUserRoles).orElse(Collections.emptySet());
        List<Role> roles = userRoles.stream()
                .filter(userRole -> userRole.getAuthority().equals(Authority.GUEST) || userRole.isGuestRoleIncluded())
                .map(UserRole::getRole).toList();
        return ResponseEntity.ok(
                manage.addManageMetaData(roles).stream().
                        map(this::userRoleProfile)
                        .toList()
        );
    }

    private UserRoleProfile userRoleProfile(Role role) {
        return new UserRoleProfile(
                role.getName(),
                role.getDescription(),
                applicationInfoList(role));
    }

    private List<ApplicationInfo> applicationInfoList(Role role) {
        return role.getApplicationMaps().stream().map(applicationMap -> new ApplicationInfo(
                (String) applicationMap.get("landingPage"),
                (String) applicationMap.get("name:en"),
                (String) applicationMap.get("name:nl"),
                (String) applicationMap.get("OrganizationName:en"),
                (String) applicationMap.get("OrganizationName:nl"),
                (String) applicationMap.get("logo")
        )).toList();

    }

}
