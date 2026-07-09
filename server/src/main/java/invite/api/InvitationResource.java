package invite.api;

import invite.mail.MailBox;
import invite.manage.Manage;
import invite.repository.ApplicationRepository;
import invite.repository.InvitationRepository;
import invite.repository.RoleRepository;

public interface InvitationResource {

    RoleRepository getRoleRepository();

    InvitationRepository getInvitationRepository();

    ApplicationRepository getApplicationRepository();

    Manage getManage();

    MailBox getMailBox();
}
