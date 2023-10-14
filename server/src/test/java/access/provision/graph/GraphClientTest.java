package access.provision.graph;

import access.AbstractTest;
import access.manage.LocalManage;
import access.provision.Provisioning;
import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.graph.authentication.TokenCredentialAuthProvider;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.requests.UserRequest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;

import static access.Seed.INVITER_SUB;

class GraphClientTest extends AbstractTest  {

    @Test
    @Disabled
    void newUserRequest() {
        GraphClient graphClient = new GraphClient("http://localhost:8080", "test.eduid.nl");
        LocalManage localManage = new LocalManage(new ObjectMapper(), true);
        Provisioning provisioning = new Provisioning(localManage.provisioning(List.of("2")).get(0));

        access.model.User user = new access.model.User();
        user.setEmail("99c1d1a6-4b4c-42b4-94de-4b517c3ec7c5@test.eduid.nl");
        user.setName("Mijn Broer");
        user.setSub(INVITER_SUB);
        GraphResponse graphResponse = graphClient.newUserRequest(provisioning, user);
        System.out.println(graphResponse);
        ClientSecretCredential credential = new ClientSecretCredentialBuilder()
                .clientId(provisioning.getGraphClientId())
                .tenantId(provisioning.getGraphTenant())
                .clientSecret(provisioning.getGraphSecret()).build();

        TokenCredentialAuthProvider authProvider = new TokenCredentialAuthProvider(credential);

        GraphServiceClient graphServiceClient = GraphServiceClient.builder().authenticationProvider( authProvider ).buildClient();

        com.microsoft.graph.models.User graphUser = graphServiceClient.users(graphResponse.remoteIdentifier())
                .buildRequest()
                .get();
        System.out.println(graphUser);

        UserRequest userRequest = graphServiceClient.users(graphUser.id).buildRequest();
        graphUser.mail = "jdo.eplokij@gmail.com";
        userRequest.patch(graphUser);

        graphUser = graphServiceClient.users(graphResponse.remoteIdentifier())
                .buildRequest()
                .get();
        System.out.println(graphUser.mail);
    }
}