package invite.cron;

import lombok.SneakyThrows;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public abstract class AbstractNodeLeader {

    private static final Log LOG = LogFactory.getLog(AbstractNodeLeader.class);

    private final String lockName;
    private final int timeoutSeconds;
    private final DataSource dataSource;

    protected AbstractNodeLeader(String lockName, int timeoutSeconds, DataSource dataSource) {
        this.lockName = lockName;
        this.timeoutSeconds = timeoutSeconds;
        this.dataSource = dataSource;
    }

    @SneakyThrows
    public void perform(String name, Executable executable) {


        Connection conn = null;
        boolean lockAcquired = false;

        try {
            conn = dataSource.getConnection();
            lockAcquired = tryGetLock(conn, lockName, timeoutSeconds);

            if (!lockAcquired) {
                LOG.info(String.format("Another node is running %s, skipping this one", name));
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
                }
            }
        }
    }

    private boolean tryGetLock(Connection conn, String name, int timeoutSec) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement("SELECT GET_LOCK(?, ?)")) {
            ps.setString(1, name);
            ps.setInt(2, timeoutSec);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int result = rs.getInt(1);
                    return !rs.wasNull() && result == 1;
                }
                return false;
            }
        }
    }

    private void releaseLock(Connection conn, String name) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement("SELECT RELEASE_LOCK(?)")) {
            ps.setString(1, name);
            ps.executeQuery(); // ignore result
        }
    }

}
