package access.api;

import access.mail.MailBox;
import access.manage.Manage;
import access.repository.InvitationRepository;
import access.repository.RoleRepository;

public interface InvitationResource {

    RoleRepository getRoleRepository();

    InvitationRepository getInvitationRepository();

    Manage getManage();

    MailBox getMailBox();
}
