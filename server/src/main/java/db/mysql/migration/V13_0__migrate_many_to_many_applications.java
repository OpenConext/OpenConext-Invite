package db.mysql.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Map;


public class V13_0__migrate_many_to_many_applications extends BaseJavaMigration {

    public void migrate(Context context) {
        JdbcTemplate jdbcTemplate =
                new JdbcTemplate(new SingleConnectionDataSource(context.getConnection(), true));
        jdbcTemplate.query("SELECT id, manage_id, manage_type FROM roles", rs -> {
            long roleId = rs.getLong("id");

            GeneratedKeyHolder generatedKeyHolder = new GeneratedKeyHolder();
            String manageId = rs.getString("manage_id");
            String manageType = rs.getString("manage_type");

            long applicationId;

            List<Map<String, Object>> maps = jdbcTemplate.queryForList("SELECT `id`, `manage_id`, `manage_type` from `applications` WHERE " +
                    "`manage_id` = ? AND `manage_type` = ?", manageId, manageType);
            if (maps.isEmpty()) {
                String sql = "INSERT INTO `applications` (`manage_id`, `manage_type`) VALUES(?, ?)";
                jdbcTemplate.update(conn -> {
                    PreparedStatement preparedStatement = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                    preparedStatement.setString(1, manageId);
                    preparedStatement.setString(2, manageType);
                    return preparedStatement;
                }, generatedKeyHolder);
                applicationId = generatedKeyHolder.getKey().longValue();
            } else {
                applicationId = (long) maps.get(0).get("id");
            }
            jdbcTemplate.update("INSERT INTO `roles_applications` (`role_id`,`application_id`) VALUES (?, ?)",
                    roleId, applicationId);
        });


    }
}
