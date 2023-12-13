package db.mysql.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import java.util.concurrent.atomic.AtomicReference;


public class V22_0__migrate_applications_per_role_landings_page extends BaseJavaMigration {

    public void migrate(Context context) {
        JdbcTemplate jdbcTemplate =
                new JdbcTemplate(new SingleConnectionDataSource(context.getConnection(), true));
        jdbcTemplate.query("SELECT role_id, application_id FROM roles_applications", rs -> {
            long roleId = rs.getLong("role_id");
            long applicationId = rs.getLong("application_id");
            AtomicReference<String> landingPageReference = new AtomicReference<>();
            //As initial value we use the landing page from the role, not the application
            jdbcTemplate.query(
                    "select landing_page from roles where id = ?",
                    resultSet -> {
                        landingPageReference.set(resultSet.getString("landing_page"));
                    },
                    roleId);
            jdbcTemplate.update("INSERT INTO `application_usages` (`landing_page` ,`role_id`,`application_id`) VALUES (?, ?, ?)",
                    landingPageReference.get(), roleId, applicationId);
        });
    }
}
