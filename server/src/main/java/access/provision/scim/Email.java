package access.provision.scim;

import lombok.Getter;

import java.io.Serializable;

@Getter
public class Email implements Serializable {

    private final String type = "other";
    private final String value;

    public Email(String value) {
        this.value = value;
    }
}
