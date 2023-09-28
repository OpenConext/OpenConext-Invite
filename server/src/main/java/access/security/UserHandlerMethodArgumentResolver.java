package access.security;

import access.exception.UserRestrictionException;
import access.manage.Manage;
import access.model.User;
import access.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static access.security.InstitutionAdmin.*;

public class UserHandlerMethodArgumentResolver implements HandlerMethodArgumentResolver {

    private final UserRepository userRepository;
    private final SuperAdmin superAdmin;
    private final Manage manage;

    public UserHandlerMethodArgumentResolver(UserRepository userRepository, SuperAdmin superAdmin, Manage manage) {
        this.userRepository = userRepository;
        this.superAdmin = superAdmin;
        this.manage = manage;
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
        AtomicBoolean validImpersonation = new AtomicBoolean(false);
        Optional<User> optionalUser = userRepository.findBySubIgnoreCase(sub)
                .or(() ->
                        superAdmin.getUsers().stream().filter(adminSub -> adminSub.equals(sub))
                                .findFirst()
                                .map(adminSub -> userRepository.save(new User(true, attributes)))
                )
                .or(() -> {
                    if ((boolean) attributes.get(INSTITUTION_ADMIN)) {
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
                        validImpersonation.set(true);
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
            if (validImpersonation.get() && user.isInstitutionAdmin() && StringUtils.hasText(user.getOrganizationGUID())) {
                String organizationGUID = user.getOrganizationGUID();
                user.setApplications(manage.providersByInstitutionalGUID(organizationGUID));
                user.setInstitution(manage.identityProviderByInstitutionalGUID(organizationGUID).orElse(Collections.emptyMap()));
            }
            return user;
        }).orElseThrow(UserRestrictionException::new);

    }

}