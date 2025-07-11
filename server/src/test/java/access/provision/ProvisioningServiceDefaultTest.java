package access.provision;

import access.AbstractTest;
import access.eduid.EduIDProvision;
import access.exception.RemoteException;
import access.model.*;
import access.provision.scim.OperationType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

class ProvisioningServiceDefaultTest extends AbstractTest {

    @Autowired
    private ProvisioningService provisioningService;

    @Test
    void newUserRequest() throws JsonProcessingException {
        User user = userRepository.findBySubIgnoreCase(GUEST_SUB).get();
        //See server/src/main/resources/manage/provisioning.json, applicationId="3"
        this.stubForManageProvisioning(List.of("3"));
        String remoteScimIdentifier = this.stubForCreateEvaUser();
        provisioningService.newUserRequest(user);
        List<RemoteProvisionedUser> remoteProvisionedUsers = remoteProvisionedUserRepository.findAll();
        assertEquals(1, remoteProvisionedUsers.size());
        assertEquals(remoteScimIdentifier, remoteProvisionedUsers.get(0).getRemoteIdentifier());
    }

    @Test
    void newUserRequestWithInvalidRemoteResponse() throws JsonProcessingException {
        User user = userRepository.findBySubIgnoreCase(GUEST_SUB).get();
        //We will return a SCIM provisioning app
        this.stubForManageProvisioning(List.of("1"));
        this.stubForCreateScimUser("");
        assertThrows(RemoteException.class, () -> provisioningService.newUserRequest(user));
    }

    @Test
    void newUserRequestWithEduIDProvisioning() throws JsonProcessingException {
        User user = userRepository.findBySubIgnoreCase(GUEST_SUB).get();

        this.stubForManageProvisioning(List.of("1"));

        EduIDProvision eduIDProvision = new EduIDProvision(user.getEduId(), UUID.randomUUID().toString());
        stubFor(post(urlPathMatching("/myconext/api/invite/provision-eduid")).willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(objectMapper.writeValueAsString(eduIDProvision))));

        String remoteScimIdentifier = this.stubForCreateScimUser();
        provisioningService.newUserRequest(user);
        List<RemoteProvisionedUser> remoteProvisionedUsers = remoteProvisionedUserRepository.findAll();
        assertEquals(1, remoteProvisionedUsers.size());
        assertEquals(remoteScimIdentifier, remoteProvisionedUsers.get(0).getRemoteIdentifier());
    }

    @Test
    void updateUserRequest() throws JsonProcessingException {
        User user = userRepository.findBySubIgnoreCase(GUEST_SUB).get();
        //Need to ensure the user is updated, therefore the remote needs to exists and provisioning is scimn
        String remoteScimIdentifier = UUID.randomUUID().toString();
        RemoteProvisionedUser remoteProvisionedUser = new RemoteProvisionedUser(user, remoteScimIdentifier, "7");
        remoteProvisionedUserRepository.save(remoteProvisionedUser);
        this.stubForManageProvisioning(List.of("1", "4", "5"));
        this.stubForUpdateScimUser();
        provisioningService.updateUserRequest(user);
        List<LoggedRequest> loggedRequests = findAll(putRequestedFor(urlPathMatching(String.format("/api/scim/v2/Users/(.*)"))));

        assertEquals(1, loggedRequests.size());
        Map<String, Object> userRequest = objectMapper.readValue(loggedRequests.get(0).getBodyAsString(), Map.class);
        assertEquals(remoteScimIdentifier, userRequest.get("id"));
    }

    @Test
    void deleteUserRequest() throws JsonProcessingException {
        User user = userRepository.findBySubIgnoreCase(GUEST_SUB).get();

        //Mock that this user was provisioned earlier
        RemoteProvisionedUser remoteProvisionedUser = new RemoteProvisionedUser(user, UUID.randomUUID().toString(), "10");
        remoteProvisionedUserRepository.save(remoteProvisionedUser);

        this.stubForManageProvisioning(List.of("3"));
        this.stubForDeleteEvaUser();

        provisioningService.deleteUserRequest(user);
        List<RemoteProvisionedUser> remoteProvisionedUsers = remoteProvisionedUserRepository.findAll();
        assertEquals(0, remoteProvisionedUsers.size());
    }

    @Test
    void deleteUserRoleRequest() throws JsonProcessingException {
        User user = userRepository.findBySubIgnoreCase(GUEST_SUB).get();

        //Mock that this user was provisioned earlier
        RemoteProvisionedUser remoteProvisionedUser = new RemoteProvisionedUser(user, UUID.randomUUID().toString(), "10");
        remoteProvisionedUserRepository.save(remoteProvisionedUser);

        this.stubForManageProvisioning(List.of("3"));
        this.stubForDeleteEvaUser();

        UserRole userRole = user.getUserRoles().stream().filter(ur -> ur.getRole().getName().equals("Storage"))
                .findFirst()
                .get();
        provisioningService.deleteUserRoleRequest(userRole);
        List<RemoteProvisionedUser> remoteProvisionedUsers = remoteProvisionedUserRepository.findAll();
        assertEquals(0, remoteProvisionedUsers.size());
    }

    @Test
    void updateUserRoleRequest() throws JsonProcessingException {
        User user = userRepository.findBySubIgnoreCase(GUEST_SUB).get();

        //Mock that this user was provisioned earlier
        String id = UUID.randomUUID().toString();
        RemoteProvisionedUser remoteProvisionedUser = new RemoteProvisionedUser(user, id, "10");
        remoteProvisionedUserRepository.save(remoteProvisionedUser);

        this.stubForManageProvisioning(List.of("3"));
        this.stubForUpdateEvaUser();

        UserRole userRole = user.getUserRoles().stream().filter(ur -> ur.getRole().getName().equals("Storage"))
                .findFirst()
                .get();
        provisioningService.updateUserRoleRequest(userRole);
        List<LoggedRequest> loggedRequests = findAll(postRequestedFor(urlPathMatching("/eva/api/v1/guest/create")));
        assertEquals(1, loggedRequests.size());
        assertTrue(loggedRequests.get(0).getBodyAsString().contains(id));

    }


    @Test
    void deleteGraphUserRequest() throws JsonProcessingException {
        User user = userRepository.findBySubIgnoreCase(GUEST_SUB).get();

        //Mock that this user was provisioned earlier
        RemoteProvisionedUser remoteProvisionedUser = new RemoteProvisionedUser(user, UUID.randomUUID().toString(), "9");
        remoteProvisionedUserRepository.save(remoteProvisionedUser);

        this.stubForManageProvisioning(List.of("2", "6"));
        this.stubForDeleteGraphUser();

        provisioningService.deleteUserRequest(user);
        List<RemoteProvisionedUser> remoteProvisionedUsers = remoteProvisionedUserRepository.findAll();
        assertEquals(0, remoteProvisionedUsers.size());
    }

    @Test
    void updateGroupRequest() {
        //We only provision GUEST users
        provisioningService.updateGroupRequest(new UserRole(Authority.INVITER, null), OperationType.Add);
    }

    @Test
    void deleteGroupRequest() throws JsonProcessingException {
        Role role = roleRepository.findByName("Calendar").get();

        RemoteProvisionedGroup remoteProvisionedGroup = new RemoteProvisionedGroup(role, UUID.randomUUID().toString(), "7");
        remoteProvisionedGroupRepository.save(remoteProvisionedGroup);

        this.stubForManageProvisioning(List.of("1"));
        this.stubForDeleteScimRole();

        provisioningService.deleteGroupRequest(role);
        Optional<RemoteProvisionedGroup> remoteProvisionedGroupOptional = remoteProvisionedGroupRepository.findByManageProvisioningIdAndRole("7", role);
        assertTrue(remoteProvisionedGroupOptional.isEmpty());
    }

    @Test
    void updateEvaUserRequest() throws JsonProcessingException {
        User user = userRepository.findBySubIgnoreCase(GUEST_SUB).get();
        //See server/src/main/resources/manage/provisioning.json, applicationId="3"
        this.stubForManageProvisioning(List.of("3"));
        String remoteScimIdentifier = this.stubForCreateEvaUser();
        provisioningService.newUserRequest(user);

        //Change the name and do update
        user.setName("Ely Doe");
        provisioningService.updateUserRequest(user);

        List<RemoteProvisionedUser> remoteProvisionedUsers = remoteProvisionedUserRepository.findAll();
        assertEquals(1, remoteProvisionedUsers.size());
        assertEquals(remoteScimIdentifier, remoteProvisionedUsers.get(0).getRemoteIdentifier());

        List<LoggedRequest> requests = findAll(postRequestedFor(urlPathMatching("/eva/api/v1/guest/create")));
        assertEquals(2, requests.size());
        String updateRequest = requests.getLast().getBodyAsString();
        //After URLEncoding
        assertTrue(updateRequest.contains("name=Ely+Doe"));
        assertTrue(updateRequest.contains(String.format("id=%s", remoteScimIdentifier)));
    }

}