package access.security;

import access.model.User;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class InstitutionAdminTest {

    private final static String entitlement = "urn:mace:surfnet.nl:surfnet.nl:sab:role:SURFconextverantwoordelijke";
    private final static String organizationGuidPrefix = "urn:mace:surfnet.nl:surfnet.nl:sab:organizationGUID:";

    @Test
    void isInstitutionAdmin() {
        User user = new User();
        assertFalse(InstitutionAdmin.isInstitutionAdmin(user));

        user.setInstitutionAdmin(true);
        assertFalse(InstitutionAdmin.isInstitutionAdmin(user));

        user.setOrganizationGUID(UUID.randomUUID().toString());
        assertTrue(InstitutionAdmin.isInstitutionAdmin(user));

        user.setInstitutionAdminByInvite(true);
        user.setOrganizationGUID(null);
        assertFalse(InstitutionAdmin.isInstitutionAdmin(user));

        user.setInstitutionAdminByInvite(true);
        user.setOrganizationGUID(UUID.randomUUID().toString());
        assertTrue(InstitutionAdmin.isInstitutionAdmin(user));
    }

    @Test
    void testIsInstitutionAdmin() {
        assertFalse(InstitutionAdmin.isInstitutionAdmin(Map.of(), entitlement));
        assertFalse(InstitutionAdmin.isInstitutionAdmin(Map.of("eduperson_entitlement", List.of()), entitlement));
        assertFalse(InstitutionAdmin.isInstitutionAdmin(Map.of("eduperson_entitlement", List.of("nope")), entitlement));

        assertTrue(InstitutionAdmin.isInstitutionAdmin(Map.of("eduperson_entitlement", List.of(entitlement)), entitlement));
    }

    @Test
    void getOrganizationGuid() {
        assertFalse(InstitutionAdmin.getOrganizationGuid(Map.of(), organizationGuidPrefix, Optional.empty()).isPresent());
        assertFalse(InstitutionAdmin.getOrganizationGuid(Map.of("eduperson_entitlement", List.of("nope")),
                organizationGuidPrefix,
                Optional.empty()).isPresent());

        Optional<String> organizationGuid = InstitutionAdmin.getOrganizationGuid(Map.of("eduperson_entitlement", List.of(organizationGuidPrefix + " ")),
                organizationGuidPrefix,
                Optional.empty());
        assertFalse(organizationGuid.isPresent());

        String guid = UUID.randomUUID().toString();
        organizationGuid = InstitutionAdmin.getOrganizationGuid(Map.of("eduperson_entitlement", List.of(organizationGuidPrefix + guid)),
                organizationGuidPrefix,
                Optional.empty());
        assertEquals(guid, organizationGuid.get());

        User user = new User();
        assertFalse(InstitutionAdmin.getOrganizationGuid(Map.of(), organizationGuidPrefix, Optional.of(user)).isPresent());

        user.setOrganizationGUID("  ");
        organizationGuid = InstitutionAdmin.getOrganizationGuid(Map.of(), organizationGuidPrefix, Optional.of(user));
        assertFalse(organizationGuid.isPresent());

        user.setOrganizationGUID(guid);
        organizationGuid = InstitutionAdmin.getOrganizationGuid(Map.of(), organizationGuidPrefix, Optional.of(user));
        assertEquals(guid, organizationGuid.get());
    }
}