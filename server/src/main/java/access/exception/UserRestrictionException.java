package access.exception;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class UserRestrictionException extends AuthenticationException {

    public UserRestrictionException(String msg) {
        super(msg);
    }
}
