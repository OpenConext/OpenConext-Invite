package access.provision.scim;

public class ThreadLocalSCIMFailureStrategy {

    private static final ThreadLocal<Boolean> contextHolder = new ThreadLocal<>();

    private ThreadLocalSCIMFailureStrategy() {
    }

    public static Boolean ignoreFailures() {
        return contextHolder.get() != null;
    }

    public static void startIgnoringFailures() {
        contextHolder.set(Boolean.TRUE);
    }

    public static void stopIgnoringFailures() {
        contextHolder.remove();
    }

}
