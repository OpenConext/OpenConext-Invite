package invite.provision.scim;

import lombok.Getter;

import java.util.HashSet;
import java.util.List;

@Getter
public class DisplayNameOperation implements Operation {

    private final OperationType op = OperationType.replace;
    private final String path = "displayName";
    private final String value;

    public DisplayNameOperation(String value) {
        this.value = value;
    }

}
