package invite.cron;

@FunctionalInterface
public interface Executable {

        void execute() throws Throwable;

}
