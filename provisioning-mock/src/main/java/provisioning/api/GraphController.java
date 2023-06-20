package provisioning.api;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import provisioning.config.Database;
import provisioning.config.ProvisioningType;

import java.util.Map;

@RestController
@RequestMapping(value = "/graph", produces = MediaType.APPLICATION_JSON_VALUE)
public class GraphController {

    private static final Log LOG = LogFactory.getLog(GraphController.class);

    private final Map<String, Map<String, Object>> users;
    private final Map<String, Map<String, Object>> groups;

    @Autowired
    public GraphController(Database database) {
        this.users = database.users(ProvisioningType.scim);
        this.groups = database.users(ProvisioningType.scim);
    }

}
