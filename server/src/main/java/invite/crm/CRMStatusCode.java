package invite.crm;

import lombok.Getter;

public enum CRMStatusCode {

    InProcess("In process", "in_process"),
    Paired("Paired","paired"),
    NotPaired("Not paired","not_paired");

    @Getter
    private final String statusCode;
    @Getter
    private final String status;

    CRMStatusCode(String status, String statusCode) {
        this.status = status;
        this.statusCode = statusCode;
    }
}
