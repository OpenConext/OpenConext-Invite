package invite.crm;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class CRMContact {

    private String contactId;
    private String firstname;
    private String middlename;
    private String surname;
    private String email;
    private CRMOrganisation organisation;
    private List<CRMRole> roles;
}
