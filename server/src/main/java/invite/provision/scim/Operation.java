package invite.provision.scim;

public interface Operation {

    OperationType getOp();

    String getPath();
}
