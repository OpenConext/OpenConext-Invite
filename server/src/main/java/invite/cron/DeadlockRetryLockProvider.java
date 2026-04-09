package invite.cron;

import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.sql.SQLException;
import java.sql.SQLTransactionRollbackException;
import java.util.Optional;

public class DeadlockRetryLockProvider implements LockProvider {

    private static final Log LOG = LogFactory.getLog(DeadlockRetryLockProvider.class);

    private final LockProvider delegate;

    public DeadlockRetryLockProvider(LockProvider delegate) {
        this.delegate = delegate;
    }

    @Override
    public Optional<SimpleLock> lock(LockConfiguration lockConfiguration) {
        try {
            return delegate.lock(lockConfiguration);
        } catch (Exception e) {
            if (isDeadlock(e)) {
                LOG.warn(String.format("Deadlock acquiring ShedLock '%s', skipping cycle",
                        lockConfiguration.getName()));
                return Optional.empty();  // treat as "already locked", skip this cycle
            }
            throw e;  // rethrow non-deadlock exceptions as-is
        }
    }

    private boolean isDeadlock(Exception e) {
        Throwable cause = e;
        while (cause != null) {
            if (cause instanceof SQLTransactionRollbackException) {
                return true;
            }
            // MariaDB/MySQL error code 1213
            if (cause instanceof SQLException sql && sql.getErrorCode() == 1213) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

}