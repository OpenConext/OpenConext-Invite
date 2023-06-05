package access;

import access.manage.EntityType;
import access.model.*;
import access.repository.InvitationRepository;
import access.repository.RoleRepository;
import access.repository.UserRepository;
import access.repository.UserRoleRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Arrays;
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


        User superUser =
                new User(true, "super@example.com", "super@example.com", "David", "Doe", "super@example.com");
        User manager =
                new User(false, "manager@example.com", "manager@example.com", "Mary", "Doe", "manager@example.com");
        User inviter =
                new User(false, "inviter@example.com", "inviter@example.com", "Paul", "Doe", "inviter@example.com");
        User guest =
                new User(false, "guest@example.com", "guest@example.com", "Ann", "Doe", "guest@example.com");
        doSave(this.userRepository, superUser, manager, inviter, guest);

        Role wiki =
                new Role("Wiki", "Wiki desc", "1", EntityType.SAML20_SP);
        Role network =
                new Role("Network", "Network desc", "2", EntityType.SAML20_SP);
        Role storage =
                new Role("Storage", "Storage desc", "3", EntityType.SAML20_SP);
        Role research =
                new Role("Research", "Research desc", "4", EntityType.SAML20_SP);
        Role calendar =
                new Role("Calendar", "Calendar desc", "5", EntityType.OIDC10_RP);
        Role mail =
                new Role("Mail", "Mail desc", "5", EntityType.OIDC10_RP);
        doSave(this.roleRepository, wiki, network, storage, research, calendar, mail);

        UserRole wikiManager =
                new UserRole("system", manager, wiki, Authority.MANAGER);
        UserRole calendarInviter =
                new UserRole("system", inviter, calendar, Authority.INVITER);
        UserRole mailInviter =
                new UserRole("system", inviter, mail, Authority.INVITER);
        UserRole storageGuest =
                new UserRole("system", guest, storage, Authority.GUEST);
        doSave(this.userRoleRepository, wikiManager, calendarInviter, mailInviter, storageGuest);

        Invitation superUserInvitation =
                new Invitation(Authority.SUPER_USER, Authority.SUPER_USER.name(), "super_user@new.com", false,
                        inviter, Set.of());
        Invitation managerInvitation =
                new Invitation(Authority.MANAGER, Authority.MANAGER.name(), "manager@new.com", false,
                        inviter, Set.of(new InvitationRole(research)));
        Invitation inviterInvitation =
                new Invitation(Authority.INVITER, Authority.INVITER.name(), "inviter@new.com", false,
                        inviter, Set.of(new InvitationRole(calendar), new InvitationRole(mail)));
        Invitation guestInvitation =
                new Invitation(Authority.GUEST, Authority.GUEST.name(), "guest@new.com", false,
                        inviter, Set.of(new InvitationRole(mail)));
        doSave(invitationRepository, superUserInvitation, managerInvitation, inviterInvitation, guestInvitation);
    }

    private <M> void doSave(JpaRepository<M, Long> repository, M... entities) {
        repository.saveAll(Arrays.asList(entities));
    }
}
