package access.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Getter
public class RemoteException extends ResponseStatusException {

    private final String reference;

    public RemoteException(HttpStatus status, String reason, Throwable cause) {
        super(status, reason, cause);
        this.reference = String.valueOf(Math.round(Math.random() * 10000));
    }

    @Override
    public String toString() {
        return "reference='" + reference + "' " + super.toString();
    }
}
