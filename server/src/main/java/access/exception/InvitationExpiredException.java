package access.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.GONE)
public class InvitationExpiredException extends RuntimeException {

    public InvitationExpiredException(String message) {
        super(message);
    }
}
