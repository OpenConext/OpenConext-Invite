package access.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class InvitationStatusException extends BaseException {

    public InvitationStatusException(String message) {
        super(message);
    }

    @Override
    protected boolean suppressStackTrace() {
        return true;
    }

}
