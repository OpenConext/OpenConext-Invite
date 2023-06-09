package access.scim;

import access.manage.Manage;
import access.manage.ManageIdentifier;
import com.fasterxml.jackson.databind.ObjectMapper;
import access.model.*;
import access.mail.MailBox;
import access.repository.*;
import lombok.SneakyThrows;
import okhttp3.OkHttpClient;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class SCIMServiceDefault implements SCIMService {

    public final static String USER_API = "users";
    public final static String GROUP_API = "groups";

    private static final Log LOG = LogFactory.getLog(SCIMServiceDefault.class);

    private final ParameterizedTypeReference<Map<String, Object>> mapParameterizedTypeReference = new ParameterizedTypeReference<>() {
    };

    private final ParameterizedTypeReference<String> stringParameterizedTypeReference = new ParameterizedTypeReference<>() {
    };

    private final RestTemplate restTemplate = new RestTemplate();

    private final UserRoleRepository userRoleRepository;
    private final RemoteProvisionedUserRepository remoteProvisionedUserRepository;
    private final RemoteProvisionedGroupRepository remoteProvisionedGroupRepository;
    private final Manage manage;
    private final ObjectMapper objectMapper;
    private final MailBox mailBox;
    private final String groupUrnPrefix;

    @Autowired
    public SCIMServiceDefault(UserRoleRepository userRoleRepository,
                              RemoteProvisionedUserRepository remoteProvisionedUserRepository,
                              RemoteProvisionedGroupRepository remoteProvisionedGroupRepository,
                              Manage manage,
                              ObjectMapper objectMapper,
                              MailBox mailBox,
                              @Value("${voot.group_urn_domain}") String groupUrnPrefix) {
        this.userRoleRepository = userRoleRepository;
        this.remoteProvisionedUserRepository = remoteProvisionedUserRepository;
        this.remoteProvisionedGroupRepository = remoteProvisionedGroupRepository;
        this.manage = manage;
        this.objectMapper = objectMapper;
        this.mailBox = mailBox;
        this.groupUrnPrefix = groupUrnPrefix;
        // Otherwise, we can't use method PATCH
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.connectTimeout(1, TimeUnit.MINUTES);
        builder.retryOnConnectionFailure(true);
        restTemplate.setRequestFactory(new OkHttp3ClientHttpRequestFactory(builder.build()));
    }

    @Override
    @SneakyThrows
    public void newUserRequest(User user) {
        List<Provisioning> provisionings = getProvisionings(user);
        //Provision the user to all provisionings in Manage where the user is unknown or the ProvisionType is mail
        provisionings.stream()
                .filter(provisioning -> this.remoteProvisionedUserRepository.findByManageProvisioningIdAndUser(provisioning.getId(), user)
                        .isEmpty() || hasEmailHook(provisioning))
                .forEach(provisioning -> {
                    String userRequest = prettyJson(new UserRequest(user));
                    String remoteScimIdentifier = this.newRequest(provisioning, userRequest, USER_API);
                    RemoteProvisionedUser remoteProvisionedUser = new RemoteProvisionedUser(user, remoteScimIdentifier, provisioning.getId());
                    this.remoteProvisionedUserRepository.save(remoteProvisionedUser);
                });
    }

    @Override
    @SneakyThrows
    public void deleteUserRequest(User user) {
        //First send update role requests
        user.getUserRoles()
                .forEach(userRole -> this.doUpdateGroupRequest(userRole, OperationType.Remove, user.getUserRoles()));

        List<Provisioning> provisionings = getProvisionings(user);
        //Delete the user to all provisionings in Manage where the user is known
        provisionings.forEach(provisioning -> {
            Optional<RemoteProvisionedUser> provisionedUserOptional = this.remoteProvisionedUserRepository
                    .findByManageProvisioningIdAndUser(provisioning.getId(), user);
            if (provisionedUserOptional.isPresent()) {
                RemoteProvisionedUser remoteProvisionedUser = provisionedUserOptional.get();
                String remoteScimIdentifier = remoteProvisionedUser.getRemoteScimIdentifier();
                String userRequest = prettyJson(new UserRequest(user, remoteScimIdentifier));
                this.deleteRequest(provisioning, userRequest, USER_API, remoteScimIdentifier);
                this.remoteProvisionedUserRepository.delete(remoteProvisionedUser);
            }
        });
    }

    @Override
    public void newGroupRequest(Role role) {
        doNewGroupRequest(role, Collections.emptyList());
    }

    @Override
    public void updateGroupRequest(UserRole userRole, OperationType operationType) {
        doUpdateGroupRequest(userRole, operationType, Collections.emptyList());
    }

    @Override
    public void deleteGroupRequest(Role role) {
        List<Provisioning> provisionings = getProvisionings(role);
        //Delete the group to all provisionings in Manage where the group is known
        provisionings.forEach(provisioning -> {
            this.remoteProvisionedGroupRepository
                    .findByManageProvisioningIdAndRole(provisioning.getId(), role)
                    .ifPresent(remoteProvisionedGroup -> {
                        String remoteScimIdentifier = remoteProvisionedGroup.getRemoteScimIdentifier();
                        String externalId = GroupURN.urnFromRole(groupUrnPrefix, role);
                        String groupRequest = prettyJson(new GroupRequest(externalId, remoteScimIdentifier, role.getName(), Collections.emptyList()));
                        this.deleteRequest(provisioning, groupRequest, GROUP_API, remoteScimIdentifier);
                        this.remoteProvisionedGroupRepository.delete(remoteProvisionedGroup);
                    });
        });
    }

    private void doGroupRequest(Role role, List<UserRole> userRolesToDelete) {
        List<Provisioning> provisionings = getProvisionings(role);
        provisionings.forEach(provisioning -> {
            Optional<RemoteProvisionedGroup> remoteRoleOptional = this.remoteProvisionedGroupRepository
                    .findByManageProvisioningIdAndRole(provisioning.getId(), role);
            if (remoteRoleOptional.isPresent()) {
                if (provisioning.isScimUpdateRolePutMethod()) {
                    Set<UserRole> userRoles = role.getUserRoles()

                    String groupRequest = constructGroupRequest(role, userRoles);
                    this.updateRequest(provisioning, groupRequest, GROUP_API, role, HttpMethod.PUT);
                } else {
                    String groupRequest = patchGroupRequest(role, userRole, operationType);
                    this.updateRequest(provisioning, groupRequest, GROUP_API, role, HttpMethod.PATCH);
                }

            } else {
                List<UserRole> userRoles = role.getU
                RemoteProvisionedGroup remoteProvisionedGroup = provisionedGroupOptional.get();
                String scimIdentifier = remoteProvisionedGroup.getRemoteScimIdentifier();
                String groupRequest = constructGroupRequest(role, scimIdentifier, userRoles.stream().map());
                this.newRequest(provisioning, groupRequest, GROUP_API, role);

                String remoteScimIdentifier = provisionedGroupOptional.map(RemoteProvisionedGroup::getRemoteScimIdentifier)
                        .orElse(null);
                String externalId = GroupURN.urnFromRole(groupUrnPrefix, role);
                String groupRequest = prettyJson(new GroupRequest(externalId, remoteScimIdentifier, role.getName(), Collections.emptyList()));
                this.deleteRequest(provisioning, groupRequest, GROUP_API, remoteScimIdentifier);
                provisionedGroupOptional.ifPresent(this.remoteProvisionedGroupRepository::delete);
            }
        });
    }

    private void doNewGroupRequest(Role role, Collection<UserRole> userRolesToBeDeleted) {
        List<Provisioning> provisionings = getProvisionings(role);
        provisionings.forEach(provisioning -> {
            Optional<RemoteProvisionedGroup> provisionedGroupOptional = this.remoteProvisionedGroupRepository
                    .findByManageProvisioningIdAndRole(provisioning.getId(), role);
            if (provisionedGroupOptional.isEmpty()) {
                List<UserRole> userRoles = getUserRoles(role, userRolesToBeDeleted);
                RemoteProvisionedGroup remoteProvisionedGroup = provisionedGroupOptional.get();
                String scimIdentifier = remoteProvisionedGroup.getRemoteScimIdentifier();
                String groupRequest = constructGroupRequest(role, scimIdentifier, userRoles.stream().map());
                this.newRequest(provisioning, groupRequest, GROUP_API, role);

                String remoteScimIdentifier = provisionedGroupOptional.map(RemoteProvisionedGroup::getRemoteScimIdentifier)
                        .orElse(null);
                String externalId = GroupURN.urnFromRole(groupUrnPrefix, role);
                String groupRequest = prettyJson(new GroupRequest(externalId, remoteScimIdentifier, role.getName(), Collections.emptyList()));
                this.deleteRequest(provisioning, groupRequest, GROUP_API, remoteScimIdentifier);
                provisionedGroupOptional.ifPresent(this.remoteProvisionedGroupRepository::delete);
            }
        });

        Provisioning provisioning = role.getApplication();
        if (provisioning.provisioningEnabled()) {
            if (hasEmailHook(provisioning)) {
                role.setServiceProviderId(UUID.randomUUID().toString());
            }
        }
    }

    private List<UserRole> getUserRoles(Role role, Collection<UserRole> userRolesToBeDeleted) {
        List<Long> userRoleIdentifiers = userRolesToBeDeleted.stream()
                .map(UserRole::getId)
                .toList();
        return userRoleRepository.findByRoleAndIdNotIn(role, userRoleIdentifiers);
    }

    private void doUpdateGroupRequest(UserRole userRole, OperationType operationType, Collection<UserRole> userRolesToBeDeleted) {
        Role role = userRole.getRole();
        List<Provisioning> provisionings = getProvisionings(role);
        provisionings.forEach(provisioning -> {
            Optional<RemoteProvisionedGroup> provisionedGroupOptional = this.remoteProvisionedGroupRepository
                    .findByManageProvisioningIdAndRole(provisioning.getId(), role);
            if (provisionedGroupOptional.isPresent() || this.hasEmailHook(provisioning)) {
                if (provisioning.isScimUpdateRolePutMethod()) {
                    List<UserRole> userRoles = getUserRoles(role, userRolesToBeDeleted);
                    String groupRequest = constructGroupRequest(role, userRoles);
                    this.updateRequest(provisioning, groupRequest, GROUP_API, role, HttpMethod.PUT);
                } else {
                    String groupRequest = patchGroupRequest(role, userRole, operationType);
                    this.updateRequest(provisioning, groupRequest, GROUP_API, role, HttpMethod.PATCH);
                }
            } else {
                this.doNewGroupRequest(role, userRolesToBeDeleted);
            }
        });
        if (provisioning.provisioningEnabled()) {
            if (StringUtils.hasText(role.getServiceProviderId())) {
            }
        }
    }

    private String constructGroupRequest(Role role, String remoteGroupScimIdentifier, List<String> remoteUserScimIdentifiers) {
        List<Member> members = remoteUserScimIdentifiers.stream()
                .filter(StringUtils::hasText)
                .map(Member::new)
                .toList();
        String externalId = GroupURN.urnFromRole(groupUrnPrefix, role);
        return prettyJson(new GroupRequest(externalId, remoteGroupScimIdentifier, role.getName(), members));
    }

    private String patchGroupRequest(Role role,
                                     String remoteScimProvisionedUser,
                                     String remoteScimProvisionedGroup,
                                     OperationType operationType) {
        String externalId = GroupURN.urnFromRole(groupUrnPrefix, role);
        GroupPatchRequest request = new GroupPatchRequest(externalId, remoteScimProvisionedGroup,
                new Operation(operationType, remoteScimProvisionedUser));
        return prettyJson(request);
    }

    @SneakyThrows
    private String newRequest(Provisioning provisioning, String request, String apiType) {
        if (hasEmailHook(provisioning)) {
            mailBox.sendProvisioningMail(String.format("SCIM %s: CREATE", apiType), request, provisioning.getProvisioningMail());
            return UUID.randomUUID().toString();
        } else {
            URI uri = this.provisioningUri(provisioning, apiType, Optional.empty());
            RequestEntity<String> requestEntity = new RequestEntity<>(request, httpHeaders(provisioning), HttpMethod.POST, uri);
            Map<String, Object> results = doExchange(requestEntity, apiType, mapParameterizedTypeReference, provisioning);
            return String.valueOf(results.get("id"));
        }
    }

    @SneakyThrows
    private void updateRequest(Provisioning provisioning,
                               String request,
                               String apiType,
                               String remoteScimIdentifier,
                               HttpMethod httpMethod) {
        if (hasEmailHook(provisioning)) {
            mailBox.sendProvisioningMail(String.format("SCIM %s: UPDATE", apiType), request, provisioning.getProvisioningMail());
        } else {
            URI uri = this.provisioningUri(provisioning, apiType, Optional.ofNullable(remoteScimIdentifier));
            RequestEntity<String> requestEntity = new RequestEntity<>(request, httpHeaders(provisioning), httpMethod, uri);
            doExchange(requestEntity, apiType, mapParameterizedTypeReference, provisioning);
        }
    }

    private List<Provisioning> getProvisionings(User user) {
        Set<ManageIdentifier> manageIdentifiers = user.manageIdentifierSet();
        List<String> identifiers = manageIdentifiers.stream().map(ManageIdentifier::id).toList();
        List<Provisioning> provisionings = manage.provisioning(identifiers).stream().map(Provisioning::new).toList();
        return provisionings;
    }

    private List<Provisioning> getProvisionings(Role role) {
        List<Provisioning> provisionings = manage.provisioning(List.of(role.getManageId())).stream().map(Provisioning::new).toList();
        return provisionings;
    }


    @SneakyThrows
    private void deleteRequest(Provisioning provisioning, String request, String apiType, String remoteScimIdentifier) {
        if (hasEmailHook(provisioning)) {
            mailBox.sendProvisioningMail(String.format("SCIM %s: DELETE", apiType), request, provisioning.getProvisioningMail());
        } else {
            URI uri = this.provisioningUri(provisioning, apiType, Optional.ofNullable(remoteScimIdentifier));
            HttpHeaders headers = new HttpHeaders();
            headers.setBasicAuth(provisioning.getScimUser(), provisioning.getScimPassword());
            RequestEntity<String> requestEntity = new RequestEntity<>(request, headers, HttpMethod.DELETE, uri);
            doExchange(requestEntity, apiType, stringParameterizedTypeReference, provisioning);
        }
    }

    private <T, S> T doExchange(RequestEntity<S> requestEntity,
                                String api,
                                ParameterizedTypeReference<T> typeReference,
                                Provisioning provisioning) {
        try {
            LOG.info(String.format("Send %s SCIM request with %s httpMethod %s and body %s to %s",
                    api,
                    requestEntity.getUrl(),
                    requestEntity.getMethod(),
                    requestEntity.getBody(),
                    provisioning.getEntityId()));
            return restTemplate.exchange(requestEntity, typeReference).getBody();
        } catch (RestClientException e) {
            LOG.error(String.format("Error %s SCIM request with %s httpMethod %s and body %s to %s",
                    api,
                    requestEntity.getUrl(),
                    requestEntity.getMethod(),
                    requestEntity.getBody(),
                    provisioning.getEntityId()), e);
            throw e;
        }
    }

    private boolean hasEmailHook(Provisioning provisioning) {
        return provisioning.getProvisioningType().equals(ProvisioningType.mail);
    }

    private URI provisioningUri(Provisioning provisioning, String objectType, Optional<String> remoteScimIdentifier) {
        String postFix = remoteScimIdentifier.map(identifier -> "/" + remoteScimIdentifier).orElse("");
        return URI.create(String.format("%s/%s%s",
                provisioning.getScimUrl(),
                objectType,
                postFix));
    }

    @SneakyThrows
    private String prettyJson(Object obj) {
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
    }

    private HttpHeaders httpHeaders(Provisioning provisioning) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(provisioning.getScimUser(), provisioning.getScimPassword());
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        return headers;
    }

}
