package access.model;

import access.WithApplicationTest;
import access.manage.EntityType;
import access.manage.ManageIdentifier;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RoleTest extends WithApplicationTest {

    @Test
    void groupBy() {
        List<Role> roles = List.of(
                new Role("cloud", "cloud", application("1", EntityType.OIDC10_RP), 365, false, false),
                new Role("mail", "mail", application("1", EntityType.OIDC10_RP), 365, false, false),
                new Role("wiki", "wiki", application("2", EntityType.SAML20_SP), 365, false, false)
        );
        Set<ManageIdentifier> rolesByApplication = roles.stream()
                .map(Role::applicationsUsed)
                .flatMap(Collection::stream)
                .map(application -> new ManageIdentifier(application.getManageId(), application.getManageType()))
                .collect(Collectors.toSet());

        assertEquals(2, rolesByApplication.size());
    }
}