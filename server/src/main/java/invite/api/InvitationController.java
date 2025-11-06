package invite.api;

import invite.audit.UserRoleAuditService;
import invite.exception.InvitationEmailMatchingException;
import invite.exception.InvitationExpiredException;
import invite.exception.InvitationStatusException;
import invite.exception.NotFoundException;
import invite.logging.AccessLogger;
import invite.logging.Event;
import invite.mail.MailBox;
import invite.manage.Manage;
import invite.model.*;
import invite.provision.Provisioning;
import invite.provision.ProvisioningService;
import invite.provision.graph.GraphResponse;
import invite.provision.scim.OperationType;
import invite.repository.InvitationRepository;
import invite.repository.RoleRepository;
import invite.repository.UserRepository;
import invite.security.SuperAdmin;
import invite.security.UserPermissions;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static invite.SwaggerOpenIdConfig.API_TOKENS_SCHEME_NAME;
import static invite.SwaggerOpenIdConfig.OPEN_ID_SCHEME_NAME;
import static java.util.Collections.emptyList;

@RestController
@RequestMapping(value = {
        "/api/v1/invitations",
        "/api/external/v1/invitations"}
        , produces = MediaType.APPLICATION_JSON_VALUE)
@Transactional
@SecurityRequirement(name = OPEN_ID_SCHEME_NAME, scopes = {"openid"})
@SecurityRequirement(name = API_TOKENS_SCHEME_NAME)
@EnableConfigurationProperties(SuperAdmin.class)
public class InvitationController implements InvitationResource {

    private static final Log LOG = LogFactory.getLog(InvitationController.class);

    @Getter
    private final MailBox mailBox;
    @Getter
    private final Manage manage;
    @Getter
    private final InvitationRepository invitationRepository;
    @Getter
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final ProvisioningService provisioningService;
    private final SecurityContextRepository securityContextRepository;
    private final SuperAdmin superAdmin;
    private final InvitationOperations invitationOperations;
    private final UserRoleAuditService userRoleAuditService;

    public InvitationController(MailBox mailBox,
                                Manage manage,
                                InvitationRepository invitationRepository,
                                UserRepository userRepository,
                                RoleRepository roleRepository,
                                ProvisioningService provisioningService,
                                SecurityContextRepository securityContextRepository,
                                SuperAdmin superAdmin, UserRoleAuditService userRoleAuditService) {
        this.mailBox = mailBox;
        this.manage = manage;
        this.invitationRepository = invitationRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.provisioningService = provisioningService;
        this.securityContextRepository = securityContextRepository;
        this.superAdmin = superAdmin;
        this.invitationOperations = new InvitationOperations(this);
        this.userRoleAuditService = userRoleAuditService;
    }

    @PostMapping("")
    public ResponseEntity<InvitationResponse> newInvitation(@Validated @RequestBody InvitationRequest invitationRequest,
                                                            @Parameter(hidden = true) User user) {
        LOG.debug(String.format("New invitation request by user %s", user.getEduPersonPrincipalName()));
        return this.invitationOperations.sendInvitation(invitationRequest, user, null);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInvitation(@PathVariable("id") Long id,
                                                 @Parameter(hidden = true) User user) {
        LOG.debug(String.format("/deleteInvitation/%s by user %s", id, user.getEduPersonPrincipalName()));
        //We need to assert validations on the roles soo we need to load them
        Invitation invitation = invitationRepository.findById(id).orElseThrow(() -> new NotFoundException("Invitation not found"));
        List<Role> requestedRoles = invitation.getRoles().stream()
                .map(InvitationRole::getRole).toList();
        Authority intendedAuthority = invitation.getIntendedAuthority();
        UserPermissions.assertValidInvitation(user, intendedAuthority, requestedRoles);

        AccessLogger.invitation(LOG, Event.Deleted, invitation);
        invitationRepository.delete(invitation);

        return Results.deleteResult();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Integer>> resendInvitation(@PathVariable("id") Long id,
                                                                 @Parameter(hidden = true) User user) {
        LOG.debug(String.format("ResendInvitation with id %s by user %s ", id, user.getEduPersonPrincipalName()));
        return this.invitationOperations.resendInvitation(id, user, null);
    }

    @GetMapping("public")
    public ResponseEntity<Invitation> getInvitation(@RequestParam("hash") String hash) {
        LOG.debug(String.format("getInvitation with hash %s", hash));
        Invitation invitation = invitationRepository.findByHash(hash).orElseThrow(() -> new NotFoundException("Invitation not found"));
        if (!invitation.getStatus().equals(Status.OPEN)) {
            throw new InvitationStatusException("Invitation is not OPEN anymore");
        }
        manage.addManageMetaData(invitation.getRoles().stream().map(InvitationRole::getRole).toList());
        return ResponseEntity.ok(invitation);
    }

    @GetMapping("all")
    public ResponseEntity<List<Invitation>> all(@Parameter(hidden = true) User user) {
        LOG.debug("GET /all invitations");
        UserPermissions.assertAuthority(user, Authority.SUPER_USER);
        return ResponseEntity.ok(invitationRepository.findByStatus(Status.OPEN));
    }


    @PostMapping("accept")
    public ResponseEntity<Map<String, Object>> accept(@Validated @RequestBody AcceptInvitation acceptInvitation,
                                                      Authentication authentication,
                                                      HttpServletRequest servletRequest,
                                                      HttpServletResponse servletResponse) {
        Invitation invitation = invitationRepository.findByHash(acceptInvitation.hash())
                .orElseThrow(() -> new NotFoundException("Invitation not found"));

        LOG.debug("POST /accept invitation");

        if (!invitation.getId().equals(acceptInvitation.invitationId())) {
            throw new NotFoundException("Invitation not found");
        }
        if (!invitation.getStatus().equals(Status.OPEN)) {
            throw new InvitationStatusException("Invitation is not OPEN anymore");
        }
        if (invitation.getExpiryDate().isBefore(Instant.now())) {
            throw new InvitationExpiredException("Invitation has expired");
        }
        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
        Map<String, Object> attributes = token.getPrincipal().getAttributes();
        String sub = (String) attributes.get("sub");

        LOG.info(String.format("Accept invitation with id %s by user %s", invitation.getId(), sub));

        Optional<User> optionalUser = userRepository.findBySubIgnoreCase(sub);
        Authority intendedAuthority = invitation.getIntendedAuthority();
        User user = optionalUser.orElseGet(() -> {
            boolean superUser = this.superAdmin.getUsers().stream().anyMatch(superSub -> superSub.equals(sub))
                    || intendedAuthority.equals(Authority.SUPER_USER);
            return new User(superUser, attributes);
        });

        checkEmailEquality(user, invitation);
        user.setLastActivity(Instant.now());

        invitation.setStatus(Status.ACCEPTED);
        invitation.setAcceptedAt(Instant.now());
        invitation.setSubInvitee(sub);
        invitationRepository.save(invitation);
        AccessLogger.invitation(LOG, Event.Accepted, invitation);

        /*
         * Chicken & egg problem. The user including his / hers roles must be first provisioned, and then we
         * need to send the updateRoleRequests for each new Role of this user.
         */
        List<UserRole> newUserRoles = new ArrayList<>();
        User inviter = invitation.getInviter();
        invitation.getRoles()
                .forEach(invitationRole -> {
                    Role role = invitationRole.getRole();
                    Optional<UserRole> optionalUserRole = user.getUserRoles().stream()
                            .filter(userRole -> userRole.getRole().getId().equals(role.getId())).findFirst();
                    if (optionalUserRole.isPresent()) {
                        UserRole userRole = optionalUserRole.get();
                        Authority currentAuthority = userRole.getAuthority();
                        //Only act upon different authorities
                        if (!currentAuthority.equals(intendedAuthority)) {
                            boolean userRoleChanged = false;
                            if (intendedAuthority.hasHigherRights(currentAuthority)) {
                                userRole.setAuthority(intendedAuthority);
                                userRole.setEndDate(invitation.getRoleExpiryDate());
                                userRoleChanged = true;
                            }
                            if (currentAuthority.equals(Authority.GUEST) || intendedAuthority.equals(Authority.GUEST) ||
                                    invitation.isGuestRoleIncluded()) {
                                userRole.setGuestRoleIncluded(true);
                                userRoleChanged = true;
                            }
                            if (userRoleChanged) {
                                userRoleAuditService.logAction(userRole, UserRoleAudit.ActionType.UPDATE);
                            }
                        }
                    } else {
                        UserRole userRole = new UserRole(
                                inviter != null ? inviter.getName() : invitation.getRemoteApiUser(),
                                user,
                                role,
                                intendedAuthority,
                                invitation.isGuestRoleIncluded(),
                                invitation.getRoleExpiryDate());
                        user.addUserRole(userRole);
                        newUserRoles.add(userRole);
                    }
                });
        if (intendedAuthority.equals(Authority.INSTITUTION_ADMIN) && inviter != null) {
            user.setInstitutionAdmin(true);
            user.setInstitutionAdminByInvite(true);
            //Might be that a super-user has invited the institution admin or a different institution admin
            if (inviter.isSuperUser()) {
                user.setOrganizationGUID(invitation.getOrganizationGUID());
            } else {
                user.setOrganizationGUID(inviter.getOrganizationGUID());
            }
            //Rare case - a new institution admin has logged in, but was not yet enriched by the CustomOidcUserService
            if (optionalUser.isEmpty()) {
                saveOAuth2AuthenticationToken(authentication, user, servletRequest, servletResponse);
            }
        }
        user.setInternalPlaceholderIdentifier(invitation.getInternalPlaceholderIdentifier());
        userRepository.save(user);
        AccessLogger.user(LOG, Event.Created, user);
        newUserRoles.forEach(userRole -> userRoleAuditService.logAction(userRole, UserRoleAudit.ActionType.ADD));

        //Only interact with the provisioning service if there is a guest role
        boolean isGuest = user.getUserRoles().stream()
                .anyMatch(userRole -> userRole.isGuestRoleIncluded() || userRole.getAuthority().equals(Authority.GUEST));
        //Already provisioned users in the remote systems are ignored / excluded
        Optional<GraphResponse> optionalGraphResponse = isGuest ? provisioningService.newUserRequest(user) : Optional.empty();
        if (isGuest) {
            newUserRoles.forEach(userRole -> provisioningService.updateGroupRequest(userRole, OperationType.Add));
        }
        LOG.info(String.format("User %s accepted invitation with role(s) %s",
                user.getEduPersonPrincipalName(),
                invitation.getRoles().stream().map(role -> role.getRole().getName()).collect(Collectors.joining(", "))));

        //Must be mutable, because of possible userWaitTime
        Map<String, Object> body = new HashMap<>();
        optionalGraphResponse.ifPresentOrElse(graphResponse -> {
            if (graphResponse.isErrorResponse()) {
                body.put("errorResponse", Boolean.TRUE);
            } else {
                body.put("inviteRedeemUrl", graphResponse.inviteRedeemUrl());
            }
        }, () -> body.put("status", "ok"));

        if (!isGuest) {
            //We are done then
            return ResponseEntity.status(HttpStatus.CREATED).body(body);
        }
        // See if there is a userWaitTime on of the provisionings
        List<Provisioning> provisionings = provisioningService.getProvisionings(newUserRoles);
        provisionings.stream()
                .filter(provisioning -> provisioning.getUserWaitTime() != null)
                .max(Comparator.comparingInt(Provisioning::getUserWaitTime))
                .ifPresent(provisioning -> {
                    Set<String> manageIdentifiers = provisioning.getRemoteApplications().stream()
                            .map(app -> app.manageId()).collect(Collectors.toSet());
                    newUserRoles.stream()
                            .map(userRole -> userRole.getRole())
                            .filter(role -> role.getApplicationUsages().stream()
                                    .anyMatch(appUsage -> manageIdentifiers.contains(appUsage.getApplication().getManageId())))
                            .min(Comparator.comparing(Role::getName))
                            .ifPresent(role -> {
                                body.put("userWaitTime", provisioning.getUserWaitTime());
                                body.put("role", role.getName());
                            });
                });
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    private void saveOAuth2AuthenticationToken(Authentication authentication,
                                               User user,
                                               HttpServletRequest servletRequest,
                                               HttpServletResponse servletResponse) {
        //Boilerplate code for setting updated Authentication on the SecurityContextHolder
        OAuth2AuthenticationToken existingToken = (OAuth2AuthenticationToken) authentication;
        DefaultOidcUser existingTokenPrincipal = (DefaultOidcUser) existingToken.getPrincipal();
        //Claims of the tokenPrincipal are immutable, so we need to instantiate a new Map
        Map<String, Object> claims = new HashMap<>(existingTokenPrincipal.getClaims());
        claims.putAll(manage.enrichInstitutionAdmin(user.getOrganizationGUID()));
        DefaultOidcUser oidcUser = new DefaultOidcUser(
                existingToken.getAuthorities(),
                existingTokenPrincipal.getIdToken(),
                new OidcUserInfo(claims)
        );
        OAuth2AuthenticationToken newToken = new OAuth2AuthenticationToken(
                oidcUser,
                existingToken.getAuthorities(),
                existingToken.getAuthorizedClientRegistrationId()
        );
        SecurityContextHolder.getContext().setAuthentication(newToken);
        //New in Spring security 6.x,
        // See https://docs.spring.io/spring-security/reference/5.8/migration/servlet/session-management.html#_require_explicit_saving_of_securitycontextrepository
        securityContextRepository.saveContext(SecurityContextHolder.getContext(), servletRequest, servletResponse);
    }

    @GetMapping("roles/{roleId}")
    public ResponseEntity<List<Invitation>> byRole(@PathVariable("roleId") Long roleId, @Parameter(hidden = true) User user) {
        LOG.debug(String.format("GET /roles/%s by user %s", roleId, user.getEduPersonPrincipalName()));

        Role role = roleRepository.findById(roleId).orElseThrow(() -> new NotFoundException("Role not found"));
        UserPermissions.assertRoleAccess(user, role, Authority.INVITER);
        List<Invitation> invitations = invitationRepository.findByStatusAndRoles_role(Status.OPEN, role);
        return ResponseEntity.ok(invitations);
    }

    @GetMapping("search")
    public ResponseEntity<Page<Map<String, Object>>> search(@Parameter(hidden = true) User user,
                                                            @RequestParam(value = "roleId", required = false) Long roleId,
                                                            @RequestParam(value = "query", required = false, defaultValue = "") String query,
                                                            @RequestParam(value = "pageNumber", required = false, defaultValue = "0") int pageNumber,
                                                            @RequestParam(value = "pageSize", required = false, defaultValue = "10") int pageSize,
                                                            @RequestParam(value = "sort", required = false, defaultValue = "name") String sort,
                                                            @RequestParam(value = "sortDirection", required = false, defaultValue = "ASC") String sortDirection) {
        LOG.debug(String.format("GET /search for invitations %s", user.getEduPersonPrincipalName()));

        Page<Map<String, Object>> invitationsPage;
        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.fromString(sortDirection), sort));

        if (roleId == null) {
            UserPermissions.assertSuperUser(user);
            invitationsPage = StringUtils.hasText(query) ?
                    invitationRepository.searchByStatusPageWithKeyword(Status.OPEN.name(), FullSearchQueryParser.parse(query), pageable) :
                    invitationRepository.searchByStatusPage(Status.OPEN.name(), pageable);
        } else {
            Role role = roleRepository.findById(roleId).orElseThrow(() -> new NotFoundException("Role not found"));
            UserPermissions.assertRoleAccess(user, role, Authority.INVITER);
            invitationsPage = StringUtils.hasText(query) ?
                    invitationRepository.searchByStatusAndRoleWithKeywordPage(Status.OPEN.name(), role.getId(), FullSearchQueryParser.parse(query), pageable) :
                    invitationRepository.searchByStatusAndRolePage(Status.OPEN.name(), role.getId(), pageable);
        }
        if (invitationsPage.getTotalElements() == 0L) {
            return ResponseEntity.ok(invitationsPage);
        }
        List<Long> invitationIdentifiers = invitationsPage.getContent().stream()
                .map(m -> (Long) m.get("id")).toList();
        //The rolesAndManageIdentifiers is a cartesian product, but the relationship between invitation, role and application is mainly 1-1-1
        List<Map<String, Object>> rolesAndManageIdentifiers = invitationRepository.findRoles(invitationIdentifiers);
        Map<Long, List<Map<String, Object>>> rolesGroupedByInvitation = rolesAndManageIdentifiers
                .stream()
                .collect(Collectors.groupingBy(m -> (Long) m.get("id")));
        //We need to add all roles, but also a list of manageIdentifiers for each role
        List<Map<String, Object>> invitations = invitationsPage.getContent()
                .stream()
                //Must copy to avoid java.lang.UnsupportedOperationException: A TupleBackedMap cannot be modified
                .map(invitationMap -> {
                    Map<String, Object> copy = new HashMap<>(invitationMap);
                    List<Map<String, Object>> aggregatedRoles = rolesGroupedByInvitation.get((Long) invitationMap.get("id"));
                    //For Invitations for SuperUsers and InstitutionAdmins have no userRoles
                    if (CollectionUtils.isEmpty(aggregatedRoles)) {
                        copy.put("roles", emptyList());
                        return copy;
                    }
                    Map<Long, List<Map<String, Object>>> rolesGroupedByRole = aggregatedRoles
                            .stream()
                            .collect(Collectors.groupingBy(m -> (Long) m.get("role_id")));
                    List<Map<String, Object>> roles = rolesGroupedByRole
                            .entrySet()
                            .stream()
                            .map(entry -> Map.of(
                                    "id", entry.getKey(),
                                    "name", entry.getValue().getFirst().get("name"),
                                    "manageIdentifiers", entry.getValue().stream().map(m -> m.get("manage_id")).toList()
                            ))
                            .toList();
                    copy.put("roles", roles);
                    return copy;
                })
                .toList();
        return Pagination.of(invitationsPage, invitations);
    }


    private void checkEmailEquality(User user, Invitation invitation) {
        if (invitation.isEnforceEmailEquality() && !invitation.getEmail().equalsIgnoreCase(user.getEmail())) {
            throw new InvitationEmailMatchingException(
                    String.format("Invitation email %s does not match user email %s", invitation.getEmail(), user.getEmail()));
        }
    }


}
