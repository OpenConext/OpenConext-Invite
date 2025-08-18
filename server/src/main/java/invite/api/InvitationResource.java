package invite.api;

import invite.mail.MailBox;
import invite.manage.Manage;
import invite.repository.InvitationRepository;
import invite.repository.RoleRepository;

public interface InvitationResource {

    RoleRepository getRoleRepository();

    InvitationRepository getInvitationRepository();

    Manage getManage();

    MailBox getMailBox();
}
