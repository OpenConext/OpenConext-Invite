package access.provision.graph;

import access.exception.RemoteException;
import access.model.User;
import access.provision.Provisioning;
import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.microsoft.graph.authentication.TokenCredentialAuthProvider;
import com.microsoft.graph.core.ClientException;
import com.microsoft.graph.http.BaseRequest;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.requests.InvitationCollectionRequest;
import com.microsoft.graph.requests.UserRequest;
import okhttp3.Request;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.HttpStatus;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;

public class GraphClient {

    private static final Log LOG = LogFactory.getLog(GraphClient.class);

    private final String serverUrl;
    private final String eduidIdpSchacHomeOrganization;

    public GraphClient(String serverUrl, String eduidIdpSchacHomeOrganization) {
        this.serverUrl = serverUrl;
        this.eduidIdpSchacHomeOrganization = eduidIdpSchacHomeOrganization;
    }

    @SuppressWarnings("unchecked")
    public GraphResponse newUserRequest(Provisioning provisioning, User user) {
        GraphServiceClient<Request> graphServiceClient = getRequestGraphServiceClient(provisioning);

        InvitationCollectionRequest buildRequest = graphServiceClient.invitations().buildRequest();

        String graphUrl = provisioning.getGraphUrl();
        replaceGraphUrl(graphUrl, buildRequest.getBaseRequest());

        com.microsoft.graph.models.Invitation invitation = new com.microsoft.graph.models.Invitation();
        invitation.invitedUserEmailAddress = eduidIdpSchacHomeOrganization.equalsIgnoreCase(user.getSchacHomeOrganization()) ? user.getEduPersonPrincipalName() : user.getEmail();
        invitation.invitedUserDisplayName = user.getName();
        invitation.inviteRedirectUrl = String.format("%s/api/v1/invitations/ms-accept-return/%s/%s",
                serverUrl, provisioning.getId(), user.getId());
        invitation.sendInvitationMessage = false;
        invitation.invitedUserType = "Guest";

        LOG.info(String.format("Send CreateUser Graph request to %s for provisioning %s for user %s",
                buildRequest.getBaseRequest().getRequestUrl(),
                provisioning.getGraphClientId(),
                user.getEduPersonPrincipalName()));
        try {
            com.microsoft.graph.models.Invitation newInvitation = buildRequest.post(invitation);

            LOG.info(String.format("Response from graph endpoint for user %s, inviteRedeemUrl: %s",
                    user.getEmail(),
                    newInvitation.inviteRedeemUrl
            ));
            return new GraphResponse(newInvitation.invitedUser.id, newInvitation.inviteRedeemUrl);
        } catch (ClientException e) {
            String errorMessage = String.format("Error Graph request (entityID %s) to %s for user %s",
                    provisioning.getEntityId(),
                    graphUrl,
                    user.getEmail());
            throw new RemoteException(HttpStatus.BAD_REQUEST, errorMessage, e);
        }
    }

    public void updateUserRequest(User user, Provisioning provisioning, String remoteUserIdentifier) {
        GraphServiceClient<Request> graphServiceClient = getRequestGraphServiceClient(provisioning);
        UserRequest userRequest = graphServiceClient.users(remoteUserIdentifier).buildRequest();

        String graphUrl = provisioning.getGraphUrl();
        replaceGraphUrl(graphUrl, userRequest);
        try {
            com.microsoft.graph.models.User graphUser = userRequest.get();

            graphUser.mail = user.getEmail();
            userRequest.patch(graphUser);
        } catch (ClientException e) {
            String errorMessage = String.format("Error Graph request (entityID %s) to %s for user %s",
                    provisioning.getEntityId(),
                    graphUrl,
                    user.getEmail());
            throw new RemoteException(HttpStatus.BAD_REQUEST, errorMessage, e);
        }
    }

    public void deleteUser(User user, Provisioning provisioning, String remoteUserIdentifier) {
        GraphServiceClient<Request> graphServiceClient = getRequestGraphServiceClient(provisioning);
        UserRequest userRequest = graphServiceClient.users(remoteUserIdentifier).buildRequest();

        String graphUrl = provisioning.getGraphUrl();
        replaceGraphUrl(graphUrl, userRequest);

        try {
            userRequest.delete();
        } catch (ClientException e) {
            String errorMessage = String.format("Error Graph delete (entityID %s) to %s for user %s",
                    provisioning.getEntityId(),
                    graphUrl,
                    user.getEmail());
            throw new RemoteException(HttpStatus.BAD_REQUEST, errorMessage, e);
        }

    }

    private GraphServiceClient<Request> getRequestGraphServiceClient(Provisioning provisioning) {
        ClientSecretCredential credential = new ClientSecretCredentialBuilder()
                .clientId(provisioning.getGraphClientId())
                .tenantId(provisioning.getGraphTenant())
                .clientSecret(provisioning.getGraphSecret()).build();

        TokenCredentialAuthProvider authProvider = new TokenCredentialAuthProvider(credential);
        GraphServiceClient<Request> graphClient = GraphServiceClient.builder().authenticationProvider(authProvider).buildClient();
        return graphClient;
    }

    private static void replaceGraphUrl(String graphUrl, BaseRequest buildRequest) {
        //hack to enable testing
        if (StringUtils.hasText(graphUrl) && (graphUrl.startsWith("http://") || graphUrl.contains("mock"))) {
            Field field = ReflectionUtils.findField(BaseRequest.class, "requestUrl");
            ReflectionUtils.makeAccessible(field);
            ReflectionUtils.setField(field, buildRequest, graphUrl);
        }
    }


}
