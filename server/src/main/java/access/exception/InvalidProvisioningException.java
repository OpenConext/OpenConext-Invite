package access.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidProvisioningException extends RuntimeException {
    public InvalidProvisioningException(String s) {
        super(s);
    }
}
