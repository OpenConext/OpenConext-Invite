package access.provision.scim;

import access.model.User;
import access.provision.Provisioning;
import access.provision.ProvisioningType;
import access.provision.ScimUserIdentifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserRequestTest {

    private User user;

    @BeforeEach
    void beforeEach() {
        this.user = new User(Map.of(
                "sub", "sub",
                "eduperson_principal_name", "eduperson_principal_name",
                "schac_home_organization", "schac_home_organization",
                "email", "email",
                "subject_id", "subject_id",
                "eduid", "eduid",
                "uids", List.of("uid")
        ));
    }

    @Test
    void externalIdEduPersonPrincipalName() {
        Provisioning provisioning = getProvisioning(ScimUserIdentifier.eduperson_principal_name);
        UserRequest userRequest = new UserRequest(user, provisioning);
        assertEquals(user.getEduPersonPrincipalName(), userRequest.getExternalId());
    }

    @Test
    void externalIdEduId() {
        Provisioning provisioning = getProvisioning(ScimUserIdentifier.eduID);
        UserRequest userRequest = new UserRequest(user, provisioning);
        assertEquals(user.getEduId(), userRequest.getExternalId());

        ReflectionTestUtils.setField(user, "eduId", null);
        userRequest = new UserRequest(user, provisioning);
        assertEquals(user.getEduPersonPrincipalName(), userRequest.getExternalId());
    }

    @Test
    void externalIdEmail() {
        Provisioning provisioning = getProvisioning(ScimUserIdentifier.email);
        UserRequest userRequest = new UserRequest(user, provisioning);
        assertEquals(user.getEmail(), userRequest.getExternalId());

        ReflectionTestUtils.setField(user, "email", null);
        userRequest = new UserRequest(user, provisioning);
        assertEquals(user.getEduPersonPrincipalName(), userRequest.getExternalId());
    }

    @Test
    void externalIdUid() {
        Provisioning provisioning = getProvisioning(ScimUserIdentifier.uids);
        UserRequest userRequest = new UserRequest(user, provisioning);
        assertEquals(user.getUid(), userRequest.getExternalId());

        ReflectionTestUtils.setField(user, "uid", null);
        userRequest = new UserRequest(user, provisioning);
        assertEquals(user.getEduPersonPrincipalName(), userRequest.getExternalId());
    }

    @Test
    void externalIdSubjectId() {
        Provisioning provisioning = getProvisioning(ScimUserIdentifier.subject_id);
        UserRequest userRequest = new UserRequest(user, provisioning);
        assertEquals(user.getSubjectId(), userRequest.getExternalId());

        ReflectionTestUtils.setField(user, "subjectId", null);
        userRequest = new UserRequest(user, provisioning);
        assertEquals(user.getEduPersonPrincipalName(), userRequest.getExternalId());
    }

    @Test
    void externalIdDefault() {
        Provisioning provisioning = new Provisioning(Map.of(
                "provisioning_type", ProvisioningType.scim.name(),
                "scim_url", "http://localhost",
                "scim_user", "user",
                "scim_password", "secret"
        ));
        UserRequest userRequest = new UserRequest(user, provisioning);
        assertEquals(user.getEduPersonPrincipalName(), userRequest.getExternalId());
    }

    @Test
    void externalIdAvoidNullPointer() {
        Provisioning provisioning = new Provisioning(Map.of(
                "provisioning_type", ProvisioningType.scim.name(),
                "scim_url", "http://localhost",
                "scim_user", "user",
                "scim_password", "secret"
        ));
        ReflectionTestUtils.setField(provisioning, "scimUserIdentifier", null);
        UserRequest userRequest = new UserRequest(user, provisioning);
        assertEquals(user.getEduPersonPrincipalName(), userRequest.getExternalId());
    }

    private Provisioning getProvisioning(ScimUserIdentifier scimUserIdentifier) {
        return new Provisioning(Map.of(
                "provisioning_type", ProvisioningType.scim.name(),
                "scim_url", "http://localhost",
                "scim_user", "user",
                "scim_password", "secret",
                "scim_user_identifier", scimUserIdentifier.name(),
                "institutionGuid", UUID.randomUUID().toString()
        ));
    }
}