package access.provision;

import access.exception.RemoteException;
import access.manage.Manage;
import access.manage.ManageIdentifier;
import access.model.*;
import access.provision.eva.EvaClient;
import access.provision.graph.GraphClient;
import access.provision.graph.GraphResponse;
import access.provision.scim.*;
import access.repository.RemoteProvisionedGroupRepository;
import access.repository.RemoteProvisionedUserRepository;
import access.repository.UserRoleRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import crypto.KeyStore;
import lombok.SneakyThrows;
import okhttp3.OkHttpClient;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
@SuppressWarnings("unchecked")
public class ProvisioningServiceDefault implements ProvisioningService {

    public final static String USER_API = "users";
    public final static String GROUP_API = "groups";

    private static final Log LOG = LogFactory.getLog(ProvisioningServiceDefault.class);

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
    private final String groupUrnPrefix;
    private final GraphClient graphClient;
    private final EvaClient evaClient;
    private final KeyStore keyStore;

    @Autowired
    public ProvisioningServiceDefault(UserRoleRepository userRoleRepository,
                                      RemoteProvisionedUserRepository remoteProvisionedUserRepository,
                                      RemoteProvisionedGroupRepository remoteProvisionedGroupRepository,
                                      Manage manage,
                                      ObjectMapper objectMapper,
                                      KeyStore keyStore,
                                      @Value("${voot.group_urn_domain}") String groupUrnPrefix,
                                      @Value("${config.eduid-idp-schac-home-organization}") String eduidIdpSchacHomeOrganization,
                                      @Value("${config.server-url}") String serverBaseURL) {
        this.userRoleRepository = userRoleRepository;
        this.remoteProvisionedUserRepository = remoteProvisionedUserRepository;
        this.remoteProvisionedGroupRepository = remoteProvisionedGroupRepository;
        this.manage = manage;
        this.objectMapper = objectMapper;
        this.keyStore = keyStore;
        this.groupUrnPrefix = groupUrnPrefix;
        this.graphClient = new GraphClient(serverBaseURL, eduidIdpSchacHomeOrganization, keyStore, objectMapper);
        this.evaClient = new EvaClient(keyStore);
        // Otherwise, we can't use method PATCH
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.connectTimeout(1, TimeUnit.MINUTES);
        builder.retryOnConnectionFailure(true);
        restTemplate.setRequestFactory(new OkHttp3ClientHttpRequestFactory(builder.build()));
    }

    @Override
    public Optional<GraphResponse> newUserRequest(User user) {
        List<Provisioning> provisionings = getProvisionings(user);
        AtomicReference<GraphResponse> graphResponseReference = new AtomicReference<>();
        //Provision the user to all provisionings in Manage where the user is unknown
        provisionings.stream()
                .filter(provisioning -> this.remoteProvisionedUserRepository.findByManageProvisioningIdAndUser(provisioning.getId(), user)
                        .isEmpty())
                .forEach(provisioning -> {
                    String userRequest = prettyJson(new UserRequest(user));
                    Optional<ProvisioningResponse> provisioningResponse = this.newRequest(provisioning, userRequest, user);
                    provisioningResponse.ifPresent(response -> {
                        RemoteProvisionedUser remoteProvisionedUser = new RemoteProvisionedUser(user, response.remoteIdentifier(), provisioning.getId());
                        this.remoteProvisionedUserRepository.save(remoteProvisionedUser);
                        if (response.isGraphResponse()) {
                            graphResponseReference.set((GraphResponse) response);
                        }
                    });
                });
        return Optional.ofNullable(graphResponseReference.get());
    }

    @Override
    public void updateUserRequest(User user) {
        List<Provisioning> userProvisionings = getProvisionings(user);
        List<Provisioning> provisionings = userProvisionings.stream()
                .filter(provisioning -> provisioning.getProvisioningType().equals(ProvisioningType.scim))
                .toList();
        //Provision the user to all provisionings in Manage where the user is known
        provisionings.forEach(provisioning -> {
            Optional<RemoteProvisionedUser> provisionedUserOptional =
                    this.remoteProvisionedUserRepository.findByManageProvisioningIdAndUser(provisioning.getId(), user);
            provisionedUserOptional.ifPresent(remoteProvisionedUser -> {
                String userRequest = prettyJson(new UserRequest(user, remoteProvisionedUser.getRemoteIdentifier()));
                this.updateRequest(provisioning, userRequest, USER_API, remoteProvisionedUser.getRemoteIdentifier(), HttpMethod.PUT);
            });
        });
    }

    @Override
    public void deleteUserRequest(User user) {
        //First send update role requests
        user.getUserRoles()
                .forEach(userRole -> this.updateGroupRequest(userRole, OperationType.Remove));

        List<Provisioning> provisionings = getProvisionings(user);
        //Delete the user to all provisionings in Manage where the user is known
        provisionings.forEach(provisioning -> {
            Optional<RemoteProvisionedUser> provisionedUserOptional = this.remoteProvisionedUserRepository
                    .findByManageProvisioningIdAndUser(provisioning.getId(), user);
            if (provisionedUserOptional.isPresent()) {
                RemoteProvisionedUser remoteProvisionedUser = provisionedUserOptional.get();
                String remoteIdentifier = remoteProvisionedUser.getRemoteIdentifier();
                String userRequest = prettyJson(new UserRequest(user, remoteIdentifier));
                this.deleteRequest(provisioning, userRequest, user, remoteIdentifier);
                this.remoteProvisionedUserRepository.delete(remoteProvisionedUser);
            }
        });
    }

    @Override
    public void newGroupRequest(Role role) {
        List<Provisioning> provisionings = getProvisionings(role);
        provisionings.forEach(provisioning -> {
            Optional<RemoteProvisionedGroup> provisionedGroupOptional = this.remoteProvisionedGroupRepository
                    .findByManageProvisioningIdAndRole(provisioning.getId(), role);
            if (provisionedGroupOptional.isEmpty()) {
                String groupRequest = constructGroupRequest(role, null, Collections.emptyList());
                Optional<ProvisioningResponse> provisioningResponse = this.newRequest(provisioning, groupRequest, role);
                provisioningResponse.ifPresent(response -> {
                    RemoteProvisionedGroup remoteProvisionedGroup = new RemoteProvisionedGroup(role, response.remoteIdentifier(), provisioning.getId());
                    this.remoteProvisionedGroupRepository.save(remoteProvisionedGroup);
                });
            }
        });
    }

    @Override
    public void updateGroupRequest(UserRole userRole, OperationType operationType) {
        if (!userRole.getAuthority().equals(Authority.GUEST) && !userRole.isGuestRoleIncluded()) {
            //We only provision GUEST users
            return;
        }
        Role role = userRole.getRole();
        List<Provisioning> provisionings = getProvisionings(role).stream()
                .filter(Provisioning::isApplicableForGroupRequest)
                .toList();
        provisionings.forEach(provisioning -> {
            Optional<RemoteProvisionedGroup> provisionedGroupOptional = this.remoteProvisionedGroupRepository
                    .findByManageProvisioningIdAndRole(provisioning.getId(), role);
            provisionedGroupOptional.ifPresentOrElse(provisionedGroup -> {
                        List<UserRole> userRoles = new ArrayList<>();
                        if (provisioning.isScimUpdateRolePutMethod()) {
                            //We need all userRoles for a PUT and we only provision guests
                            userRoles = userRoleRepository.findByRole(userRole.getRole())
                                    .stream()
                                    .filter(userRoleDB -> userRoleDB.getAuthority().equals(Authority.GUEST) || userRoleDB.isGuestRoleIncluded())
                                    .collect(Collectors.toCollection(ArrayList::new));
                            boolean userRolePresent = userRoles.stream().anyMatch(dbUserRole -> dbUserRole.getId().equals(userRole.getId()));
                            if (operationType.equals(OperationType.Add) && !userRolePresent) {
                                userRoles.add(userRole);
                            } else if (operationType.equals(OperationType.Remove) && userRolePresent) {
                                userRoles = userRoles.stream()
                                        .filter(dbUserRole -> !dbUserRole.getId().equals(userRole.getId()))
                                        .collect(Collectors.toCollection(ArrayList::new));
                            }
                        } else {
                            userRoles.add(userRole);
                        }
                        sendGroupPutRequest(provisioning, provisionedGroup, userRoles, role, operationType);
                    }, () -> {
                        this.newGroupRequest(role);
                        this.updateGroupRequest(userRole, operationType);
                    }
            );
        });
    }

    private void sendGroupPutRequest(Provisioning provisioning,
                                     RemoteProvisionedGroup provisionedGroup,
                                     List<UserRole> userRoles,
                                     Role role,
                                     OperationType operationType) {
        List<String> userScimIdentifiers = userRoles.stream()
                .map(userRole -> this.remoteProvisionedUserRepository
                        .findByManageProvisioningIdAndUser(provisioning.getId(), userRole.getUser())
                        .or(() -> {
                            this.newUserRequest(userRole.getUser());
                            return this.remoteProvisionedUserRepository
                                    .findByManageProvisioningIdAndUser(provisioning.getId(), userRole.getUser());
                        }))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(RemoteProvisionedUser::getRemoteIdentifier)
                .toList();
        if (!userScimIdentifiers.isEmpty()) {
            if (provisioning.isScimUpdateRolePutMethod()) {
                String groupRequest = constructGroupRequest(
                        role,
                        provisionedGroup.getRemoteIdentifier(),
                        userScimIdentifiers);
                this.updateRequest(provisioning, groupRequest, GROUP_API, provisionedGroup.getRemoteIdentifier(), HttpMethod.PUT);
            } else {
                String groupRequest = patchGroupRequest(
                        role,
                        userScimIdentifiers,
                        provisionedGroup.getRemoteIdentifier(),
                        operationType);
                this.updateRequest(provisioning, groupRequest, GROUP_API, provisionedGroup.getRemoteIdentifier(), HttpMethod.PATCH);
            }
        }

    }

    @Override
    public void updateGroupRequest(List<String> previousManageIdentifiers, Role newRole, boolean nameChanged) {
        //Immutable List can not be sorted
        List<String> previousManageIdentifiersSorted = previousManageIdentifiers.stream().sorted().toList();
        List<String> newManageIdentifiers = this.getManageIdentifiers(newRole);
        if (!nameChanged && previousManageIdentifiers.equals(newManageIdentifiers)) {
            LOG.info(String.format("Group %s update request with no difference in manage identifiers (%s). No action required",
                    newRole.getName(),
                    newManageIdentifiers));
            return;
        }

        LOG.info(String.format("Group %s update request with different manage identifiers. Old identifiers %s, new identifiers %s",
                newRole.getName(),
                previousManageIdentifiers,
                newManageIdentifiers));

        List<String> addedManageIdentifiers = newManageIdentifiers.stream()
                .filter(id -> !previousManageIdentifiersSorted.contains(id) || nameChanged).toList();
        List<String> deletedManageIdentifiers = previousManageIdentifiers.stream()
                .filter(id -> !newManageIdentifiers.contains(id)).toList();

        manage.provisioning(addedManageIdentifiers).stream().map(Provisioning::new)
                .forEach(provisioning -> {
                    Optional<RemoteProvisionedGroup> provisionedGroupOptional = this.remoteProvisionedGroupRepository
                            .findByManageProvisioningIdAndRole(provisioning.getId(), newRole);
                    if (provisionedGroupOptional.isEmpty()) {
                        //Ensure the group is provisioned just in time
                        this.newGroupRequest(newRole);
                        provisionedGroupOptional = this.remoteProvisionedGroupRepository
                                .findByManageProvisioningIdAndRole(provisioning.getId(), newRole);
                    }
                    provisionedGroupOptional.ifPresent(provisionedGroup -> {
                        List<UserRole> userRoles = userRoleRepository.findByRole(newRole);
                        this.sendGroupPutRequest(provisioning, provisionedGroup, userRoles, newRole, OperationType.Add);
                    });
                });

        LOG.info(String.format("Deleting existing provisionings %s from group %s", deletedManageIdentifiers, newRole.getName()));

        List<Provisioning> provisionings = manage.provisioning(deletedManageIdentifiers).stream().map(Provisioning::new).toList();
        deleteGroupRequest(newRole, provisionings);
    }

    @Override
    public void deleteGroupRequest(Role role) {
        List<Provisioning> provisionings = getProvisionings(role);
        deleteGroupRequest(role, provisionings);
    }

    private void deleteGroupRequest(Role role, List<Provisioning> provisionings) {
        //Delete the group to all provisionings in Manage where the group is known
        provisionings.forEach(provisioning ->
                this.remoteProvisionedGroupRepository
                        .findByManageProvisioningIdAndRole(provisioning.getId(), role)
                        .ifPresent(remoteProvisionedGroup -> {
                            String remoteIdentifier = remoteProvisionedGroup.getRemoteIdentifier();
                            String externalId = GroupURN.urnFromRole(groupUrnPrefix, role);
                            String groupRequest = prettyJson(new GroupRequest(externalId, remoteIdentifier, role.getName(), Collections.emptyList()));
                            this.deleteRequest(provisioning, groupRequest, role, remoteIdentifier);
                            this.remoteProvisionedGroupRepository.delete(remoteProvisionedGroup);
                        }));
    }

    private String constructGroupRequest(Role role, String remoteGroupScimIdentifier, List<String> remoteUserScimIdentifiers) {
        HashSet<String> uniqueRemoteUserScimIdentifiers = new HashSet<>(remoteUserScimIdentifiers);
        List<Member> members = uniqueRemoteUserScimIdentifiers.stream()
                .filter(StringUtils::hasText)
                .map(Member::new)
                .toList();
        String externalId = GroupURN.urnFromRole(groupUrnPrefix, role);
        return prettyJson(new GroupRequest(externalId, remoteGroupScimIdentifier, role.getName(), members));
    }

    private String patchGroupRequest(Role role,
                                     List<String> remoteScimProvisionedUsers,
                                     String remoteScimProvisionedGroup,
                                     OperationType operationType) {
        String externalId = GroupURN.urnFromRole(groupUrnPrefix, role);
        GroupPatchRequest request = new GroupPatchRequest(externalId, remoteScimProvisionedGroup,
                new Operation(operationType, remoteScimProvisionedUsers));
        return prettyJson(request);
    }

    private Optional<ProvisioningResponse> newRequest(Provisioning provisioning, String request, Provisionable provisionable) {
        boolean isUser = provisionable instanceof User;
        String apiType = isUser ? USER_API : GROUP_API;
        RequestEntity<String> requestEntity = null;
        if (hasEvaHook(provisioning) && isUser) {
            LOG.info(String.format("Provisioning new eva account for user %s and provisioning %s",
                    ((User) provisionable).getEmail(), provisioning.getEntityId()));
            requestEntity = this.evaClient.newUserRequest(provisioning, (User) provisionable);
        } else if (hasScimHook(provisioning)) {
            LOG.info(String.format("Provisioning new SCIM account for provisionable %s and provisioning %s",
                    provisionable.getName(), provisioning.getEntityId()));
            URI uri = this.provisioningUri(provisioning, apiType, Optional.empty());
            requestEntity = new RequestEntity<>(request, httpHeaders(provisioning), HttpMethod.POST, uri);
        } else if (hasGraphHook(provisioning) && isUser) {
            LOG.info(String.format("Provisioning new Graph user for provisionable %s and provisioning %s",
                    ((User) provisionable).getEmail(), provisioning.getEntityId()));
            GraphResponse graphResponse = this.graphClient.newUserRequest(provisioning, (User) provisionable);
            return Optional.of(graphResponse);
        }
        if (requestEntity != null) {
            Map<String, Object> results = doExchange(requestEntity, apiType, mapParameterizedTypeReference, provisioning);
            return Optional.of(new DefaultProvisioningResponse(String.valueOf(results.get("id"))));
        }
        return Optional.empty();

    }

    private void updateRequest(Provisioning provisioning,
                               String request,
                               String apiType,
                               String remoteIdentifier,
                               HttpMethod httpMethod) {
        if (hasScimHook(provisioning)) {
            URI uri = this.provisioningUri(provisioning, apiType, Optional.ofNullable(remoteIdentifier));
            RequestEntity<String> requestEntity = new RequestEntity<>(request, httpHeaders(provisioning), httpMethod, uri);
            doExchange(requestEntity, apiType, mapParameterizedTypeReference, provisioning);
        }
    }

    private List<Provisioning> getProvisionings(User user) {
        Set<ManageIdentifier> manageIdentifiers = user.manageIdentifierSet();
        List<String> identifiers = manageIdentifiers.stream().map(ManageIdentifier::manageId).toList();
        return manage.provisioning(identifiers).stream().map(Provisioning::new).toList();
    }

    private List<Provisioning> getProvisionings(Role role) {
        List<String> manageIdentifiers = getManageIdentifiers(role);
        return manage.provisioning(manageIdentifiers).stream().map(Provisioning::new).toList();
    }

    private List<String> getManageIdentifiers(Role role) {
        return role.applicationsUsed().stream().map(Application::getManageId).distinct().sorted().toList();
    }

    private void deleteRequest(Provisioning provisioning,
                               String request,
                               Provisionable provisionable,
                               String remoteIdentifier) {
        boolean isUser = provisionable instanceof User;
        String apiType = isUser ? USER_API : GROUP_API;
        RequestEntity<String> requestEntity = null;
        if (hasEvaHook(provisioning) && isUser) {
            String url = provisioning.getEvaUrl() + "/api/v1/guest/disable/" + remoteIdentifier;
            requestEntity = new RequestEntity(httpHeaders(provisioning), HttpMethod.POST, URI.create(url));
        } else if (hasScimHook(provisioning)) {
            URI uri = this.provisioningUri(provisioning, apiType, Optional.ofNullable(remoteIdentifier));
            HttpHeaders headers = this.httpHeaders(provisioning);
            headers.setBasicAuth(provisioning.getScimUser(), this.decryptScimPassword(provisioning));
            requestEntity = new RequestEntity<>(request, headers, HttpMethod.DELETE, uri);
        } else if (hasGraphHook(provisioning) && isUser) {
            this.graphClient.deleteUser((User) provisionable, provisioning, remoteIdentifier);
        }
        if (requestEntity != null) {
            doExchange(requestEntity, apiType, stringParameterizedTypeReference, provisioning);
        }
    }

    private <T, S> T doExchange(RequestEntity<S> requestEntity,
                                String api,
                                ParameterizedTypeReference<T> typeReference,
                                Provisioning provisioning) {
        try {
            LOG.info(String.format("Send %s Provisioning request (protocol %s) with %s httpMethod %s and body %s to %s",
                    api,
                    provisioning.getProvisioningType(),
                    requestEntity.getMethod(),
                    requestEntity.getUrl(),
                    requestEntity.getBody(),
                    provisioning.getEntityId()));
            return restTemplate.exchange(requestEntity, typeReference).getBody();
        } catch (RestClientException e) {
            String errorMessage = String.format("Error %s SCIM request (entityID %s) to %s with %s httpMethod and body %s",
                    api,
                    provisioning.getEntityId(),
                    requestEntity.getUrl(),
                    requestEntity.getMethod(),
                    requestEntity.getBody());
            throw new RemoteException(HttpStatus.BAD_REQUEST, errorMessage, e);
        }
    }

    private boolean hasEvaHook(Provisioning provisioning) {
        return provisioning.getProvisioningType().equals(ProvisioningType.eva);
    }

    private boolean hasScimHook(Provisioning provisioning) {
        return provisioning.getProvisioningType().equals(ProvisioningType.scim);
    }

    private boolean hasGraphHook(Provisioning provisioning) {
        return provisioning.getProvisioningType().equals(ProvisioningType.graph);
    }

    private URI provisioningUri(Provisioning provisioning, String objectType, Optional<String> remoteIdentifier) {
        String postFix = remoteIdentifier.map(identifier -> "/" + identifier).orElse("");
        return URI.create(String.format("%s/%s%s",
                provisioning.getScimUrl(),
                objectType,
                postFix));
    }

    @SneakyThrows
    private String prettyJson(Object obj) {
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
    }

    private String decryptScimPassword(Provisioning provisioning) {
        String scimPassword = provisioning.getScimPassword();
        return keyStore.isEncryptedSecret(scimPassword) ? keyStore.decodeAndDecrypt(scimPassword) : scimPassword;
    }

    private HttpHeaders httpHeaders(Provisioning provisioning) {
        HttpHeaders headers = new HttpHeaders();
        switch (provisioning.getProvisioningType()) {
            case scim -> {
                headers.setBasicAuth(provisioning.getScimUser(), decryptScimPassword(provisioning));
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            }
            case eva -> {
                headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
                headers.add("X-Api-Key", provisioning.getEvaToken());
            }
        }
        return headers;
    }

}
