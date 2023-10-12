package access.provision.graph;

import access.model.Authority;
import access.model.User;
import access.provision.Provisioning;
import access.provision.ProvisioningServiceDefault;
import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.microsoft.graph.authentication.TokenCredentialAuthProvider;
import com.microsoft.graph.http.BaseRequest;
import com.microsoft.graph.models.PasswordProfile;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.requests.UserCollectionRequest;
import okhttp3.Request;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.RequestEntity;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;

public class GraphClient {

    private static final Log LOG = LogFactory.getLog(ProvisioningServiceDefault.class);

    private final String inviteBaseURL;
    private final String welcomeBaseURL;

    public GraphClient(String inviteBaseURL, String welcomeBaseURL) {
        this.inviteBaseURL = inviteBaseURL;
        this.welcomeBaseURL = welcomeBaseURL;
    }

    @SuppressWarnings("unchecked")
    public String newUserRequest(Provisioning provisioning, User user) {
        ClientSecretCredential credential = new ClientSecretCredentialBuilder()
                .clientId(provisioning.getGraphClientId())
                .tenantId(provisioning.getGraphTenant())
                .clientSecret(provisioning.getGraphSecret()).build();

        TokenCredentialAuthProvider authProvider = new TokenCredentialAuthProvider(credential);

        boolean nonGuest = user.getUserRoles().stream().anyMatch(userRole -> userRole.getAuthority().hasHigherRights(Authority.GUEST));
        String baseUrl = nonGuest ? this.inviteBaseURL : this.welcomeBaseURL;
        com.microsoft.graph.models.Invitation invitation = new com.microsoft.graph.models.Invitation();
        invitation.invitedUserEmailAddress = user.getEmail();
        invitation.invitedUserDisplayName = user.getName();
        invitation.inviteRedirectUrl = String.format("%s/ms-accept-return/%s", baseUrl,)
                com.microsoft.graph.models.User graphUser = new com.microsoft.graph.models.User();

        graphUser.accountEnabled = true;
        graphUser.displayName = user.getName();
        graphUser.userPrincipalName = user.getEduPersonPrincipalName();
        graphUser.mailNickname = user.getGivenName();
        graphUser.mail = user.getEmail();
        graphUser.companyName = user.getSchacHomeOrganization();
        graphUser.givenName = user.getGivenName();
        graphUser.surname = user.getFamilyName();

        PasswordProfile passwordProfile = new PasswordProfile();
        passwordProfile.forceChangePasswordNextSignIn = true;
        passwordProfile.password = "xWwvJ]6NMw+bWH-d";
        graphUser.passwordProfile = passwordProfile;
        GraphServiceClient<Request> graphClient = GraphServiceClient.builder().authenticationProvider(authProvider).buildClient();
        graphClient.invitations().buildRequest();

        UserCollectionRequest buildRequest = graphClient.users().buildRequest();
        String graphUrl = provisioning.getGraphUrl();
        if (graphUrl.startsWith("http://") || graphUrl.contains("mock")) {
            Field field = ReflectionUtils.findField(BaseRequest.class, "requestUrl");
            ReflectionUtils.makeAccessible(field);
            ReflectionUtils.setField(field, buildRequest.getBaseRequest(), graphUrl);
        }
        LOG.info(String.format("Send CreateUser Graph request to %s for provisioning %s for user %s",
                buildRequest.getBaseRequest().getRequestUrl(),
                provisioning.getGraphClientId(),
                user.getEduPersonPrincipalName()));
        com.microsoft.graph.models.User createdUser = buildRequest.post(graphUser);
        return createdUser.id;

    }
}
