package access.manage;

import org.junit.jupiter.api.Test;
import org.mockito.internal.util.collections.HashCodeAndEqualsSafeSet;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class IdentityTest {

    @Test
    void testEquals() {
        assertEquals(new Identity("id", EntityType.SAML20_SP), new Identity("id", EntityType.SAML20_SP));
        Set<Identity> set = new HashSet<>();
        set.add(new Identity("id", EntityType.SAML20_SP));
        set.add(new Identity("id", EntityType.SAML20_SP));
        assertEquals(1, set.size());
    }

    @Test
    void testHashCode() {
        assertEquals(new Identity("id", EntityType.SAML20_SP).hashCode(), new Identity("id", EntityType.SAML20_SP).hashCode());
    }
}