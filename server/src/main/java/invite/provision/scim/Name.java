package invite.provision.scim;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.Serializable;

@AllArgsConstructor
@Getter
public class Name implements Serializable {

    private String formatted;
    private String familyName;
    private String givenName;

}
