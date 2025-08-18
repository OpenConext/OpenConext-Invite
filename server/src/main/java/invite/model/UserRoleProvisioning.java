package invite.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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
    @NotNull
    public String email;
    public String eduPersonPrincipalName;
    public String givenName;
    public String familyName;
    public String name;
    public String schacHomeOrganization;
    public boolean guestRoleIncluded;

    public void validate() {
        if (!StringUtils.hasText(email) && (!StringUtils.hasText(eduPersonPrincipalName) || !eduPersonPrincipalName.contains("@"))) {
            throw new IllegalArgumentException("Requires one off: email, eduPersonPrincipalName. Invalid userRoleProvisioning: " + this);
        }
    }

    @JsonIgnore
    public String resolveSub() {
        if (StringUtils.hasText(sub)) {
            return sub;
        }
        String schacHome = null;
        String uid = null;
        if (StringUtils.hasText(schacHomeOrganization)) {
            schacHome = schacHomeOrganization;
        }
        String eppn = eduPersonPrincipalName;
        if (StringUtils.hasText(eppn) && eppn.contains("@")) {
            uid = eppn.substring(0, eppn.indexOf("@"));
            schacHome = schacHome != null ? schacHome : eppn.substring(eppn.indexOf("@") + 1);
        }
        String mail = email;
        if (StringUtils.hasText(mail)) {
            uid = uid != null ? uid : mail.substring(0, mail.indexOf("@"));
            schacHome = schacHome != null ? schacHome : mail.substring(mail.indexOf("@") + 1);
        }
        if (schacHome == null || uid == null) {
            throw new IllegalArgumentException("Can't resolve sub from " + this);
        }
        if (!StringUtils.hasText(this.schacHomeOrganization) && StringUtils.hasText(schacHome)) {
            this.schacHomeOrganization = schacHome;
        }
        return String.format("urn:collab:person:%s:%s", schacHome, uid);
    }


}
