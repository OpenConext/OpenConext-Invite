package access.model;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.util.StringUtils;

import java.util.List;

@ToString
@AllArgsConstructor
@NoArgsConstructor
public class UserRoleProvisioning {

    @NotEmpty
    public List<Long> roleIdentifiers;
    public Authority intendedAuthority = Authority.GUEST;
    public String sub;
    public String email;
    public String eduPersonPrincipalName;
    public String givenName;
    public String familyName;
    public String name;
    public String schacHomeOrganization;

    public void validate() {
        if (!StringUtils.hasText(email) && !StringUtils.hasText(eduPersonPrincipalName)) {
            throw new IllegalArgumentException("Requires one off: email, eduPersonPrincipalName. Invalid userRoleProvisioning: " + this);
        }
    }

}
