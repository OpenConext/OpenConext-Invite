package access.provision.scim;

import lombok.Getter;

import java.io.Serializable;

@Getter
public class Email implements Serializable {

    private final String type;
    private final String value;

    public Email(String type, String value) {
        this.type = type;
        this.value = value;
    }
}
