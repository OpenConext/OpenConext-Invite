package access.provision;

import access.provision.graph.GraphResponse;

public interface ProvisioningResponse {

    boolean isGraphResponse();

    String remoteIdentifier();

    boolean isErrorResponse();
}
