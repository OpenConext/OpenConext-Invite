package invite.cron;

import lombok.SneakyThrows;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public abstract class AbstractNodeLeader {

    private static final Log LOG = LogFactory.getLog(AbstractNodeLeader.class);

    private final String lockName;
    private final DataSource dataSource;

    protected AbstractNodeLeader(String lockName, DataSource dataSource) {
        this.lockName = lockName;
        this.dataSource = dataSource;
    }

    @SneakyThrows
    public void perform(String name, Executable executable) {
        Connection conn = null;
        boolean lockAcquired = false;

        try {
            conn = dataSource.getConnection();
            lockAcquired = tryGetLock(conn, lockName);

            if (!lockAcquired) {
                LOG.info(String.format("Another node is running %s, skipping this one", name));
                //Might be that there is a lock not cleaned up due to VM crash
                this.cleanupStaleLocks(conn, 60);
                return;
            }

            LOG.info(String.format("Lock acquired for %s", name));
            executable.execute();
            LOG.info(String.format("Executable %s completed successfully", name));
        } catch (Throwable e) {
            LOG.error(String.format("Error occurred in %s", name), e);
        } finally {
            if (lockAcquired) {
                try {
                    releaseLock(conn, lockName);
                    LOG.info(String.format("Lock released for %s", name));
                } catch (Exception e) {
                    LOG.error(String.format("Failed to release lock %s", name), e);
                }
            }

            if (conn != null) {
                try {
                    conn.close();
                } catch (Exception ignored) {
                    //Can't do anything about this
                    LOG.warn(String.format("Failed to close lock %s", name));
                }
            }
        }
    }

    protected boolean tryGetLock(Connection conn, String name) throws Exception {
        try {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO distributed_locks (lock_name, acquired_at) VALUES (?, NOW())")) {
                ps.setString(1, name);
                ps.executeUpdate();
                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                // Duplicate key or other constraint violation means lock is held
                return false;
            }
        } finally {
            conn.setAutoCommit(true);
        }
    }

    private void releaseLock(Connection conn, String name) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM distributed_locks WHERE lock_name = ?")) {
            ps.setString(1, name);
            ps.executeUpdate();
        }
    }

    private void cleanupStaleLocks(Connection conn, int timeoutMinutes) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM distributed_locks WHERE acquired_at < NOW() - INTERVAL ? MINUTE")) {
            ps.setInt(1, timeoutMinutes);
            ps.executeUpdate();
        }
    }

}
