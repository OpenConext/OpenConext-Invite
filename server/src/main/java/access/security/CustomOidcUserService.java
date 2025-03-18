package access.security;

import access.manage.Manage;
import access.model.User;
import access.provision.ProvisioningService;
import access.repository.UserRepository;
import lombok.Getter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static access.security.InstitutionAdmin.*;

public class CustomOidcUserService implements OAuth2UserService<OidcUserRequest, OidcUser> {

    private static final Log LOG = LogFactory.getLog(CustomOidcUserService.class);

    @Getter
    private final Manage manage;
    private final ProvisioningService provisioningService;
    private final UserRepository userRepository;
    private final String entitlement;
    private final String organizationGuidPrefix;
    private final OidcUserService delegate;

    public CustomOidcUserService(Manage manage,
                                 UserRepository userRepository,
                                 ProvisioningService provisioningService,
                                 String entitlement,
                                 String organizationGuidPrefix) {
        this.manage = manage;
        this.userRepository = userRepository;
        this.provisioningService = provisioningService;
        this.entitlement = entitlement;
        this.organizationGuidPrefix = organizationGuidPrefix;
        delegate = new OidcUserService();
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        // Delegate to the default implementation for loading a user
        OidcUser oidcUser = delegate.loadUser(userRequest);
        Map<String, Object> claims = oidcUser.getUserInfo().getClaims();
        // We need a mutable Map instead of the returned immutable Map
        Map<String, Object> newClaims = new HashMap<>(claims);

        String sub = (String) newClaims.get("sub");
        Optional<User> optionalUser = userRepository.findBySubIgnoreCase(sub);
        boolean institutionAdmin = isInstitutionAdmin(claims, entitlement) ||
                (optionalUser.isPresent() && isInstitutionAdmin(optionalUser.get()));
        newClaims.put(INSTITUTION_ADMIN, institutionAdmin);

        String organizationGuid = institutionAdmin ? getOrganizationGuid(claims, organizationGuidPrefix, optionalUser)
                .orElse(null) : null;
        newClaims.put(ORGANIZATION_GUID, organizationGuid);

        if (institutionAdmin && StringUtils.hasText(organizationGuid)) {
            Map<String, Object> manageClaims = manage.enrichInstitutionAdmin(organizationGuid);
            newClaims.putAll(manageClaims);
        }
        optionalUser.ifPresent(user -> {
            boolean changed = user.updateAttributes(newClaims);
            if (changed) {
                try {
                    provisioningService.updateUserRequest(user);
                } catch (RuntimeException e) {
                    //We choose to ignore these, because remote provisioning errors may not prevent login flow
                    LOG.error("Error in updateUserRequest", e);
                }

            }
            userRepository.save(user);
        });
        OidcUserInfo oidcUserInfo = new OidcUserInfo(newClaims);
        return new DefaultOidcUser(oidcUser.getAuthorities(), oidcUser.getIdToken(), oidcUserInfo);
    }
}
