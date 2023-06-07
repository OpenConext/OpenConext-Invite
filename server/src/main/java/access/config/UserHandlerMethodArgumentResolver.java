package access.config;

import access.exception.UserRestrictionException;
import access.model.User;
import access.repository.UserRepository;
import access.secuirty.SuperAdmin;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.security.Principal;
import java.util.Map;
import java.util.Optional;

public class UserHandlerMethodArgumentResolver implements HandlerMethodArgumentResolver {

    private final UserRepository userRepository;
    private final SuperAdmin superAdmin;

    public UserHandlerMethodArgumentResolver(UserRepository userRepository, SuperAdmin superAdmin) {
        this.userRepository = userRepository;
        this.superAdmin = superAdmin;
    }

    public boolean supportsParameter(MethodParameter methodParameter) {
        return methodParameter.getParameterType().equals(User.class);
    }

    public User resolveArgument(MethodParameter methodParameter,
                                ModelAndViewContainer mavContainer,
                                NativeWebRequest webRequest,
                                WebDataBinderFactory binderFactory) {
        return getUser(webRequest, true);
    }

    private User getUser(NativeWebRequest webRequest, boolean retry) {
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
        Optional<User> optionalUser = userRepository.findBySubIgnoreCase(sub);
        return optionalUser.or(() ->
                superAdmin.getUsers().stream().filter(adminSub -> adminSub.equals(sub))
                        .findFirst()
                        .map(adminSub -> {
                            try {
                                return userRepository.save(new User(true, attributes));
                            } catch (DataIntegrityViolationException e) {
                                //Can happen in race condition in development mode
                                //See https://legacy.reactjs.org/docs/strict-mode.html#ensuring-reusable-state
                                if (retry) {
                                    return getUser(webRequest, false);
                                } else {
                                    throw e;
                                }
                            }
                        })
        ).or(() -> {
            String requestURI = ((ServletWebRequest) webRequest).getRequest().getRequestURI();
            //After additional validations, this user will be persisted in the InvitationController#accept
            if (requestURI.startsWith("/api/v1/invitations/accept") || requestURI.startsWith("/api/v1/users/config")) {
                return Optional.of(new User(false, attributes));
            } else {
                return Optional.empty();
            }
        }).orElseThrow(UserRestrictionException::new);
    }
}