package invite.crm;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CRMContact {

    private String contactId;
    private String firstname;
    private String middlename;
    private String surname;
    private String email;
    private CRMOrganisation organisation;
    private List<CRMRole> roles;
}
