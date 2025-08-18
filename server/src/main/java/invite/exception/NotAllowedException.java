package invite.exception;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class NotAllowedException extends AuthenticationException {

    public NotAllowedException(String msg) {
        super(msg);
    }
}
