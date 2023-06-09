package access.manage;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ManageIdentifierTest {

    @Test
    void testEquals() {
        assertEquals(new ManageIdentifier("id", EntityType.SAML20_SP), new ManageIdentifier("id", EntityType.SAML20_SP));
        Set<ManageIdentifier> set = new HashSet<>();
        set.add(new ManageIdentifier("id", EntityType.SAML20_SP));
        set.add(new ManageIdentifier("id", EntityType.SAML20_SP));
        assertEquals(1, set.size());
    }

    @Test
    void testHashCode() {
        assertEquals(new ManageIdentifier("id", EntityType.SAML20_SP).hashCode(), new ManageIdentifier("id", EntityType.SAML20_SP).hashCode());
    }
}