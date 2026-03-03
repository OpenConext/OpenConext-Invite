package invite.crm;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import invite.audit.UserRoleAuditService;
import invite.config.HashGenerator;
import invite.exception.InvalidInputException;
import invite.exception.NotFoundException;
import invite.mail.MailBox;
import invite.manage.EntityType;
import invite.manage.Manage;
import invite.model.Application;
import invite.model.ApplicationUsage;
import invite.model.Authority;
import invite.model.GroupedProviders;
import invite.model.Invitation;
import invite.model.InvitationRole;
import invite.model.Language;
import invite.model.Organisation;
import invite.model.Provisionable;
import invite.model.Role;
import invite.model.User;
import invite.model.UserRole;
import invite.model.UserRoleAudit;
import invite.provision.ProvisioningService;
import invite.provision.scim.OperationType;
import invite.repository.ApplicationRepository;
import invite.repository.InvitationRepository;
import invite.repository.OrganisationRepository;
import invite.repository.RoleRepository;
import invite.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static invite.SwaggerOpenIdConfig.API_HEADER_SCHEME_NAME;
import static invite.api.InvitationOperations.identityProviderName;

@RestController
@RequestMapping(value = {"/api/internal/v1/crm"}, produces = MediaType.APPLICATION_JSON_VALUE)
@Transactional
@SecurityRequirement(name = API_HEADER_SCHEME_NAME)
public class CRMController {

    private static final Log LOG = LogFactory.getLog(CRMController.class);

    private final String collabPersonPrefix;
    private final String inviterName;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ApplicationRepository applicationRepository;
    private final ProvisioningService provisioningService;
    private final MailBox mailBox;
    private final Manage manage;
    private final Map<String, CrmConfigEntry> crmConfig;
    private final UserRoleAuditService userRoleAuditService;
    private final Provisionable provisionable = () -> "SURF CRM";
    private final InvitationRepository invitationRepository;
    private final OrganisationRepository organisationRepository;


    @SuppressWarnings("unchecked")
    public CRMController(@Value("${crm.collab-person-prefix}") String collabPersonPrefix,
                         @Value("${crm.crm-config-resource}") Resource crmConfigResource,
                         @Value("${crm.inviter-name}") String inviterName,
                         UserRepository userRepository,
                         RoleRepository roleRepository,
                         ApplicationRepository applicationRepository,
                         ProvisioningService provisioningService,
                         ObjectMapper objectMapper,
                         MailBox mailBox, Manage manage,
                         UserRoleAuditService userRoleAuditService,
                         InvitationRepository invitationRepository,
                         OrganisationRepository organisationRepository) throws IOException {
        this.userRepository = userRepository;
        this.collabPersonPrefix = collabPersonPrefix;
        this.inviterName = inviterName;
        this.roleRepository = roleRepository;
        this.applicationRepository = applicationRepository;
        this.provisioningService = provisioningService;
        this.mailBox = mailBox;
        this.manage = manage;
        this.userRoleAuditService = userRoleAuditService;
        this.invitationRepository = invitationRepository;
        this.organisationRepository = organisationRepository;
        Map<String, Map<String, Object>> crmConfigRaw = objectMapper.readValue(crmConfigResource.getInputStream(), new TypeReference<>() {
        });
        this.crmConfig = crmConfigRaw.entrySet().stream()
                .map(entry -> new CrmConfigEntry(entry.getKey(), (String) entry.getValue().get("name"),
                        ((List<Map<String, String>>) entry.getValue().getOrDefault("applications", List.of()))
                                .stream()
                                .map(application -> new CrmManageIdentifier(
                                        EntityType.valueOf(application.get("manageType").toUpperCase()),
                                        application.get("manageEntityID"))).toList()))
                .collect(Collectors.toMap(
                        crmConfigEntry -> crmConfigEntry.code(),
                        crmConfigEntry -> crmConfigEntry
                ));
        LOG.debug(String.format("Parsed %s entries from %s", this.crmConfig.size(), crmConfigResource.getDescription()));
    }

    @PostMapping("")
    @Operation(summary = "Create, update CRM role memberships",
            description = "Add or delete the CRM roles to the CRM contact")
    public ResponseEntity<String> contact(@RequestBody CRMContact crmContact) {
        LOG.debug("POST /api/external/v1/crm: " + crmContact);

        boolean created;

        if (crmContact.isSuppressInvitation() &&
                StringUtils.hasText(crmContact.getSchacHomeOrganisation()) && StringUtils.hasText(crmContact.getUid())) {
            created = provisionUser(crmContact);
        } else {
            created = sendInvitation(crmContact);
        }

        return ResponseEntity.ok().body(created ? "created" : "updated");
    }

    @DeleteMapping("")
    @Operation(summary = "Delete CRM profile",
            description = "Delete CRM profile")
    public ResponseEntity<String> delete(@RequestBody CRMContact crmContact) {
        LOG.debug("DELETE /api/external/v1/crm: " + crmContact);

        List<Invitation> invitations = invitationRepository.findByCrmContactIdAndCrmOrganisationId(
                crmContact.getContactId(), crmContact.getOrganisation().getOrganisationId());
        invitations.forEach(invitation -> {
            LOG.debug("Deleting CRM invitation: " + invitation.getEmail());
            this.invitationRepository.delete(invitation);
        });
        String organisationId = crmContact.getOrganisation().getOrganisationId();
        Optional<Organisation> optionalOrganisation = organisationRepository.findByCrmOrganisationId(organisationId);
        optionalOrganisation
                .flatMap(organisation -> userRepository.findByCrmContactIdAndOrganisation(crmContact.getContactId(), organisation))
                .ifPresent(user -> {
                    LOG.debug("Deleting CRM user: " + user.getEmail());
                    this.provisioningService.deleteUserRequest(user);
                    this.userRepository.delete(user);
                });
        return ResponseEntity.ok().body("deleted");
    }

    @GetMapping("/profile")
    public ResponseEntity<ProfileResponse> query(@RequestParam(value = "uid", required = false) String uid,
                                                 @RequestParam(value = "idp", required = false) String idp,
                                                 @RequestParam(value = "guid", required = false) String guid,
                                                 @RequestParam(value = "role", required = false) String role) {
        if (StringUtils.hasText(uid) && StringUtils.hasText(idp)) {
            String sub = this.constructSub(idp, uid);
            Optional<User> userOptional = userRepository.findBySubIgnoreCase(sub);
            userOptional.map(user ->
                            new ProfileResponse("OK", 0,
                                    List.of(new Profile(user.getGivenName(),
                                            user.getMiddleName(),
                                            user.getFamilyName(),
                                            user.getEmail(),
                                            null,
                                            user.getSchacHomeOrganization(),
                                            user.getUid(),
                                            user.getCrmContactId(),
                                            createOrganisation(user),
                                            user.getUserRoles().stream()
                                                    .map(userRole -> userRole.getRole())
                                                    .filter(role -> StringUtils.hasText(role.getCrmRoleId()))
                                                    .map(role -> new Authorisation(role.getCrm))
                                    ))))
                    .orElseGet(() -> crmUserNotFoundOrNoRoles())

        }
    }

    private ProfileResponse crmUserNotFoundOrNoRoles() {
        return new ProfileResponse("Could not find any profiles with the given search parameters", 50, List.of());
    }

    private boolean provisionUser(CRMContact crmContact) {
        String sub = constructSub(crmContact.getSchacHomeOrganisation(), crmContact.getUid());
        Optional<User> optionalUser = userRepository.findBySubIgnoreCase(sub);
        User user = optionalUser.orElseGet(() -> createUser(crmContact, sub));
        user.setCrmContactId(crmContact.getContactId());
        user.setCrmOrganisationId(crmContact.getOrganisation().getOrganisationId());

        List<CRMRole> newCrmRoles = syncCrmRoles(crmContact, user);

        List<Role> roles = convertCrmRolesToInviteRoles(crmContact, newCrmRoles);
        roles
                .forEach(role -> {
                    UserRole userRole = new UserRole(Authority.GUEST, role);
                    user.addUserRole(userRole);

                    userRoleAuditService.logAction(userRole, UserRoleAudit.ActionType.ADD);
                    this.provisioningService.updateGroupRequest(userRole, OperationType.add);
                });
        userRepository.save(user);

        LOG.debug(String.format("Provisioned user %s with roles %s",
                user.getEmail(), roles.stream().map(Role::getName).collect(Collectors.joining(","))));

        return optionalUser.isEmpty();
    }

    private User createUser(CRMContact crmContact, String sub) {
        String middleName = crmContact.getMiddlename();
        String surName = crmContact.getSurname();
        User unsavedUser = new User(
                false,
                crmContact.getEmail(),
                sub,
                crmContact.getSchacHomeOrganisation(),
                crmContact.getFirstname(),
                StringUtils.hasText(middleName) && !middleName.equals(".") ? String.format("%s %s", middleName, surName) : surName,
                crmContact.getEmail());
        unsavedUser.setUid(crmContact.getUid());
        //Need to keep track of this, for reporting back to CRM API consumers
        unsavedUser.setMiddleName(middleName);
        User user = userRepository.save(unsavedUser);

        LOG.debug(String.format("Created new user %s with sub %s",
                user.getEmail(), sub));

        this.provisioningService.newUserRequest(user);
        return user;
    }

    private String constructSub(String schacHomeOrganisation, String uid) {
        return String.format("%s:%s:%s", collabPersonPrefix, schacHomeOrganisation, uid);
    }

    private List<CRMRole> syncCrmRoles(CRMContact crmContact, User user) {
        LOG.debug(String.format("Start syncing crmRoles %s for user %s", crmContact.getRoles(), user.getEmail()));
        // Removes roles no longer present in CRM
        user.getUserRoles().removeIf(userRole -> {
            Role role = userRole.getRole();
            //Ensure not to delete regular non-CRM roles
            boolean isDeleted = StringUtils.hasText(role.getCrmRoleId()) && crmContact.getRoles().stream()
                    .noneMatch(crmRole -> crmRole.getRoleId().equals(role.getCrmRoleId()));
            if (isDeleted) {
                this.provisioningService.updateGroupRequest(userRole, OperationType.remove);
            }
            return isDeleted;
        });
        List<Role> currentRoles = user.getUserRoles().stream()
                .map(userRole -> userRole.getRole())
                .toList();
        // Return all the new CRM roles
        List<CRMRole> crmRoles = crmContact.getRoles().stream()
                .filter(crmRole -> currentRoles.stream()
                        .noneMatch(role -> crmRole.getRoleId().equalsIgnoreCase(role.getCrmRoleId())))
                .toList();
        LOG.debug(String.format("Finished syncing crmRoles %s for user %s", crmContact.getRoles(), user.getEmail()));
        return crmRoles;
    }

    private boolean sendInvitation(CRMContact crmContact) {
        Optional<User> optionalUser =
                userRepository.findByCrmContactIdAndCrmOrganisationId(
                        crmContact.getContactId(), crmContact.getOrganisation().getOrganisationId());
        List<CRMRole> newCrmRoles = syncCrmRoles(crmContact, optionalUser.orElse(new User()));
        //Only save the user when the user already existed
        optionalUser.ifPresent(user -> userRepository.save(user));
        // Maps CRM roles to existing or new roles
        List<Role> roles = convertCrmRolesToInviteRoles(crmContact, newCrmRoles);
        //If there are no new roles, then we can't do anything
        if (!CollectionUtils.isEmpty(roles)) {
            //There are roles in CRM without applications, we need to ignore those
            List<GroupedProviders> groupedProviders = manage.getGroupedProviders(roles);
            Set<InvitationRole> invitationRoles = roles.stream()
                    .map(role -> new InvitationRole(role))
                    .collect(Collectors.toSet());
            Invitation invitation = createInvitation(crmContact, invitationRoles);

            Optional<String> idpName = identityProviderName(manage, invitation);
            if (crmContact.isSuppressInvitation()) {
                LOG.debug(String.format("Not actualy sending invitation to user %s for roles %s, because suppressInvitation is set to true",
                        invitation.getEmail(), roles.stream().map(Role::getName).collect(Collectors.joining(","))));
            } else {
                LOG.debug(String.format("Sending invitation to user %s for roles %s",
                        invitation.getEmail(), roles.stream().map(Role::getName).collect(Collectors.joining(","))));
                mailBox.sendInviteMail(this.provisionable, invitation, groupedProviders, Language.en, idpName);
            }
        }
        return optionalUser.isEmpty();
    }

    private List<Role> convertCrmRolesToInviteRoles(CRMContact crmContact, List<CRMRole> newCrmRoles) {
        return newCrmRoles.stream()
                .map(crmRole -> roleRepository.findByCrmRoleIdAndCrmOrganisationId(
                                crmRole.getRoleId(), crmContact.getOrganisation().getOrganisationId())
                        .or(() -> this.createRole(crmContact.getOrganisation(), crmRole)))
                .flatMap(Optional::stream)
                .toList();
    }

    private Optional<Role> createRole(CRMOrganisation crmOrganisation, CRMRole crmRole) {
        CrmConfigEntry crmConfigEntry = this.crmConfig.get(crmRole.getSabCode());
        if (crmConfigEntry == null) {
            throw new InvalidInputException("CRM sabCode is not configured: " + crmRole.getSabCode());
        }
        if (crmConfigEntry.crmManageIdentifiers().isEmpty()) {
            return Optional.empty();
        }
        Set<ApplicationUsage> applicationUsages = crmConfigEntry.crmManageIdentifiers().stream()
                .map(crmManageIdentifier -> manage
                        .providerByEntityID(crmManageIdentifier.manageType(), crmManageIdentifier.manageEntityID())
                        .orElseThrow(() -> new NotFoundException("Manage entity not found: " + crmManageIdentifier)))
                .map(provider -> {
                    String manageId = (String) provider.get("id");
                    EntityType manageType = EntityType.valueOf(((String) provider.get("type")).toUpperCase());
                    return applicationRepository.findByManageIdAndManageTypeOrderById(manageId, manageType)
                            .orElseGet(
                                    () -> applicationRepository.save(new Application(manageId, manageType, (String) provider.get("url"))));
                })
                .map(application -> new ApplicationUsage(application, application.getLandingPage()))
                .collect(Collectors.toSet());
        Role unsavedRole = new Role(
                String.format("%s for %s", crmConfigEntry.name(), crmOrganisation.getName()),
                String.format("CRM role %s for organisation %s", crmConfigEntry.name(), crmOrganisation.getName()),
                applicationUsages,
                365 * 25,
                true,
                false
        );
        unsavedRole.setCrmRoleId(crmRole.getRoleId());
        unsavedRole.setCrmRoleName(crmConfigEntry.name());
        unsavedRole.setCrmOrganisationId(crmOrganisation.getOrganisationId());
        unsavedRole.setCrmOrganisationCode(crmOrganisation.getAbbrev());
        unsavedRole.setOrganizationGUID(crmOrganisation.getOrganisationId());

        Role role = roleRepository.save(unsavedRole);
        this.provisioningService.newGroupRequest(role);
        return Optional.of(role);
    }

    private Invitation createInvitation(CRMContact crmContact, Set<InvitationRole> invitationRoles) {
        Invitation invitation = new Invitation(
                Authority.GUEST,
                HashGenerator.generateRandomHash(),
                crmContact.getEmail(),
                true,
                false,
                null,
                false,
                null,
                Language.en,
                null,
                null,//Defauly expiryDate
                null,
                invitationRoles,
                null
        );
        String crmOrganisationId = crmContact.getOrganisation().getOrganisationId();
        invitation.setOrganizationGUID(crmOrganisationId);
        invitation.setCrmOrganisationId(crmOrganisationId);
        invitation.setCrmContactId(crmContact.getContactId());
        invitation.setRemoteApiUser(inviterName);
        invitationRepository.save(invitation);
        return invitation;
    }
}
