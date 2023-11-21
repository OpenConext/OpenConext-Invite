package access.db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.sql.PreparedStatement;
import java.util.UUID;


public class V11__migrate_applications extends BaseJavaMigration {

    public void migrate(Context context) {
        JdbcTemplate jdbcTemplate =
                new JdbcTemplate(new SingleConnectionDataSource(context.getConnection(), true));

        jdbcTemplate.query("SELECT id, manage_id, manage_type FROM roles", rs -> {
            long roleId = rs.getLong("id");
            jdbcTemplate.update("UPDATE roles SET identifier = ? WHERE id = ?", UUID.randomUUID().toString(), roleId);

            String manageId = rs.getString("manage_id");
            String manageType = rs.getString("manage_type");

            KeyHolder keyHolder = new GeneratedKeyHolder();

            jdbcTemplate.update(connection -> {
                PreparedStatement preparedStatement = connection.prepareStatement(
                        "INSERT INTO applications (manage_id, manage_type) VALUES (?, ?)"
                );
                preparedStatement.setString(1, manageId);
                preparedStatement.setString(2, manageType);
                return preparedStatement;
            }, keyHolder);

            //now we create the relationship
            jdbcTemplate.update("INSERT INTO roles_applications (role_id, application_id) VALUES (?, ?)", roleId, keyHolder.getKeyAs(Long.class));
        });
    }
}
