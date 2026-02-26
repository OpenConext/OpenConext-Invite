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
import invite.model.Provisionable;
import invite.model.Role;
import invite.model.User;
import invite.model.UserRole;
import invite.model.UserRoleAudit;
import invite.provision.ProvisioningService;
import invite.provision.scim.OperationType;
import invite.repository.ApplicationRepository;
import invite.repository.InvitationRepository;
import invite.repository.RoleRepository;
import invite.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
                         InvitationRepository invitationRepository) throws IOException {
        this.userRepository = userRepository;
        this.collabPersonPrefix = collabPersonPrefix;
        this.inviterName = inviterName;
        this.roleRepository = roleRepository;
        this.applicationRepository = applicationRepository;
        this.provisioningService = provisioningService;
        this.mailBox = mailBox;
        this.manage = manage;
        this.userRoleAuditService = userRoleAuditService;
        Map<String, Map<String, Object>> crmConfigRaw = objectMapper.readValue(crmConfigResource.getInputStream(), new TypeReference<>() {
        });
        this.crmConfig = crmConfigRaw.entrySet().stream()
                .map(entry -> new CrmConfigEntry(entry.getKey(), (String) entry.getValue().get("name"),
                        ((List<Map<String, String>>) entry.getValue().get("applications")).stream()
                                .map(application -> new CrmManageIdentifier(
                                        EntityType.valueOf(application.get("manageType").toUpperCase()),
                                        application.get("manageEntityID"))).toList()))
                .collect(Collectors.toMap(
                        crmConfigEntry -> crmConfigEntry.code(),
                        crmConfigEntry -> crmConfigEntry
                ));
        LOG.info(String.format("Parsed %s entries from %s", this.crmConfig.size(), crmConfigResource.getDescription()));
        this.invitationRepository = invitationRepository;
    }

    @PostMapping("")
    @Operation(summary = "Create, update CRM role memberships",
            description = "Add or delete the CRM roles to the CRM contact")
    public ResponseEntity<String> contact(@RequestBody CRMContact crmContact) {
        LOG.debug("POST /api/external/v1/crm: " + crmContact);

        boolean created;

        if (crmContact.isSuppressInvitation()) {
            if (!StringUtils.hasText(crmContact.getSchacHomeOrganisation()) || !StringUtils.hasText(crmContact.getUid())) {
                throw new InvalidInputException(
                        "Missing schacHomeOrganisation or uid in crmContact with isSuppressInvitation true: " + crmContact);
            }
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
            LOG.info("Deleting CRM invitation: " + invitation.getEmail());
            this.invitationRepository.delete(invitation);
        });

        Optional<User> userOptional = userRepository.findByCrmContactIdAndCrmOrganisationId(
                crmContact.getContactId(), crmContact.getOrganisation().getOrganisationId());
        userOptional.ifPresent(user -> {
            LOG.info("Deleting CRM user: " + user.getEmail());
            this.provisioningService.deleteUserRequest(user);
            this.userRepository.delete(user);
        });

        return ResponseEntity.ok().body("deleted");
    }

    private boolean provisionUser(CRMContact crmContact) {
        String sub = constructSub(crmContact);
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
                StringUtils.hasText(middleName) ? String.format("%s %s", middleName, surName) : surName,
                crmContact.getEmail());
        User user = userRepository.save(unsavedUser);
        this.provisioningService.newUserRequest(user);
        return user;
    }

    private String constructSub(CRMContact crmContact) {
        return String.format("%s:%s:%s", collabPersonPrefix, crmContact.getSchacHomeOrganisation(), crmContact.getUid());
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
        //If there are no new roles, then we can't do anything
        if (!CollectionUtils.isEmpty(newCrmRoles)) {
            // Maps CRM roles to existing or new roles
            List<Role> roles = convertCrmRolesToInviteRoles(crmContact, newCrmRoles);
            List<GroupedProviders> groupedProviders = manage.getGroupedProviders(roles);
            Set<InvitationRole> invitationRoles = roles.stream()
                    .map(role -> new InvitationRole(role))
                    .collect(Collectors.toSet());
            Invitation invitation = createInvitation(crmContact, invitationRoles);

            Optional<String> idpName = identityProviderName(manage, invitation);
            mailBox.sendInviteMail(this.provisionable, invitation, groupedProviders, Language.en, idpName);
        }
        return optionalUser.isEmpty();
    }

    private @NonNull List<Role> convertCrmRolesToInviteRoles(CRMContact crmContact, List<CRMRole> newCrmRoles) {
        return newCrmRoles.stream()
                .map(crmRole -> roleRepository.findByCrmRoleId(crmRole.getRoleId())
                        .orElseGet(() -> this.createRole(crmContact.getOrganisation(), crmRole)))
                .toList();
    }

    private Role createRole(CRMOrganisation crmOrganisation, CRMRole crmRole) {
        CrmConfigEntry crmConfigEntry = this.crmConfig.get(crmRole.getSabCode());
        if (crmConfigEntry == null) {
            throw new InvalidInputException("CRM sabCode is not configured: " + crmRole.getSabCode());
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
        return role;
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
