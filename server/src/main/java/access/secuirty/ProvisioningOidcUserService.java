package access.secuirty;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

@Service
@EnableConfigurationProperties(SuperAdmin.class)
public class ProvisioningOidcUserService extends OidcUserService {

    private final SuperAdmin superAdmin;

    public ProvisioningOidcUserService(SuperAdmin superAdmin) {
        this.superAdmin = superAdmin;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);

        //Check if the user exist as super admin otherwise provision, do nothing otherwise
        return oidcUser;
    }
}
