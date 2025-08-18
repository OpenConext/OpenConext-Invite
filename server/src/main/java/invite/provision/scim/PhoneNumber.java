package invite.provision.scim;

import lombok.Getter;

import java.io.Serializable;

@Getter
public class PhoneNumber implements Serializable {

    private final String type = "other";
    private final String value;

    public PhoneNumber(String value) {
        this.value = value;
    }
}
