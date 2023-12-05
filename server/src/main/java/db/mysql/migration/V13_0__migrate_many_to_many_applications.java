package db.mysql.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;

import java.sql.PreparedStatement;
import java.sql.Statement;


public class V13_0__migrate_many_to_many_applications extends BaseJavaMigration {

    public void migrate(Context context) {
        JdbcTemplate jdbcTemplate =
                new JdbcTemplate(new SingleConnectionDataSource(context.getConnection(), true));
        jdbcTemplate.query("SELECT id, manage_id, manage_type FROM roles", rs -> {
            long roleId = rs.getLong("id");

            GeneratedKeyHolder generatedKeyHolder = new GeneratedKeyHolder();

            String sql = "INSERT INTO `applications` (`manage_id`, `manage_type`) VALUES(?, ?)";
            jdbcTemplate.update(conn -> {
                PreparedStatement preparedStatement = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                preparedStatement.setString(1, rs.getString("manage_id"));
                preparedStatement.setString(2, rs.getString("manage_type"));
                return preparedStatement;
            }, generatedKeyHolder);

            long applicationId = generatedKeyHolder.getKey().longValue();
            jdbcTemplate.update("INSERT INTO `roles_applications` (`role_id`,`application_id`) VALUES (?, ?)",
                    roleId, applicationId);
        });


    }
}
