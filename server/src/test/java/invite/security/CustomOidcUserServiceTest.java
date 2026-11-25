package invite.security;

import invite.AbstractTest;
import invite.AccessCookieFilter;
import invite.model.User;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomOidcUserServiceTest extends AbstractTest {

    private AccessCookieFilter login(String sub, UnaryOperator<Map<String, Object>> userInfoEnhancer) throws Exception {
        stubForManageProvisioning(List.of());
        stubForManageProvidersAllowedByIdP(ORGANISATION_GUID);
        return openIDConnectFlow("/api/v1/users/login", sub, userInfoEnhancer);
    }

    private User user(String sub) {
        return userRepository.findBySubIgnoreCase(sub).orElseThrow();
    }

    private int institutionAdminsStatus(AccessCookieFilter accessCookieFilter) {
        return given()
                .when()
                .filter(accessCookieFilter.cookieFilter())
                .accept(ContentType.JSON)
                .get("/api/v1/users/institutionAdmins")
                .statusCode();
    }

    @Test
    void downgradeInstitutionAdminByEntitlement() throws Exception {
        AccessCookieFilter accessCookieFilter = login(GUEST_SUB, institutionalAdminEntitlementOperator(ORGANISATION_GUID));
        assertTrue(user(GUEST_SUB).isInstitutionAdmin());
        assertFalse(user(GUEST_SUB).isInstitutionAdminByInvite());
        assertEquals(ORGANISATION_GUID, user(GUEST_SUB).getOrganizationGUID());
        assertEquals(200, institutionAdminsStatus(accessCookieFilter));

        //Still an institution admin when the entitlement is received again
        login(GUEST_SUB, institutionalAdminEntitlementOperator(ORGANISATION_GUID));
        assertTrue(user(GUEST_SUB).isInstitutionAdmin());

        accessCookieFilter = login(GUEST_SUB, m -> m);
        assertFalse(user(GUEST_SUB).isInstitutionAdmin());
        assertNull(user(GUEST_SUB).getOrganizationGUID());
        assertEquals(403, institutionAdminsStatus(accessCookieFilter));
    }

    @Test
    void downgradeInstitutionAdminWithOtherEntitlements() throws Exception {
        login(GUEST_SUB, institutionalAdminEntitlementOperator(ORGANISATION_GUID));

        //The organizationGUID on its own does not make an institution admin
        login(GUEST_SUB, m -> {
            m.put("eduperson_entitlement", List.of("urn:mace:surfnet.nl:surfnet.nl:sab:organizationGUID:" + ORGANISATION_GUID));
            return m;
        });
        assertFalse(user(GUEST_SUB).isInstitutionAdmin());
        assertNull(user(GUEST_SUB).getOrganizationGUID());
    }

    @Test
    void upgradeAfterDowngrade() throws Exception {
        login(GUEST_SUB, institutionalAdminEntitlementOperator(ORGANISATION_GUID));
        login(GUEST_SUB, m -> m);
        assertFalse(user(GUEST_SUB).isInstitutionAdmin());

        login(GUEST_SUB, institutionalAdminEntitlementOperator(ORGANISATION_GUID));
        assertTrue(user(GUEST_SUB).isInstitutionAdmin());
        assertEquals(ORGANISATION_GUID, user(GUEST_SUB).getOrganizationGUID());
    }

    @Test
    void noDowngradeInstitutionAdminByInvite() throws Exception {
        assertTrue(user(INSTITUTION_ADMIN_SUB).isInstitutionAdminByInvite());
        //The second login must not be affected by the first
        for (int i = 0; i < 2; i++) {
            AccessCookieFilter accessCookieFilter = login(INSTITUTION_ADMIN_SUB, m -> m);
            assertTrue(user(INSTITUTION_ADMIN_SUB).isInstitutionAdmin());
            assertTrue(user(INSTITUTION_ADMIN_SUB).isInstitutionAdminByInvite());
            assertEquals(ORGANISATION_GUID, user(INSTITUTION_ADMIN_SUB).getOrganizationGUID());
            assertEquals(200, institutionAdminsStatus(accessCookieFilter));
        }
    }

    @Test
    void noDowngradeInstitutionAdminByInviteWithEntitlement() throws Exception {
        login(INSTITUTION_ADMIN_SUB, institutionalAdminEntitlementOperator(ORGANISATION_GUID));
        login(INSTITUTION_ADMIN_SUB, m -> m);

        assertTrue(user(INSTITUTION_ADMIN_SUB).isInstitutionAdmin());
        assertEquals(ORGANISATION_GUID, user(INSTITUTION_ADMIN_SUB).getOrganizationGUID());
    }

}
