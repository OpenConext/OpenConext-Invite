package db.mysql.migration;

import access.config.ObjectMapperHolder;
import access.manage.EntityType;
import access.model.Application;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import java.util.Set;
import java.util.UUID;


public class V11_0__migrate_applications extends BaseJavaMigration {

    public void migrate(Context context) {
        JdbcTemplate jdbcTemplate =
                new JdbcTemplate(new SingleConnectionDataSource(context.getConnection(), true));
        jdbcTemplate.query("SELECT id, manage_id, manage_type FROM roles", rs -> {
            long roleId = rs.getLong("id");
            jdbcTemplate.update("UPDATE roles SET identifier = ? WHERE id = ?", UUID.randomUUID().toString(), roleId);

            String manageId = rs.getString("manage_id");
            String manageType = rs.getString("manage_type");
            Set<Application> applications = Set.of(new Application(manageId, EntityType.valueOf(manageType)));
            String jsonNode;
            try {
                jsonNode =  ObjectMapperHolder.objectMapper.writeValueAsString(applications);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            jdbcTemplate.update("UPDATE roles SET applications = ?", jsonNode);
        });
    }
}
