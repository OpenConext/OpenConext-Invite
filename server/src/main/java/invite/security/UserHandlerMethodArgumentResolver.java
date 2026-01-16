package invite.security;

import invite.config.HashGenerator;
import invite.exception.UserRestrictionException;
import invite.manage.Manage;
import invite.model.APIToken;
import invite.model.User;
import invite.repository.APITokenRepository;
import invite.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static invite.security.InstitutionAdmin.INSTITUTION_ADMIN;
import static invite.security.SecurityConfig.API_TOKEN_HEADER;

public class UserHandlerMethodArgumentResolver implements HandlerMethodArgumentResolver {

    private final UserRepository userRepository;
    private final APITokenRepository apiTokenRepository;
    private final SuperAdmin superAdmin;
    private final Manage manage;

    public UserHandlerMethodArgumentResolver(UserRepository userRepository,
                                             APITokenRepository apiTokenRepository,
                                             SuperAdmin superAdmin,
                                             Manage manage) {
        this.userRepository = userRepository;
        this.apiTokenRepository = apiTokenRepository;
        this.superAdmin = superAdmin;
        this.manage = manage;
    }

    public boolean supportsParameter(MethodParameter methodParameter) {
        return methodParameter.getParameterType().equals(User.class);
    }

    @SuppressWarnings("unchecked")
    public User resolveArgument(MethodParameter methodParameter,
                                ModelAndViewContainer mavContainer,
                                NativeWebRequest webRequest,
                                WebDataBinderFactory binderFactory) {
        Principal userPrincipal = webRequest.getUserPrincipal();
        Map<String, Object> attributes;

        String apiTokenHeader = webRequest.getHeader(API_TOKEN_HEADER);

        if (userPrincipal instanceof BearerTokenAuthentication bearerTokenAuthentication) {
            //The user has logged in and obtained an access_token. Invite is acting as an API resource server
            attributes = bearerTokenAuthentication.getTokenAttributes();
        } else if (userPrincipal instanceof OAuth2AuthenticationToken authenticationToken) {
            //The user has logged in with OpenIDConnect. Invite is acting as a backend server
            attributes = authenticationToken.getPrincipal().getAttributes();
        } else if (StringUtils.hasText(apiTokenHeader)) {
            //The user has an API token (from her institution admin) and there is no state
            String hashedToken = HashGenerator.hashToken(apiTokenHeader);
            APIToken apiToken = apiTokenRepository.findByHashedValue(hashedToken)
                    .orElseThrow(UserRestrictionException::new);
            List<User> apiUsers = new ArrayList<>();
            User owner = apiToken.getOwner();
            String organizationGUID = apiToken.getOrganizationGUID();
            if (owner != null) {
                apiUsers.add(apiToken.getOwner());
            } else {
                //backward compatibility for tokens without an owner or superusers tokens without organizationGUID
                if (StringUtils.hasText(organizationGUID)) {
                    apiUsers.addAll(userRepository.findByOrganizationGUIDAndInstitutionAdmin(organizationGUID, true));
                } else if (apiToken.isSuperUserToken()) {
                    apiUsers.addAll(userRepository.findBySuperUserTrue());
                }
            }
            if (apiUsers.isEmpty()) {
                //we don't want to return null as this is not part of the happy-path
                throw new UserRestrictionException();
            }
            //For old superuser and institution admin tokens it does not make any difference security-wise which user we return
            //For inviters and managers (and all new tokens after 0.0.36 release) the user is linked to the API token
            User user = apiUsers.getFirst();
            if (StringUtils.hasText(organizationGUID)) {
                //The overhead is needed / justified for API usage as this are stateless
                addInstitutionAdminAttributes(user, organizationGUID, Map.of());
            }
            return user;
        } else {
            //The user is not authenticated, but that is part of the accept invitation flow or Basic API auth. Do not throw any Exception
            return null;
        }

        String sub = attributes.get("sub").toString();
        AtomicBoolean validImpersonation = new AtomicBoolean(false);
        Optional<User> optionalUser = userRepository.findBySubIgnoreCase(sub)
                .or(() ->
                        //Provision super-admin users on the fly
                        superAdmin.getUsers().stream().filter(adminSub -> adminSub.equals(sub))
                                .findFirst()
                                .map(adminSub -> userRepository.save(new User(true, attributes)))
                )
                .or(() -> {
                    if ((boolean) attributes.get(INSTITUTION_ADMIN)) {
                        //Provision institution-admins on the fly as they do not need an invitation
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
        HttpServletRequest request = ((ServletWebRequest) webRequest).getRequest();
        String requestURI = request.getRequestURI();
        if (optionalUser.isEmpty() && requestURI.equals("/api/v1/users/config")) {
            return new User(attributes);
        }
        return optionalUser.map(user -> {
            if (user.isInstitutionAdmin() && StringUtils.hasText(user.getOrganizationGUID())) {
                String organizationGUID = user.getOrganizationGUID();
                if (validImpersonation.get()) {
                    //The overhead for retrieving data from manage is justified when super_user is impersonating institutionAdmin
                    addInstitutionAdminAttributes(user, organizationGUID, attributes);
                } else {
                    user.updateRemoteAttributes(attributes);
                }
            }
            return user;
        }).orElseThrow(UserRestrictionException::new);

    }

    private void addInstitutionAdminAttributes(User user, String organizationGUID, Map<String, Object> claims) {
        Map<String, Object> attributes = manage.enrichInstitutionAdmin(organizationGUID, claims);
        user.updateRemoteAttributes(attributes);
    }

}