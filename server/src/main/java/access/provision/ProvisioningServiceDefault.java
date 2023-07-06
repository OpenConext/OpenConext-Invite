package access.provision;

import access.exception.RemoteException;
import access.manage.Manage;
import access.manage.ManageIdentifier;
import access.model.*;
import access.provision.eva.GuestAccount;
import access.provision.scim.*;
import access.repository.RemoteProvisionedGroupRepository;
import access.repository.RemoteProvisionedUserRepository;
import access.repository.UserRoleRepository;
import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.graph.authentication.TokenCredentialAuthProvider;
import com.microsoft.graph.http.BaseRequest;
import com.microsoft.graph.models.PasswordProfile;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.requests.UserCollectionRequest;
import lombok.SneakyThrows;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.*;
import java.util.concurrent.TimeUnit;

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

    @Autowired
    public ProvisioningServiceDefault(UserRoleRepository userRoleRepository,
                                      RemoteProvisionedUserRepository remoteProvisionedUserRepository,
                                      RemoteProvisionedGroupRepository remoteProvisionedGroupRepository,
                                      Manage manage,
                                      ObjectMapper objectMapper,
                                      @Value("${voot.group_urn_domain}") String groupUrnPrefix) {
        this.userRoleRepository = userRoleRepository;
        this.remoteProvisionedUserRepository = remoteProvisionedUserRepository;
        this.remoteProvisionedGroupRepository = remoteProvisionedGroupRepository;
        this.manage = manage;
        this.objectMapper = objectMapper;
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
        //Provision the user to all provisionings in Manage where the user is unknown
        provisionings.stream()
                .filter(provisioning -> this.remoteProvisionedUserRepository.findByManageProvisioningIdAndUser(provisioning.getId(), user)
                        .isEmpty())
                .forEach(provisioning -> {
                    String userRequest = prettyJson(new UserRequest(user));
                    Optional<String> remoteScimIdentifier = this.newRequest(provisioning, userRequest, user);
                    remoteScimIdentifier.ifPresent(identifier -> {
                        RemoteProvisionedUser remoteProvisionedUser = new RemoteProvisionedUser(user, identifier, provisioning.getId());
                        this.remoteProvisionedUserRepository.save(remoteProvisionedUser);
                    });
                });
    }

    @Override
    @SneakyThrows
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
                String remoteScimIdentifier = remoteProvisionedUser.getRemoteScimIdentifier();
                String userRequest = prettyJson(new UserRequest(user, remoteScimIdentifier));
                this.deleteRequest(provisioning, userRequest, user, remoteScimIdentifier);
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
                Optional<String> remoteScimIdentifier = this.newRequest(provisioning, groupRequest, role);
                remoteScimIdentifier.ifPresent(identifier -> {
                    RemoteProvisionedGroup remoteProvisionedGroup = new RemoteProvisionedGroup(role, identifier, provisioning.getId());
                    this.remoteProvisionedGroupRepository.save(remoteProvisionedGroup);
                });
            }
        });
    }

    @Override
    public void updateGroupRequest(UserRole userRole, OperationType operationType) {
        Role role = userRole.getRole();
        List<Provisioning> provisionings = getProvisionings(role).stream()
                .filter(Provisioning::isApplicableForGroupRequest)
                .toList();
        provisionings.forEach(provisioning -> {
            Optional<RemoteProvisionedGroup> provisionedGroupOptional = this.remoteProvisionedGroupRepository
                    .findByManageProvisioningIdAndRole(provisioning.getId(), role);
            provisionedGroupOptional.ifPresentOrElse(provisionedGroup -> {
                        if (provisioning.isScimUpdateRolePutMethod()) {
                            //We need all userRoles for a PUT
                            List<UserRole> userRoles = userRoleRepository.findByRole(userRole.getRole());
                            boolean userRolePresent = userRoles.stream().anyMatch(dbUserRole -> dbUserRole.getId().equals(userRole.getId()));
                            if (operationType.equals(OperationType.Add) && !userRolePresent) {
                                userRoles.add(userRole);
                            } else if (operationType.equals(OperationType.Remove) && userRolePresent) {
                                userRoles = userRoles.stream()
                                        .filter(dbUserRole -> !dbUserRole.getId().equals(userRole.getId()))
                                        .toList();
                            }
                            List<String> userScimIdentifiers = userRoles.stream()
                                    .map(ur -> {
                                        Optional<RemoteProvisionedUser> provisionedUser = this.remoteProvisionedUserRepository.findByManageProvisioningIdAndUser(provisioning.getId(), ur.getUser());
                                        //Should not happen, but try to provision anyway
                                        if (provisionedUser.isEmpty()) {
                                            this.newUserRequest(ur.getUser());
                                            provisionedUser = this.remoteProvisionedUserRepository.findByManageProvisioningIdAndUser(provisioning.getId(), ur.getUser());
                                        }
                                        return provisionedUser;
                                    })
                                    .filter(Optional::isPresent)
                                    .map(Optional::get)
                                    .map(RemoteProvisionedUser::getRemoteScimIdentifier)
                                    .toList();
                            //We only provision GUEST users
                            if (!userScimIdentifiers.isEmpty()) {
                                String groupRequest = constructGroupRequest(
                                        role,
                                        provisionedGroup.getRemoteScimIdentifier(),
                                        userScimIdentifiers);
                                this.updateRequest(provisioning, groupRequest, GROUP_API, provisionedGroup.getRemoteScimIdentifier(), HttpMethod.PUT);

                            }
                        } else {
                            Optional<RemoteProvisionedUser> provisionedUserOptional = this.remoteProvisionedUserRepository
                                    .findByManageProvisioningIdAndUser(provisioning.getId(), userRole.getUser())
                                    .or(() -> {
                                        this.newUserRequest(userRole.getUser());
                                        return this.remoteProvisionedUserRepository
                                                .findByManageProvisioningIdAndUser(provisioning.getId(), userRole.getUser());
                                    });
                            //Should not be empty, but avoid error on this
                            provisionedUserOptional.ifPresent(provisionedUser -> {
                                String groupRequest = patchGroupRequest(
                                        role,
                                        provisionedUser.getRemoteScimIdentifier(),
                                        provisionedGroup.getRemoteScimIdentifier(),
                                        operationType);
                                this.updateRequest(provisioning, groupRequest, GROUP_API, provisionedGroup.getRemoteScimIdentifier(), HttpMethod.PATCH);
                            });
                        }
                    }, () -> {
                        this.newGroupRequest(role);
                        this.updateGroupRequest(userRole, operationType);
                    }
            );
        });
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
                        this.deleteRequest(provisioning, groupRequest, role, remoteScimIdentifier);
                        this.remoteProvisionedGroupRepository.delete(remoteProvisionedGroup);
                    });
        });
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
                new Operation(operationType, List.of(remoteScimProvisionedUser)));
        return prettyJson(request);
    }

    @SneakyThrows
    private Optional<String> newRequest(Provisioning provisioning, String request, Provisionable provisionable) {
        boolean isUser = provisionable instanceof User;
        String apiType = isUser ? USER_API : GROUP_API;
        RequestEntity<String> requestEntity = null;
        if (hasEvaHook(provisioning) && isUser) {
            MultiValueMap<String, String> map = new GuestAccount((User) provisionable, provisioning).getRequest();
            String url = provisioning.getEvaUrl() + "/api/v1/guest/create";
            requestEntity = new RequestEntity(map, httpHeaders(provisioning), HttpMethod.POST, URI.create(url));
        } else if (hasScimHook(provisioning)) {
            URI uri = this.provisioningUri(provisioning, apiType, Optional.empty());
            requestEntity = new RequestEntity<>(request, httpHeaders(provisioning), HttpMethod.POST, uri);
        } else if (hasGraphHook(provisioning) && isUser) {
            return Optional.of(this.graphClient(provisioning, (User) provisionable));
        }
        if (requestEntity != null) {
            Map<String, Object> results = doExchange(requestEntity, apiType, mapParameterizedTypeReference, provisioning);
            return Optional.of(String.valueOf(results.get("id")));
        }
        return Optional.empty();

    }

    @SneakyThrows
    private void updateRequest(Provisioning provisioning,
                               String request,
                               String apiType,
                               String remoteScimIdentifier,
                               HttpMethod httpMethod) {
        if (hasScimHook(provisioning)) {
            URI uri = this.provisioningUri(provisioning, apiType, Optional.ofNullable(remoteScimIdentifier));
            RequestEntity<String> requestEntity = new RequestEntity<>(request, httpHeaders(provisioning), httpMethod, uri);
            doExchange(requestEntity, apiType, mapParameterizedTypeReference, provisioning);
        }
    }

    private List<Provisioning> getProvisionings(User user) {
        Set<ManageIdentifier> manageIdentifiers = user.manageIdentifierSet();
        List<String> identifiers = manageIdentifiers.stream().map(ManageIdentifier::id).toList();
        return manage.provisioning(identifiers).stream().map(Provisioning::new).toList();
    }

    private List<Provisioning> getProvisionings(Role role) {
        return manage.provisioning(List.of(role.getManageId())).stream().map(Provisioning::new).toList();
    }

    @SneakyThrows
    private void deleteRequest(Provisioning provisioning,
                               String request,
                               Provisionable provisionable,
                               String remoteScimIdentifier) {
        boolean isUser = provisionable instanceof User;
        String apiType = isUser ? USER_API : GROUP_API;
        RequestEntity<String> requestEntity = null;
        if (hasEvaHook(provisioning) && isUser) {
            String url = provisioning.getEvaUrl() + "/api/v1/guest/disable/" + remoteScimIdentifier;
            requestEntity = new RequestEntity(httpHeaders(provisioning), HttpMethod.POST, URI.create(url));
        } else if (hasScimHook(provisioning)) {
            URI uri = this.provisioningUri(provisioning, apiType, Optional.ofNullable(remoteScimIdentifier));
            HttpHeaders headers = new HttpHeaders();
            headers.setBasicAuth(provisioning.getScimUser(), provisioning.getScimPassword());
            requestEntity = new RequestEntity<>(request, headers, HttpMethod.DELETE, uri);
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
            LOG.info(String.format("Send %s SCIM request with %s httpMethod %s and body %s to %s",
                    api,
                    requestEntity.getUrl(),
                    requestEntity.getMethod(),
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

    private URI provisioningUri(Provisioning provisioning, String objectType, Optional<String> remoteScimIdentifier) {
        String postFix = remoteScimIdentifier.map(identifier -> "/" + identifier).orElse("");
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
        switch (provisioning.getProvisioningType()) {
            case scim -> {
                headers.setBasicAuth(provisioning.getScimUser(), provisioning.getScimPassword());
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

    private String graphClient(Provisioning provisioning, User user) {
        ClientSecretCredential credential = new ClientSecretCredentialBuilder()
                .clientId(provisioning.getGraphClientId())
                .tenantId(provisioning.getGraphTenant())
                .clientSecret(provisioning.getGraphSecret()).build();

        TokenCredentialAuthProvider authProvider = new TokenCredentialAuthProvider(credential);

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
        UserCollectionRequest buildRequest = graphClient.users().buildRequest();
        String graphUrl = provisioning.getGraphUrl();
        if (graphUrl.startsWith("http://")) {
            Field field = ReflectionUtils.findField(BaseRequest.class, "requestUrl");
            ReflectionUtils.makeAccessible(field);
            ReflectionUtils.setField(field, buildRequest.getBaseRequest(), graphUrl);
        }
        com.microsoft.graph.models.User createdUser = buildRequest.post(graphUser);
        return createdUser.id;
    }

}
