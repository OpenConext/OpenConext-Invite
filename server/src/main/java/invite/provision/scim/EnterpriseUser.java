package invite.provision.scim;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.Serializable;

@AllArgsConstructor
@Getter
public class EnterpriseUser implements Serializable {

    private final String organization;

}
