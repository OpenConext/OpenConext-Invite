package access;

import access.manage.EntityType;
import access.model.*;
import access.repository.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Set;

public record Seed(InvitationRepository invitationRepository,
                   RemoteProvisionedGroupRepository remoteProvisionedGroupRepository,
                   RemoteProvisionedUserRepository remoteProvisionedUserRepository,
                   RoleRepository roleRepository,
                   UserRepository userRepository,
                   UserRoleRepository userRoleRepository
) {

    public static final String SUPER_SUB = "urn:collab:person:example.com:super";
    public static final String MANAGE_SUB = "urn:collab:person:example.com:manager";
    public static final String INVITER_SUB = "urn:collab:person:example.com:inviter";
    public static final String GUEST_SUB = "urn:collab:person:example.com:guest";
    public static final String GRAPH_INVITATION_HASH = "graph_invitation_hash";

    public void doSeed() {
        this.invitationRepository.deleteAllInBatch();
        this.remoteProvisionedGroupRepository.deleteAllInBatch();
        this.remoteProvisionedUserRepository.deleteAllInBatch();
        this.roleRepository.deleteAllInBatch();
        this.userRepository.deleteAllInBatch();
        this.userRoleRepository.deleteAllInBatch();


        User superUser =
                new User(true, SUPER_SUB, SUPER_SUB, "example.com", "David", "Doe", "david.doe@examole.com");
        User manager =
                new User(false, MANAGE_SUB, MANAGE_SUB, "example.com", "Mary", "Doe", "mary.doe@examole.com");
        User inviter =
                new User(false, INVITER_SUB, INVITER_SUB, "example.com", "Paul", "Doe", "paul.doe@examole.com");
        User guest =
                new User(false, GUEST_SUB, GUEST_SUB, "example.com", "Ann", "Doe", "ann.doe@examole.com");
        doSave(this.userRepository, superUser, manager, inviter, guest);

        Role wiki =
                new Role("Wiki", "Wiki desc", "https://landingpage.com", "1", EntityType.SAML20_SP, 365);
        Role network =
                new Role("Network", "Network desc", "https://landingpage.com", "2", EntityType.SAML20_SP, 365);
        Role storage =
                new Role("Storage", "Storage desc", "https://landingpage.com", "3", EntityType.SAML20_SP, 365);
        Role research =
                new Role("Research", "Research desc", "https://landingpage.com", "4", EntityType.SAML20_SP, 365);
        Role calendar =
                new Role("Calendar", "Calendar desc", "https://landingpage.com", "5", EntityType.OIDC10_RP, 365);
        Role mail =
                new Role("Mail", "Mail desc", "https://landingpage.com", "5", EntityType.OIDC10_RP, 365);
        doSave(this.roleRepository, wiki, network, storage, research, calendar, mail);

        UserRole wikiManager =
                new UserRole("system", manager, wiki, Authority.MANAGER);
        UserRole calendarInviter =
                new UserRole("system", inviter, calendar, Authority.INVITER);
        UserRole mailInviter =
                new UserRole("system", inviter, mail, Authority.INVITER);
        UserRole storageGuest =
                new UserRole("system", guest, storage, Authority.GUEST);
        UserRole wikiGuest =
                new UserRole("system", guest, wiki, Authority.GUEST);
        UserRole researchGuest =
                new UserRole("system", guest, research, Authority.GUEST);
        doSave(this.userRoleRepository, wikiManager, calendarInviter, mailInviter, storageGuest, wikiGuest, researchGuest);

        String message = "Please join..";
        Instant roleExpirydate = Instant.now().plus(365, ChronoUnit.DAYS);

        Invitation superUserInvitation =
                new Invitation(Authority.SUPER_USER, Authority.SUPER_USER.name(), "super_user@new.com", false, message,
                        inviter, roleExpirydate, Set.of());
        Invitation managerInvitation =
                new Invitation(Authority.MANAGER, Authority.MANAGER.name(), "manager@new.com", false, message,
                        inviter, roleExpirydate, Set.of(new InvitationRole(research)));
        Invitation inviterInvitation =
                new Invitation(Authority.INVITER, Authority.INVITER.name(), "inviter@new.com", false, message,
                        inviter, roleExpirydate, Set.of(new InvitationRole(calendar), new InvitationRole(mail)));
        inviterInvitation.setEnforceEmailEquality(true);
        Invitation guestInvitation =
                new Invitation(Authority.GUEST, Authority.GUEST.name(), "guest@new.com", false, message,
                        inviter, roleExpirydate, Set.of(new InvitationRole(mail)));
        guestInvitation.setEduIDOnly(true);
        Invitation graphInvitation =
                new Invitation(Authority.GUEST, GRAPH_INVITATION_HASH, "graph@new.com", false, message,
                        inviter, roleExpirydate, Set.of(new InvitationRole(network)));
        doSave(invitationRepository, superUserInvitation, managerInvitation, inviterInvitation, guestInvitation, graphInvitation);
    }

    @SafeVarargs
    private <M> void doSave(JpaRepository<M, Long> repository, M... entities) {
        repository.saveAll(Arrays.asList(entities));
    }
}
