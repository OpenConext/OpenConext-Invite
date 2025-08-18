package invite.manage;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ManageIdentifierTest {

    @Test
    void testEquals() {
        ManageIdentifier one = new ManageIdentifier("id", EntityType.SAML20_SP);
        ManageIdentifier two = new ManageIdentifier("id", EntityType.SAML20_SP);
        assertEquals(one, two);
        assertEquals(one.hashCode(), two.hashCode());

        Set<ManageIdentifier> set = new HashSet<>();
        set.add(one);
        set.add(two);
        assertEquals(1, set.size());
    }

}