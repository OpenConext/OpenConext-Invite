package access.provision;

public interface ProvisioningResponse {

    boolean isGraphResponse();

    String remoteIdentifier();

    boolean isErrorResponse();

}
