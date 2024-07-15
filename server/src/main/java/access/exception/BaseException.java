package access.exception;

public class BaseException extends RuntimeException {

    public BaseException(String message) {
        super(message);
    }

    protected boolean suppressStackTrace() {
        return false;
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this.suppressStackTrace() ? this : super.fillInStackTrace();
    }
}
