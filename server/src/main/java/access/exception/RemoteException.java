package access.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class RemoteException extends ResponseStatusException {

    private final String reference;

    public RemoteException(HttpStatus status, String reason, Throwable cause) {
        super(status, reason, cause);
        this.reference = String.valueOf(Math.round(Math.random() * 10000));
    }

    public String getReference() {
        return reference;
    }

    @Override
    public String toString() {
        return "reference='" + reference + "' " + super.toString();
    }
}
