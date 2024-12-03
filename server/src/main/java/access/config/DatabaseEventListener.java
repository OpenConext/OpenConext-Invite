package access.config;

import access.model.Role;
import access.provision.scim.GroupURN;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Component
public class DatabaseEventListener {

    private static final Log LOG = LogFactory.getLog(DatabaseEventListener.class);

    private final DataSource dataSource;
    private final String groupUrnPrefix;

    public DatabaseEventListener(DataSource dataSource,
                                 @Value("${voot.group_urn_domain}") String groupUrnPrefix) {
        this.dataSource = dataSource;
        this.groupUrnPrefix = groupUrnPrefix;
    }


    @EventListener
    public void onApplicationEvent(ContextRefreshedEvent event) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.query("SELECT id, identifier, short_name FROM roles WHERE urn IS NULL AND teams_origin = 0", rs -> {
            long roleId = rs.getLong("id");
            String shortName = rs.getString("short_name");
            String identifier = rs.getString("identifier");
            Role role = new Role();
            role.setTeamsOrigin(false);
            role.setIdentifier(identifier);
            role.setShortName(shortName);

            String urn = GroupURN.urnFromRole(this.groupUrnPrefix, role);
            jdbcTemplate.update("UPDATE roles SET urn = ? WHERE id = ?", urn, roleId);

            LOG.info(String.format("Update role %s with urn %s", shortName, urn));
        });
    }
}
