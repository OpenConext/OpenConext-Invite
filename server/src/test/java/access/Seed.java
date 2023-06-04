package access;

import access.manage.EntityType;
import access.model.*;
import access.repository.InvitationRepository;
import access.repository.RoleRepository;
import access.repository.UserRepository;
import access.repository.UserRoleRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Map;
import java.util.Set;

public record Seed(RoleRepository roleRepository,
                   UserRepository userRepository,
                   UserRoleRepository userRoleRepository,
                   InvitationRepository invitationRepository) {

    public void doSeed() {
        this.userRepository.deleteAllInBatch();
        this.roleRepository.deleteAllInBatch();
        this.userRoleRepository.deleteAllInBatch();
        this.invitationRepository.deleteAllInBatch();

        Map<String, User> users = Map.of(
                Authority.SUPER_USER.name(),
                new User(true, "super@example.com", "super@example.com", "David", "Doe", "super@example.com"),
                Authority.MANAGER.name(),
                new User(false, "manager@example.com", "manager@example.com", "Mary", "Doe", "manager@example.com"),
                Authority.INVITER.name(),
                new User(false, "inviter@example.com", "inviter@example.com", "Paul", "Doe", "inviter@example.com"),
                Authority.GUEST.name(),
                new User(false, "guest@example.com", "guest@example.com", "Ann", "Doe", "guest@example.com")
        );
        doSave(this.userRepository, users);

        Map<String, Role> roles = Map.of(
                "wiki",
                new Role("Wiki", "Wiki desc", "1", EntityType.SAML20_SP),
                "network",
                new Role("Network", "Network desc", "2", EntityType.SAML20_SP),
                "storage",
                new Role("Storage", "Storage desc", "3", EntityType.SAML20_SP),
                "research",
                new Role("Research", "Research desc", "4", EntityType.SAML20_SP),
                "calendar",
                new Role("Calendar", "Calendar desc", "5", EntityType.OIDC10_RP),
                "mail",
                new Role("Mail", "Mail desc", "5", EntityType.OIDC10_RP)
        );
        doSave(this.roleRepository, roles);

        Map<String, UserRole> userRoles = Map.of(
                "wiki_manager",
                new UserRole("system", users.get(Authority.MANAGER.name()), roles.get("wiki"), Authority.MANAGER),
                "network_inviter",
                new UserRole("system", users.get(Authority.INVITER.name()), roles.get("network"), Authority.INVITER),
                "storage_guest",
                new UserRole("system", users.get(Authority.GUEST.name()), roles.get("storage"), Authority.GUEST)
        );
        doSave(this.userRoleRepository, userRoles);

        User inviter = users.get(Authority.SUPER_USER.name());
        Map<String, Invitation> invitations = Map.of(
                Authority.SUPER_USER.name(),
                new Invitation(Authority.SUPER_USER, Authority.SUPER_USER.name(), "super_user@new.com", false,
                        inviter, Set.of()),
                Authority.MANAGER.name(),
                new Invitation(Authority.MANAGER, Authority.MANAGER.name(), "manager@new.com", false,
                        inviter, Set.of(new InvitationRole(roles.get("research")))),
                Authority.INVITER.name(),
                new Invitation(Authority.INVITER, Authority.INVITER.name(), "inviter@new.com", false,
                        inviter, Set.of(new InvitationRole(roles.get("calendar")), new InvitationRole(roles.get("mail")))),
                Authority.GUEST.name(),
                new Invitation(Authority.GUEST, Authority.GUEST.name(), "guest@new.com", false,
                        inviter, Set.of(new InvitationRole(roles.get("mail"))))
        );
        doSave(invitationRepository, invitations);
    }

    private <M> void doSave(JpaRepository<M, Long> repository, Map<String, M> entities) {
        repository.saveAll(entities.values());
    }
}
