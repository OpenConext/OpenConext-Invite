package db.mysql.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import java.util.concurrent.atomic.AtomicReference;

//Only change the column of the table exists, otherwise the CI build fails
public class V31_0__session_attributes_medium_blob extends BaseJavaMigration {

    public void migrate(Context context) {
        JdbcTemplate jdbcTemplate =
                new JdbcTemplate(new SingleConnectionDataSource(context.getConnection(), true));
        jdbcTemplate.query("SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES " +
                "WHERE TABLE_SCHEMA = 'invite' " +
                "AND TABLE_NAME = 'SPRING_SESSION_ATTRIBUTES'", rs -> {
            jdbcTemplate.update("ALTER TABLE SPRING_SESSION_ATTRIBUTES " +
                    "CHANGE COLUMN ATTRIBUTE_BYTES ATTRIBUTE_BYTES MEDIUMBLOB");
        });
    }
}
