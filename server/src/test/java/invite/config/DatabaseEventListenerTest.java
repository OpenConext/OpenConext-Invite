package invite.config;

import invite.AbstractTest;
import invite.model.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.support.StaticApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DatabaseEventListenerTest extends AbstractTest {

    @Autowired
    private DatabaseEventListener databaseEventListener;

    @Test
    void onApplicationEvent() {
        Role role = roleRepository.findByName("Storage").get();
        role.setUrn(null);
        role.setTeamsOrigin(false);
        roleRepository.save(role);

        databaseEventListener.onApplicationEvent(new ContextRefreshedEvent(new StaticApplicationContext()));

        Role updatedRole = roleRepository.findByName("Storage").get();

        assertEquals(String.format("urn:mace:surf.nl:test.surfaccess.nl:%s:%s",
                role.getIdentifier(),role.getShortName()), updatedRole.getUrn());
    }
}