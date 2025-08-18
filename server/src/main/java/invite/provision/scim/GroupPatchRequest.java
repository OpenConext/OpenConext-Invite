package invite.provision.scim;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

@Getter
public class GroupPatchRequest implements Serializable {

    private final List<String> schemas = Collections.singletonList("urn:ietf:params:scim:api:messages:2.0:PatchOp");
    @JsonProperty("Operations")
    private final List<Operation> operations;

    public GroupPatchRequest(String externalId, String id, Operation operation) {
        this.operations = Collections.singletonList(operation);
    }
}
