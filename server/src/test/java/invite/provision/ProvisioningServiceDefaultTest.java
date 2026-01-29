package invite.provision;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import invite.AbstractTest;
import invite.exception.RemoteException;
import invite.model.Authority;
import invite.model.RemoteProvisionedGroup;
import invite.model.RemoteProvisionedUser;
import invite.model.Role;
import invite.model.User;
import invite.model.UserRole;
import invite.provision.scim.OperationType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

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

        String eduIdForInstitution = UUID.randomUUID().toString();
        super.stubForProvisionEduID(eduIdForInstitution);

        String remoteScimIdentifier = this.stubForCreateScimUser();
        provisioningService.newUserRequest(user);
        List<ServeEvent> events = this.mockServer.getAllServeEvents();
        ServeEvent serveEvent = events.stream()
                .filter(event -> event.getRequest().getUrl().equalsIgnoreCase("/api/scim/v2/Users"))
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
        //UserRequest can not be deserialized from String due to missing constructors and setters
        Map<String, Object> userRequest = objectMapper.readValue(serveEvent.getRequest().getBodyAsString(), new TypeReference<>() {
        });
        assertEquals(eduIdForInstitution, userRequest.get("userName"));
        assertEquals(eduIdForInstitution, userRequest.get("externalId"));

        List<RemoteProvisionedUser> remoteProvisionedUsers = remoteProvisionedUserRepository.findAll();
        assertEquals(1, remoteProvisionedUsers.size());
        assertEquals(remoteScimIdentifier, remoteProvisionedUsers.get(0).getRemoteIdentifier());

    }

    @Test
    void newUserRequestWithExternalPlaceholderIdentifier() throws JsonProcessingException {
        User user = userRepository.findBySubIgnoreCase(GUEST_SUB).get();
        String internalPlaceholderIdentifier = UUID.randomUUID().toString();
        user.setInternalPlaceholderIdentifier(internalPlaceholderIdentifier);

        this.stubForManageProvisioning(List.of("4"));

        String remoteScimIdentifier = this.stubForCreateScimUser();
        provisioningService.newUserRequest(user);

        ServeEvent event = getAllServeEvents().stream().filter(e -> e.getRequest().getUrl().equals("/api/scim/v2/Users")).toList().getFirst();
        Map userRequest = objectMapper.readValue(event.getRequest().getBodyAsString(), Map.class);
        assertEquals(internalPlaceholderIdentifier, userRequest.get("id"));

        RemoteProvisionedUser remoteProvisionedUser = remoteProvisionedUserRepository.findByRemoteScimIdentifier(remoteScimIdentifier).get();
        assertEquals(remoteScimIdentifier, remoteProvisionedUser.getRemoteIdentifier());
        assertNotEquals(internalPlaceholderIdentifier, remoteScimIdentifier);
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
        super.stubForProvisionEduID(UUID.randomUUID().toString());
        provisioningService.updateUserRequest(user);
        List<LoggedRequest> loggedRequests = findAll(putRequestedFor(urlPathMatching(String.format("/api/scim/v2/Users/(.*)"))));

        assertEquals(1, loggedRequests.size());
        Map<String, Object> userRequest = objectMapper.readValue(loggedRequests.get(0).getBodyAsString(), Map.class);
        assertEquals(remoteScimIdentifier, userRequest.get("id"));
    }

    @Test
    void updateUserRequestWithEduIDProvisioningError() throws JsonProcessingException {
        User user = userRepository.findBySubIgnoreCase(GUEST_SUB).get();
        //Need to ensure the user is updated, therefore the remote needs to exists and provisioning is scimn
        String remoteScimIdentifier = UUID.randomUUID().toString();
        RemoteProvisionedUser remoteProvisionedUser = new RemoteProvisionedUser(user, remoteScimIdentifier, "7");
        remoteProvisionedUserRepository.save(remoteProvisionedUser);
        this.stubForManageProvisioning(List.of("1", "4", "5"));
        this.stubForUpdateScimUser();
        try {
            provisioningService.updateUserRequest(user);
            fail("Expected RemoteException");
        } catch (RemoteException e) {
            assertTrue(e.getMessage().contains("Error in provisionEduid"));
            assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
            assertNotNull(e.getReference());
        }

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