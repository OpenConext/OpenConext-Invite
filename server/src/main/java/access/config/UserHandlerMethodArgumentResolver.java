package access.config;

import access.exception.UserRestrictionException;
import access.model.User;
import access.repository.UserRepository;
import access.security.InstitutionAdmin;
import access.security.SuperAdmin;
import org.springframework.core.MethodParameter;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class UserHandlerMethodArgumentResolver implements HandlerMethodArgumentResolver {

    private final UserRepository userRepository;
    private final SuperAdmin superAdmin;
    private final InstitutionAdmin institutionAdmin;

    public UserHandlerMethodArgumentResolver(UserRepository userRepository, SuperAdmin superAdmin, InstitutionAdmin institutionAdmin) {
        this.userRepository = userRepository;
        this.superAdmin = superAdmin;
        this.institutionAdmin = institutionAdmin;
    }

    public boolean supportsParameter(MethodParameter methodParameter) {
        return methodParameter.getParameterType().equals(User.class);
    }

    public User resolveArgument(MethodParameter methodParameter,
                                ModelAndViewContainer mavContainer,
                                NativeWebRequest webRequest,
                                WebDataBinderFactory binderFactory) {
        Principal userPrincipal = webRequest.getUserPrincipal();
        Map<String, Object> attributes;

        if (userPrincipal instanceof BearerTokenAuthentication bearerTokenAuthentication) {
            attributes = bearerTokenAuthentication.getTokenAttributes();
        } else if (userPrincipal instanceof OAuth2AuthenticationToken authenticationToken) {
            attributes = authenticationToken.getPrincipal().getAttributes();
        } else {
            return null;
        }

        String sub = attributes.get("sub").toString();
        Optional<User> optionalUser = userRepository.findBySubIgnoreCase(sub)
                .or(() ->
                        superAdmin.getUsers().stream().filter(adminSub -> adminSub.equals(sub))
                                .findFirst()
                                .map(adminSub -> userRepository.save(new User(true, attributes)))
                )
                .or(() -> {
                    if (this.isInstitutionAdmin(attributes)) {
                        User user = new User(attributes);
                        userRepository.save(user);
                        return Optional.of(user);
                    } else {
                        return Optional.empty();
                    }
                })
                .map(user -> {
                    String impersonateId = webRequest.getHeader("X-IMPERSONATE-ID");
                    if (StringUtils.hasText(impersonateId) && user.isSuperUser()) {
                        return userRepository.findById(Long.valueOf(impersonateId))
                                .orElseThrow(UserRestrictionException::new);
                    }
                    return user;
                });
        String requestURI = ((ServletWebRequest) webRequest).getRequest().getRequestURI();
        if (optionalUser.isEmpty() && requestURI.equals("/api/v1/users/config")) {
            return new User(attributes);
        }
        return optionalUser.map(user -> {
            if (user.getId() != null) {
                user.updateAttributes(attributes);
                this.updateUser(user, attributes);
                userRepository.save(user);
            }
            return user;
        }).orElseThrow(UserRestrictionException::new);

    }

    private boolean isInstitutionAdmin(Map<String, Object> attributes) {
        if (attributes.containsKey("eduperson_entitlement")) {
            List<String> entitlements = ((List<String>) attributes.get("eduperson_entitlement"))
                    .stream().map(String::toLowerCase).toList();
            if (entitlements.contains(this.institutionAdmin.getEntitlement().toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private User updateUser(User user, Map<String, Object> attributes) {
        if (attributes.containsKey("eduperson_entitlement")) {
            List<String> entitlements = ((List<String>) attributes.get("eduperson_entitlement"))
                    .stream().map(String::toLowerCase).toList();
            user.setInstitutionAdmin(entitlements.contains(this.institutionAdmin.getEntitlement().toLowerCase()));
            String organizationGUIPrefix = this.institutionAdmin.getOrganizationGuidPrefix().toLowerCase();
            boolean hasOrganizationPrefix = false;
            //lambda requires final variables
            for (String entitlement : entitlements) {
                if (entitlement.startsWith(organizationGUIPrefix)) {
                    user.setOrganizationGUID(entitlement.substring(this.institutionAdmin.getOrganizationGuidPrefix().length()));
                    hasOrganizationPrefix = true;
                    break;
                }
            }
            if (!hasOrganizationPrefix) {
                user.setOrganizationGUID(null);
            }
        } else {
            user.setInstitutionAdmin(false);
            user.setOrganizationGUID(null);
        }
        return user;
    }
}