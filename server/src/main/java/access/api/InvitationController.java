package access.api;

import access.exception.InvitationEmailMatchingException;
import access.exception.InvitationExpiredException;
import access.exception.InvitationStatusException;
import access.exception.NotFoundException;
import access.logging.AccessLogger;
import access.logging.Event;
import access.mail.MailBox;
import access.manage.Manage;
import access.model.*;
import access.provision.Provisioning;
import access.provision.ProvisioningService;
import access.provision.graph.GraphResponse;
import access.provision.scim.OperationType;
import access.repository.InvitationRepository;
import access.repository.RoleRepository;
import access.repository.UserRepository;
import access.security.SuperAdmin;
import access.security.UserPermissions;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static access.SwaggerOpenIdConfig.API_TOKENS_SCHEME_NAME;
import static access.SwaggerOpenIdConfig.OPEN_ID_SCHEME_NAME;

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

    public InvitationController(MailBox mailBox,
                                Manage manage,
                                InvitationRepository invitationRepository,
                                UserRepository userRepository,
                                RoleRepository roleRepository,
                                ProvisioningService provisioningService,
                                SecurityContextRepository securityContextRepository,
                                SuperAdmin superAdmin) {
        this.mailBox = mailBox;
        this.manage = manage;
        this.invitationRepository = invitationRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.provisioningService = provisioningService;
        this.securityContextRepository = securityContextRepository;
        this.superAdmin = superAdmin;
        this.invitationOperations = new InvitationOperations(this);
    }

    @PostMapping("")
    public ResponseEntity<InvitationResponse> newInvitation(@Validated @RequestBody InvitationRequest invitationRequest,
                                                            @Parameter(hidden = true) User user) {
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
        return this.invitationOperations.resendInvitation(id, user, null);
    }

    @GetMapping("public")
    public ResponseEntity<Invitation> getInvitation(@RequestParam("hash") String hash) {
        Invitation invitation = invitationRepository.findByHash(hash).orElseThrow(() -> new NotFoundException("Invitation not found"));
        if (!invitation.getStatus().equals(Status.OPEN)) {
            throw new InvitationStatusException("Invitation is not OPEN anymore");
        }
        manage.addManageMetaData(invitation.getRoles().stream().map(InvitationRole::getRole).toList());
        return ResponseEntity.ok(invitation);
    }

    @GetMapping("all")
    public ResponseEntity<List<Invitation>> all(@Parameter(hidden = true) User user) {
        LOG.debug("/all invitations");
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
                            if (intendedAuthority.hasHigherRights(currentAuthority)) {
                                userRole.setAuthority(intendedAuthority);
                                userRole.setEndDate(invitation.getRoleExpiryDate());
                            }
                            if (currentAuthority.equals(Authority.GUEST) || intendedAuthority.equals(Authority.GUEST) ||
                                    invitation.isGuestRoleIncluded()) {
                                userRole.setGuestRoleIncluded(true);
                            }
                        }
                    } else {
                        UserRole userRole = new UserRole(
                                inviter.getName(),
                                user,
                                role,
                                intendedAuthority,
                                invitation.isGuestRoleIncluded(),
                                invitation.getRoleExpiryDate());
                        user.addUserRole(userRole);
                        newUserRoles.add(userRole);
                    }
                });
        if (intendedAuthority.equals(Authority.INSTITUTION_ADMIN)) {
            user.setInstitutionAdmin(true);
            user.setInstitutionAdminByInvite(true);
            user.setOrganizationGUID(inviter.getOrganizationGUID());
            //Rare case - a new institution admin has logged in, but was not yet enriched by the CustomOidcUserService
            if (optionalUser.isEmpty()) {
                saveOAuth2AuthenticationToken(authentication, user, servletRequest, servletResponse);
            }
        }
        userRepository.save(user);
        AccessLogger.user(LOG, Event.Created, user);

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
                            .filter(userRole -> userRole.getRole().getApplicationUsages().stream()
                                    .anyMatch(appUsage -> manageIdentifiers.contains(appUsage.getApplication().getManageId())))

                            .map(userRole -> userRole.getRole())
                            .sorted(Comparator.comparing(Role::getName))
                            .findFirst()
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
        LOG.debug(String.format("/roles/%s by user %s", roleId, user.getEduPersonPrincipalName()));
        Role role = roleRepository.findById(roleId).orElseThrow(() -> new NotFoundException("Role not found"));
        UserPermissions.assertRoleAccess(user, role, Authority.INVITER);
        List<Invitation> invitations = invitationRepository.findByStatusAndRoles_role(Status.OPEN, role);
        return ResponseEntity.ok(invitations);
    }

    private void checkEmailEquality(User user, Invitation invitation) {
        if (invitation.isEnforceEmailEquality() && !invitation.getEmail().equalsIgnoreCase(user.getEmail())) {
            throw new InvitationEmailMatchingException(
                    String.format("Invitation email %s does not match user email %s", invitation.getEmail(), user.getEmail()));
        }
    }


}
