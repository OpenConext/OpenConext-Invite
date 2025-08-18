package invite.provision.scim;

import lombok.Getter;

import java.util.HashSet;
import java.util.List;

@Getter
public class Operation {

    private final OperationType op;
    private final String path = "members";
    private final List<Member> value;

    public Operation(OperationType op, List<String> remoteScimIdentifiers) {
        this.op = op;
        this.value = new HashSet<>(remoteScimIdentifiers).stream().map(Member::new).toList();
    }
}
