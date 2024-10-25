package access.api;

import access.config.Config;
import access.exception.NotAllowedException;
import access.exception.NotFoundException;
import access.logging.AccessLogger;
import access.logging.Event;
import access.model.*;
import access.provision.ProvisioningService;
import access.provision.eva.EvaClient;
import access.provision.scim.OperationType;
import access.repository.RoleRepository;
import access.repository.UserRepository;
import access.repository.UserRoleRepository;
import access.security.UserPermissions;
import crypto.KeyStore;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.Getter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static access.SwaggerOpenIdConfig.API_TOKENS_SCHEME_NAME;
import static access.SwaggerOpenIdConfig.OPEN_ID_SCHEME_NAME;
@RestController
@RequestMapping(value = {"/api/v1/user_roles", "/api/external/v1/user_roles"}, produces = MediaType.APPLICATION_JSON_VALUE)
@Transactional
@SecurityRequirement(name = OPEN_ID_SCHEME_NAME, scopes = {"openid"})
@EnableConfigurationProperties(Config.class)
@SecurityRequirement(name = API_TOKENS_SCHEME_NAME)
public class UserRoleController implements UserRoleResource {

    private static final Log LOG = LogFactory.getLog(UserRoleController.class);

    @Getter
    private final UserRoleRepository userRoleRepository;
    @Getter
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final ProvisioningService provisioningService;
    private final Config config;
    private final UserRoleOperations userRoleOperations;

    public UserRoleController(UserRoleRepository userRoleRepository,
                              RoleRepository roleRepository,
                              UserRepository userRepository,
                              ProvisioningService provisioningService,
                              Config config) {
        this.userRoleRepository = userRoleRepository;
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.provisioningService = provisioningService;
        this.config = config;
        this.userRoleOperations = new UserRoleOperations(this);
    }

    @GetMapping("roles/{roleId}")
    public ResponseEntity<List<UserRole>> byRole(@PathVariable("roleId") Long roleId,
                                                 @Parameter(hidden = true) User user) {
        return this.userRoleOperations.userRolesByRole(roleId,
                role -> UserPermissions.assertRoleAccess(user, role, Authority.INVITER));
    }

    @GetMapping("/consequences/{roleId}")
    public ResponseEntity<List<Map<String, Object>>> consequencesDeleteRole(@PathVariable("roleId") Long roleId,
                                                                            @Parameter(hidden = true) User user) {
        Role role = roleRepository.findById(roleId).orElseThrow(() -> new NotFoundException("Role not found"));

        LOG.debug(String.format("Fetching consequences delete role %s by user %s", role.getName(), user.getEduPersonPrincipalName()));

        UserPermissions.assertRoleAccess(user, role, Authority.INSTITUTION_ADMIN);

        List<UserRole> userRoles = userRoleRepository.findByRole(role);
        List<Map<String, Object>> res = userRoles.stream().map(userRole -> Map.of(
                "authority", userRole.getAuthority().name(),
                "userInfo", userRole.getUser().asMap()
        )).toList();
        return ResponseEntity.ok(res);
    }

    @GetMapping("/search/{roleId}/{guests}")
    public ResponseEntity<Page<?>> searchPaginated(@PathVariable("roleId") Long roleId,
                                                   @PathVariable("guests") boolean guests,
                                                   @RequestParam(value = "query", required = false, defaultValue = "") String query,
                                                   @RequestParam(value = "pageNumber", required = false, defaultValue = "0") int pageNumber,
                                                   @RequestParam(value = "pageSize", required = false, defaultValue = "10") int pageSize,
                                                   @RequestParam(value = "sort", required = false, defaultValue = "id") String sort,
                                                   @RequestParam(value = "sortDirection", required = false, defaultValue = "ASC") String sortDirection,
                                                   @Parameter(hidden = true) User user) {
        LOG.debug(String.format("/search for user %s", user.getEduPersonPrincipalName()));

        Role role = roleRepository.findById(roleId).orElseThrow(() -> new NotFoundException("Role not found"));

        UserPermissions.assertRoleAccess(user, role, Authority.INVITER);

        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.fromString(sortDirection), sort));
        Page<Map<String, Object>> page;
        if (StringUtils.hasText(query)) {
            page = guests ?
                    userRoleRepository.searchGuestsByPageWithKeyword(roleId, query, pageable) :
                    userRoleRepository.searchNonGuestsByPageWithKeyword(roleId, query, pageable);
        } else {
            page = guests ?
                    userRoleRepository.searchGuestsByPage(roleId, pageable) :
                    userRoleRepository.searchNonGuestsByPage(roleId, pageable);
        }
        return ResponseEntity.ok(page);
    }

    @PostMapping("user_role_provisioning")
    @Operation(summary = "Add Role to a User", description = "Provision the User if the User is unknown and add the Role(s)")
    public ResponseEntity<User> userRoleProvisioning(@Validated @RequestBody UserRoleProvisioning userRoleProvisioning,
                                                                     @Parameter(hidden = true) User apiUser) {
        userRoleProvisioning.validate();
        UserPermissions.assertInstitutionAdmin(apiUser);
        List<Role> roles = userRoleProvisioning.roleIdentifiers.stream()
                .map(roleId -> roleRepository.findById(roleId).orElseThrow(() -> new NotFoundException("Role not found")))
                .toList();
        UserPermissions.assertValidInvitation(apiUser, userRoleProvisioning.intendedAuthority, roles);
        Optional<User> userOptional = Optional.empty();

        if (StringUtils.hasText(userRoleProvisioning.sub)) {
            userOptional = userRepository.findBySubIgnoreCase(userRoleProvisioning.sub);
        } else if (StringUtils.hasText(userRoleProvisioning.eduPersonPrincipalName)) {
            userOptional = userRepository.findByEduPersonPrincipalNameIgnoreCase(userRoleProvisioning.eduPersonPrincipalName);
        } else if (StringUtils.hasText(userRoleProvisioning.email)) {
            userOptional = userRepository.findByEmailIgnoreCase(userRoleProvisioning.email);
        }
        //Provision user if not found - minimal requirement is an email
        User user = userOptional.orElseGet(() -> userRepository.save(new User(userRoleProvisioning)));

        List<UserRole> newUserRoles = roles.stream()
                .map(role ->
                    user.getUserRoles().stream()
                            .noneMatch(userRole -> userRole.getRole().getId().equals(role.getId())) ?
                        user.addUserRole(new UserRole(
                                apiUser.getName(),
                                user,
                                role,
                                userRoleProvisioning.intendedAuthority,
                                userRoleProvisioning.guestRoleIncluded,
                                Instant.now().plus(role.getDefaultExpiryDays(), ChronoUnit.DAYS)))
                        : null)
                .filter(Objects::nonNull)
                .toList();

        userRepository.save(user);
        AccessLogger.user(LOG, Event.Created, user);

        provisioningService.newUserRequest(user);
        newUserRoles.forEach(userRole -> provisioningService.updateGroupRequest(userRole, OperationType.Add));

        return ResponseEntity.status(201).body(user);
    }


    @PutMapping("")
    public ResponseEntity<Map<String, Integer>> updateUserRoleExpirationDate(@Validated @RequestBody UpdateUserRole updateUserRole,
                                                                             @Parameter(hidden = true) User user) {
        UserRole userRole = userRoleRepository.findById(updateUserRole.getUserRoleId()).orElseThrow(() -> new NotFoundException("UserRole not found"));
        if (updateUserRole.getEndDate() != null && !config.isPastDateAllowed() && Instant.now().isAfter(updateUserRole.getEndDate())) {
            throw new NotAllowedException("End date must be after now");
        }
        UserPermissions.assertValidInvitation(user, userRole.getAuthority(), List.of(userRole.getRole()));
        userRole.setEndDate(updateUserRole.getEndDate());
        userRoleRepository.save(userRole);
        //If there is a EVA provisioning, then update the account
        provisioningService.updateUserRoleRequest(userRole);
        return Results.createResult();
    }

    @DeleteMapping("/{id}/{isGuest}")
    public ResponseEntity<Void> deleteUserRole(@PathVariable("id") Long id,
                                               @PathVariable("isGuest") Boolean isGuest,
                                               @Parameter(hidden = true) User user) {
        LOG.debug("/deleteUserRole");
        UserRole userRole = userRoleRepository.findById(id).orElseThrow(() -> new NotFoundException("UserRole not found"));
        UserPermissions.assertValidInvitation(user, isGuest ? Authority.GUEST : userRole.getAuthority(), List.of(userRole.getRole()));
        if (userRole.isGuestRoleIncluded()) {
            userRole.setGuestRoleIncluded(false);
            if (!isGuest) {
                userRole.setAuthority(Authority.GUEST);
            }
            userRoleRepository.save(userRole);
            AccessLogger.userRole(LOG, Event.Updated, user, userRole);

        } else {
            provisioningService.updateGroupRequest(userRole, OperationType.Remove);
            provisioningService.deleteUserRoleRequest(userRole);
            userRoleRepository.deleteUserRoleById(id);
            AccessLogger.userRole(LOG, Event.Deleted, user, userRole);
        }
        return Results.deleteResult();
    }

}
