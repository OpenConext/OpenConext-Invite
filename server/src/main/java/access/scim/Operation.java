package access.scim;

import access.model.RemoteProvisionedUser;
import access.model.UserRole;
import lombok.Getter;

import java.util.Collections;
import java.util.List;

@Getter
public class Operation {

    private final OperationType op;
    private final String path = "members";
    private final List<Member> value;

    public Operation(OperationType op, List<String> remoteScimIdentifiers) {
        this.op = op;
        this.value = remoteScimIdentifiers.stream().map(Member::new).toList();
    }
}
