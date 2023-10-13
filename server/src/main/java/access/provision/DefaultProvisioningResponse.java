package access.provision;

public record DefaultProvisioningResponse(String remoteIdentifier) implements ProvisioningResponse {

    @Override
    public boolean isGraphResponse() {
        return false;
    }

}
