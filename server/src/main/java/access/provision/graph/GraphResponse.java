package access.provision.graph;

import access.provision.ProvisioningResponse;

public record GraphResponse(String remoteIdentifier, String inviteRedeemUrl,
                            boolean errorResponse) implements ProvisioningResponse {

    @Override
    public boolean isGraphResponse() {
        return true;
    }

    @Override
    public boolean isErrorResponse() {
        return errorResponse;
    }

}
