package access.lifecycle;

import access.model.User;
import access.model.UserRole;
import access.provision.ProvisioningService;
import access.repository.UserRepository;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

import static access.SwaggerOpenIdConfig.BASIC_AUTHENTICATION_SCHEME_NAME;

@RestController
@RequestMapping(value = {"/api/deprovision", "/api/external/v1/deprovision"}, produces = MediaType.APPLICATION_JSON_VALUE)
@SecurityRequirement(name = BASIC_AUTHENTICATION_SCHEME_NAME)
public class UserLifeCycleController {

    private static final Logger LOG = LoggerFactory.getLogger(UserLifeCycleController.class);

    private final UserRepository userRepository;
    private final ProvisioningService provisioningService;

    @Autowired
    public UserLifeCycleController(UserRepository userRepository, ProvisioningService provisioningService) {
        this.userRepository = userRepository;
        this.provisioningService = provisioningService;
    }

    @RequestMapping(method = RequestMethod.GET, value = "/{userId:.+}")
    @PreAuthorize("hasRole('LIFECYCLE')")
    public LifeCycleResult preview(@PathVariable String userId, Authentication authentication) {
        LOG.info("Request for lifecycle preview for {} by {}", userId, authentication.getPrincipal());

        return doDryRun(userId, true);
    }

    @RequestMapping(method = RequestMethod.DELETE, value = "/{userId:.+}/dry-run")
    @PreAuthorize("hasRole('LIFECYCLE')")
    public LifeCycleResult dryRun(@PathVariable String userId, Authentication authentication) {
        LOG.info("Request for lifecycle dry-run for {} by {}", userId, authentication.getPrincipal());

        return doDryRun(userId, true);
    }

    @RequestMapping(method = RequestMethod.DELETE, value = "/{userId:.+}")
    @PreAuthorize("hasRole('LIFECYCLE')")
    @Transactional
    public LifeCycleResult deprovision(@PathVariable String userId, Authentication authentication) {
        LOG.info("Request for lifecycle deprovision for {} by {}", userId, authentication.getPrincipal());

        return doDryRun(userId, false);
    }

    private LifeCycleResult doDryRun(String userId, boolean dryRun) {
        LifeCycleResult result = new LifeCycleResult();
        Optional<User> optionalUser = this.userRepository.findBySubIgnoreCase(userId);
        if (optionalUser.isEmpty()) {
            return result;
        }
        User user = optionalUser.get();
        List<Attribute> attributes = new ArrayList<>();
        attributes.add(new Attribute("email", user.getEmail()));
        attributes.add(new Attribute("eduPersonPrincipalName", user.getEduPersonPrincipalName()));
        attributes.add(new Attribute("schacHomeOrganization", user.getSchacHomeOrganization()));
        attributes.add(new Attribute("name", user.getName()));
        attributes.add(new Attribute("urn", user.getSub()));
        attributes.add(new Attribute("lastLoginDate", user.getLastActivity().toString()));
        Set<UserRole> userRoles = user.getUserRoles();
        userRoles.forEach(userRole -> attributes.add(new Attribute("membership", userRole.getRole().getName())));
        if (!dryRun) {
            this.provisioningService.deleteUserRequest(user);
            userRepository.delete(user);
        }
        result.setData(attributes.stream()
                .filter(attr -> StringUtils.hasText(attr.getValue()))
                .sorted(Comparator.comparing(Attribute::getName))
                .toList());
        return result;
    }

}
