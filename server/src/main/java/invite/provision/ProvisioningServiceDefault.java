package invite.provision;

import com.fasterxml.jackson.databind.ObjectMapper;
import crypto.KeyStore;
import invite.eduid.EduID;
import invite.eduid.EduIDProvision;
import invite.exception.InvalidInputException;
import invite.exception.RemoteException;
import invite.manage.Manage;
import invite.manage.ManageIdentifier;
import invite.model.Application;
import invite.model.Authority;
import invite.model.Provisionable;
import invite.model.RemoteProvisionedGroup;
import invite.model.RemoteProvisionedUser;
import invite.model.Role;
import invite.model.User;
import invite.model.UserRole;
import invite.provision.eva.EvaClient;
import invite.provision.graph.GraphClient;
import invite.provision.graph.GraphResponse;
import invite.provision.scim.DisplayNameOperation;
import invite.provision.scim.GroupPatchRequest;
import invite.provision.scim.GroupRequest;
import invite.provision.scim.GroupURN;
import invite.provision.scim.Member;
import invite.provision.scim.MembersOperation;
import invite.provision.scim.OperationType;
import invite.provision.scim.UserRequest;
import invite.repository.RemoteProvisionedGroupRepository;
import invite.repository.RemoteProvisionedUserRepository;
import invite.repository.RoleRepository;
import invite.repository.UserRoleRepository;
import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
@SuppressWarnings("unchecked")
public class ProvisioningServiceDefault implements ProvisioningService {

    private final RoleRepository roleRepository;

    private enum APIType {
        USER_API("Users"), GROUP_API("Groups");

        @Getter
        private final String display;

        APIType(String display) {
            this.display = display;
        }
    }

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
    private final EduID eduID;

    @Autowired
    public ProvisioningServiceDefault(UserRoleRepository userRoleRepository,
                                      RemoteProvisionedUserRepository remoteProvisionedUserRepository,
                                      RemoteProvisionedGroupRepository remoteProvisionedGroupRepository,
                                      Manage manage,
                                      ObjectMapper objectMapper,
                                      KeyStore keyStore,
                                      EduID eduID,
                                      @Value("${voot.group_urn_domain}") String groupUrnPrefix,
                                      @Value("${config.eduid-idp-schac-home-organization}") String eduidIdpSchacHomeOrganization,
                                      @Value("${config.server-url}") String serverBaseURL, RoleRepository roleRepository) {
        this.userRoleRepository = userRoleRepository;
        this.remoteProvisionedUserRepository = remoteProvisionedUserRepository;
        this.remoteProvisionedGroupRepository = remoteProvisionedGroupRepository;
        this.manage = manage;
        this.objectMapper = objectMapper;
        this.keyStore = keyStore;
        this.groupUrnPrefix = groupUrnPrefix;
        this.eduID = eduID;
        this.graphClient = new GraphClient(serverBaseURL, eduidIdpSchacHomeOrganization, keyStore, objectMapper);
        this.evaClient = new EvaClient(keyStore, remoteProvisionedUserRepository);
        // Using JdkClientHttpRequestFactory (available in Spring 6.1+)
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory();
        requestFactory.setReadTimeout(Duration.ofMinutes(1));
        restTemplate.setRequestFactory(requestFactory);
        this.roleRepository = roleRepository;
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
                    UserRequest userRequest = new UserRequest(user, provisioning);
                    this.resolveInstitutionalEduID(user, provisioning, userRequest);
                    String userRequestJson = prettyJson(userRequest);
                    Optional<ProvisioningResponse> provisioningResponse = this.newRequest(provisioning, userRequestJson, user);
                    provisioningResponse.ifPresent(response -> {
                        if (!response.isErrorResponse() && StringUtils.hasText(response.remoteIdentifier())) {
                            RemoteProvisionedUser remoteProvisionedUser = new RemoteProvisionedUser(user, response.remoteIdentifier(), provisioning.getId());
                            this.remoteProvisionedUserRepository.save(remoteProvisionedUser);
                        }
                        if (response.isGraphResponse()) {
                            graphResponseReference.set((GraphResponse) response);
                        }
                    });
                });
        return Optional.ofNullable(graphResponseReference.get());
    }

    private void resolveInstitutionalEduID(User user, Provisioning provisioning, UserRequest request) {
        if (ScimUserIdentifier.eduID.equals(provisioning.getScimUserIdentifier()) &&
                request.getUserName().equals(user.getEduId())) {
            //No fallback for failure
            EduIDProvision eduIDProvision = new EduIDProvision(user.getEduId(), provisioning.getInstitutionGUID());
            String eduIdProvisionResponse = this.eduID.provisionEduid(eduIDProvision);
            //Empty eduIdProvisionResponse is handled by invite.eduid.EduID
            request.setInsitutionalEduID(eduIdProvisionResponse);
        }
    }

    @Override
    public void updateUserRequest(User user) {
        List<Provisioning> userProvisionings = getProvisionings(user);
        List<Provisioning> provisionings = userProvisionings.stream()
                .toList();
        //Provision the user to all provisionings in Manage where the user is known
        provisionings.forEach(provisioning -> {
            if (this.hasEvaHook(provisioning)) {
                RequestEntity requestEntity = this.evaClient.updateUserRequest(provisioning, user);
                this.doExchange(requestEntity, APIType.USER_API, mapParameterizedTypeReference, provisioning);
            } else if (this.hasScimHook(provisioning)) {
                Optional<RemoteProvisionedUser> provisionedUserOptional =
                        this.remoteProvisionedUserRepository.findByManageProvisioningIdAndUser(provisioning.getId(), user);
                provisionedUserOptional.ifPresent(remoteProvisionedUser -> {
                    UserRequest userRequest = new UserRequest(user, provisioning, remoteProvisionedUser.getRemoteIdentifier());
                    this.resolveInstitutionalEduID(user, provisioning, userRequest);
                    String userRequestJson = prettyJson(userRequest);
                    this.updateRequest(provisioning, userRequestJson, APIType.USER_API, remoteProvisionedUser.getRemoteIdentifier(), HttpMethod.PUT);
                });
            }
        });
    }

    @Override
    public void updateUserRoleRequest(UserRole userRole) {
        List<Provisioning> provisionings = getProvisionings(userRole.getUser());
        provisionings.forEach(provisioning -> {
            if (this.hasEvaHook(provisioning)) {
                try {
                    //For now only eva is eligible for update's for the userRole (e.g. new end date)
                    RequestEntity requestEntity = this.evaClient.updateUserRequest(provisioning, userRole.getUser());
                    doExchange(requestEntity, APIType.USER_API, stringParameterizedTypeReference, provisioning);
                } catch (InvalidInputException e) {
                    //Can't be helped and won't happen on production
                    LOG.error("Error from evaClient", e);
                }
            }

        });

    }

    @Override
    public void deleteUserRoleRequest(UserRole userRole) {
        List<Provisioning> provisionings = getProvisionings(userRole.getUser());
        provisionings.forEach(provisioning -> {
            if (this.hasEvaHook(provisioning)) {
                RequestEntity requestEntity = this.evaClient.deleteUserRequest(provisioning, userRole.getUser());
                if (requestEntity != null) {
                    doExchange(requestEntity, APIType.USER_API, stringParameterizedTypeReference, provisioning);
                }
            }
            //For now only eva is eligible for update's for the userRole (e.g. new end date)
        });
    }

    @Override
    public void deleteUserRequest(User user) {
        //First send update role requests
        user.getUserRoles()
                .forEach(userRole -> this.updateGroupRequest(userRole, OperationType.remove));

        List<Provisioning> provisionings = getProvisionings(user);
        //Delete the user to all provisionings in Manage where the user is known
        deprovisionUser(user, provisionings);
    }

    @Override
    public void deleteUserRequest(User user, UserRole userRole) {
        //First send update role request
        this.updateGroupRequest(userRole, OperationType.remove);
        /*
         * We first need a List all provisionings for the user#userRole, and then we need to remove the provisiongs
         * from that List that are in use by other user#userRoles, and those are the provisionings which we need to delete
         */
        List<Provisioning> userRoleProvisionings = getProvisioningsUserRole(user, userRole);
        List<String> otherProvisioningIdentifiers = user.getUserRoles().stream()
                .filter(otherUserRole -> !otherUserRole.getId().equals(userRole.getId()))
                .map(otherUserRole -> getProvisioningsUserRole(user, userRole))
                .flatMap(Collection::stream)
                .map(Provisioning::getId)
                .toList();
        List<Provisioning> provisionings = userRoleProvisionings.stream()
                .filter(provisioning -> !otherProvisioningIdentifiers.contains(provisioning.getId()))
                .toList();
        //Delete the user to the not used anymore provisionings  in Manage
        deprovisionUser(user, provisionings);
    }

    @Override
    public void deleteUserRequest(Role role) {
        List<String> manageIdentifiers = getManageIdentifiers(role);
        List<Provisioning> allRoleProvisionings = manage.provisioning(manageIdentifiers).stream()
                .map(Provisioning::new)
                .toList();
        /*
         * We can't deprovision all users of the Role in each provisioning, as they might be in use in other provisioned
         * roles. We need all provisionings of the Role, but we need to check for each user which provisionings needs to
         * be excluded from the deprovision.
         */
        List<UserRole> userRoles = userRoleRepository.findByRole(role);
        userRoles.forEach(userRole -> {
            User user = userRole.getUser();
            List<ManageIdentifier> otherManageIdentifiers = user.getUserRoles().stream()
                    .filter(otherUserRole -> !otherUserRole.getId().equals(userRole.getId()))
                    .map(otherUserRole -> user.manageIdentifierSet(userRole))
                    .flatMap(Collection::stream)
                    .toList();
            // Provisionings that are used by any other userRoles are filtered out
            List<Provisioning> provisionings = allRoleProvisionings.stream()
                    .filter(provisioning -> provisioning.getRemoteApplications().stream()
                            .noneMatch(otherManageIdentifiers::contains))
                    .toList();
            //Delete the user to the not used anymore provisionings  in Manage
            deprovisionUser(user, provisionings);
        });


    }

    private void deprovisionUser(User user, List<Provisioning> provisionings) {
        provisionings.forEach(provisioning -> {
            Optional<RemoteProvisionedUser> provisionedUserOptional = this.remoteProvisionedUserRepository
                    .findByManageProvisioningIdAndUser(provisioning.getId(), user);
            if (provisionedUserOptional.isPresent()) {
                RemoteProvisionedUser remoteProvisionedUser = provisionedUserOptional.get();
                String remoteIdentifier = remoteProvisionedUser.getRemoteIdentifier();
                UserRequest userRequest = new UserRequest(user, provisioning, remoteIdentifier);
                this.resolveInstitutionalEduID(user, provisioning, userRequest);
                String userRequestJson = prettyJson(userRequest);
                this.deleteRequest(provisioning, userRequestJson, user, remoteIdentifier);
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
                .toList();
        provisionings.forEach(provisioning -> {
            if (this.hasScimHook(provisioning) && !provisioning.isScimUserProvisioningOnly()) {
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
                                if (operationType.equals(OperationType.add) && !userRolePresent) {
                                    userRoles.add(userRole);
                                } else if (operationType.equals(OperationType.remove) && userRolePresent) {
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
            }
            //For now only scim is eligible for update's for the groups (e.g. role name / members have changed)
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
        if (!userScimIdentifiers.isEmpty() || operationType.equals(OperationType.replace)) {
            if (provisioning.isScimUpdateRolePutMethod()) {
                String groupRequest = constructGroupRequest(
                        role,
                        provisionedGroup.getRemoteIdentifier(),
                        userScimIdentifiers);
                this.updateRequest(provisioning, groupRequest, APIType.GROUP_API, provisionedGroup.getRemoteIdentifier(), HttpMethod.PUT);
            } else {
                GroupPatchRequest request = operationType.equals(OperationType.replace) ?
                        new GroupPatchRequest(new DisplayNameOperation(role.getName())):
                        new GroupPatchRequest(new MembersOperation(operationType, userScimIdentifiers));
                String groupRequest = prettyJson(request);
                this.updateRequest(provisioning, groupRequest, APIType.GROUP_API, provisionedGroup.getRemoteIdentifier(), HttpMethod.PATCH);
            }
        }
    }

    @Override
    public void updateGroupRequest(List<String> previousManageIdentifiers, Role newRole, boolean nameChanged) {
        //Immutable List cannot be sorted
        List<String> previousManageIdentifiersSorted = previousManageIdentifiers.stream().sorted().toList();
        List<String> newManageIdentifiers = this.getManageIdentifiers(newRole);
        boolean noApplicationsChanged = previousManageIdentifiers.equals(newManageIdentifiers);
        if (!nameChanged && noApplicationsChanged) {
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

        manage.provisioning(addedManageIdentifiers).stream()
                .map(Provisioning::new)
                .filter(provisioning -> !provisioning.isScimUserProvisioningOnly())
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
                        this.sendGroupPutRequest(provisioning, provisionedGroup, userRoles, newRole, OperationType.replace);
                    });
                });

        LOG.info(String.format("Deleting existing provisionings %s from group %s", deletedManageIdentifiers, newRole.getName()));

        List<Provisioning> provisionings = manage.provisioning(deletedManageIdentifiers).stream()
                .map(Provisioning::new).toList();
        deleteGroupRequest(newRole, provisionings);
    }

    @Override
    public void deleteGroupRequest(Role role) {
        List<Provisioning> provisionings = getProvisionings(role);
        deleteGroupRequest(role, provisionings);
    }

    @Override
    public List<Provisioning> getProvisionings(List<UserRole> userRoles) {
        Set<String> manageIdentifiers = userRoles.stream()
                .map(userRole -> this.getManageIdentifiers(userRole.getRole()))
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
        return manage.provisioning(manageIdentifiers).stream().map(Provisioning::new).toList();
    }

    private void deleteGroupRequest(Role role, List<Provisioning> provisionings) {
        //Delete the group to all provisionings in Manage where the group is known
        provisionings
                .stream()
                .filter(provisioning -> !provisioning.isScimUserProvisioningOnly())
                .forEach(provisioning ->
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

    private Optional<ProvisioningResponse> newRequest(Provisioning provisioning, String request, Provisionable provisionable) {
        boolean isUser = provisionable instanceof User;
        APIType apiType = isUser ? APIType.USER_API : APIType.GROUP_API;
        RequestEntity<String> requestEntity = null;
        boolean requiresRemoteIdentifier = false;
        if (hasEvaHook(provisioning) && isUser) {
            LOG.info(String.format("Provisioning new eva account for user %s and provisioning %s",
                    ((User) provisionable).getEmail(), provisioning.getEntityId()));
            requestEntity = this.evaClient.newUserRequest(provisioning, (User) provisionable);
        } else if (hasScimHook(provisioning) && (isUser || !provisioning.isScimUserProvisioningOnly())) {
            LOG.info(String.format("Provisioning new SCIM account for provisionable %s and provisioning %s",
                    provisionable.getName(), provisioning.getEntityId()));
            URI uri = this.provisioningUri(provisioning, apiType, Optional.empty());
            requiresRemoteIdentifier = true;
            requestEntity = new RequestEntity<>(request, httpHeaders(provisioning), HttpMethod.POST, uri);
        } else if (hasGraphHook(provisioning) && isUser) {
            LOG.info(String.format("Provisioning new Graph user for provisionable %s and provisioning %s",
                    ((User) provisionable).getEmail(), provisioning.getEntityId()));
            GraphResponse graphResponse = this.graphClient.newUserRequest(provisioning, (User) provisionable);
            return Optional.of(graphResponse);
        }
        if (requestEntity != null) {
            Map<String, Object> results = doExchange(requestEntity, apiType, mapParameterizedTypeReference, provisioning);
            String id = (String) results.get("id");
            if (!StringUtils.hasText(id) && requiresRemoteIdentifier) {
                String errorMessage = String.format("Error in %s response %s send to entityID %s. ID is required, but not present in SCIM response.",
                        apiType,
                        results,
                        provisioning.getEntityId());
                throw new RemoteException(HttpStatus.BAD_REQUEST, errorMessage, null);
            }
            return Optional.of(new DefaultProvisioningResponse(id));
        }
        return Optional.empty();

    }

    private void updateRequest(Provisioning provisioning,
                               String request,
                               APIType apiType,
                               String remoteIdentifier,
                               HttpMethod httpMethod) {
        if (hasScimHook(provisioning) && (APIType.USER_API.equals(apiType) || !provisioning.isScimUserProvisioningOnly())) {
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

    private List<Provisioning> getProvisioningsUserRole(User user, UserRole userRole) {
        Set<ManageIdentifier> manageIdentifiers = user.manageIdentifierSet(userRole);
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
        APIType apiType = isUser ? APIType.USER_API : APIType.GROUP_API;
        RequestEntity<String> requestEntity = null;
        if (hasEvaHook(provisioning) && isUser) {
            requestEntity = this.evaClient.deleteUserRequest(provisioning, (User) provisionable);
        } else if (hasScimHook(provisioning) && (isUser || !provisioning.isScimUserProvisioningOnly())) {
            URI uri = this.provisioningUri(provisioning, apiType, Optional.ofNullable(remoteIdentifier));
            HttpHeaders headers = this.httpHeaders(provisioning);
            requestEntity = new RequestEntity<>(request, headers, HttpMethod.DELETE, uri);
        } else if (hasGraphHook(provisioning) && isUser) {
            this.graphClient.deleteUser((User) provisionable, provisioning, remoteIdentifier);
        }
        if (requestEntity != null) {
            doExchange(requestEntity, apiType, stringParameterizedTypeReference, provisioning);
        }
    }

    private <T, S> T doExchange(RequestEntity<S> requestEntity,
                                APIType api,
                                ParameterizedTypeReference<T> typeReference,
                                Provisioning provisioning) {
        try {
            LOG.info(String.format("Send %s Provisioning request (protocol %s) with %s httpMethod %s and body %s " +
                            "to provisioning-entityId %s",
                    api.getDisplay(),
                    provisioning.getProvisioningType(),
                    requestEntity.getMethod(),
                    requestEntity.getUrl(),
                    requestEntity.getBody(),
                    provisioning.getEntityId()));
            ResponseEntity<T> responseEntity = restTemplate.exchange(requestEntity, typeReference);
            T body = responseEntity.getBody();
            HttpStatusCode statusCode = responseEntity.getStatusCode();
            LOG.info(String.format("Received response with status %s and body %s for Provisioning request to provisioning-entityId %s",
                    statusCode,
                    body,
                    provisioning.getEntityId()));

            return body;
        } catch (RestClientException e) {
            String errorMessage = String.format("Error %s SCIM request (entityID %s) to %s with %s httpMethod and body %s",
                    api,
                    provisioning.getEntityId(),
                    requestEntity.getUrl(),
                    requestEntity.getMethod(),
                    requestEntity.getBody());
            LOG.error(errorMessage, e);
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

    private URI provisioningUri(Provisioning provisioning, APIType apiType, Optional<String> remoteIdentifier) {
        String postFix = remoteIdentifier.map(identifier -> "/" + identifier).orElse("");
        return URI.create(String.format("%s/%s%s",
                provisioning.getScimUrl(),
                apiType.getDisplay(),
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

    private String decryptScimBearerToken(Provisioning provisioning) {
        String scimBearerToken = provisioning.getScimBearerToken();
        return keyStore.isEncryptedSecret(scimBearerToken) ? keyStore.decodeAndDecrypt(scimBearerToken) : scimBearerToken;
    }

    private HttpHeaders httpHeaders(Provisioning provisioning) {
        HttpHeaders headers = new HttpHeaders();
        switch (provisioning.getProvisioningType()) {
            case scim -> {
                if (StringUtils.hasText(provisioning.getScimPassword())) {
                    headers.setBasicAuth(provisioning.getScimUser(), this.decryptScimPassword(provisioning));
                } else if (StringUtils.hasText(provisioning.getScimBearerToken())) {
                    String decryptedScimBearerToken = this.decryptScimBearerToken(provisioning);
                    //For testing only, remove before prod
                    LOG.debug(String.format("Inserting header Authorization: Bearer %s ", decryptedScimBearerToken));
                    headers.add(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", decryptedScimBearerToken));
                }
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
