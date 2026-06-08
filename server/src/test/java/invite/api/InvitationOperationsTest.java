package invite.api;

import invite.model.Role;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InvitationOperationsTest {

    private Role roleWithExpiryDays(Integer days) {
        Role role = new Role();
        role.setDefaultExpiryDays(days);
        return role;
    }

    private Role roleWithExpiryDate(Instant date) {
        Role role = new Role();
        role.setDefaultExpiryDate(date);
        return role;
    }

    @Test
    void longestByDays() {
        Instant before = Instant.now();
        List<Role> roles = List.of(
                roleWithExpiryDays(5),
                roleWithExpiryDays(90),
                roleWithExpiryDays(30)
        );

        Instant result = InvitationOperations.calculateInvitationExpiry(roles);

        assertExpiryInDays(before, result, 90);
    }

    @Test
    void longestByDate() {
        Instant soon = Instant.now().plus(3, ChronoUnit.DAYS);
        Instant later = Instant.now().plus(60, ChronoUnit.DAYS);
        List<Role> roles = List.of(
                roleWithExpiryDate(soon),
                roleWithExpiryDate(later)
        );

        Instant result = InvitationOperations.calculateInvitationExpiry(roles);

        assertEquals(later, result);
    }

    @Test
    void dateWinsWhenLaterThanDays() {
        Instant dateExpiry = Instant.now().plus(60, ChronoUnit.DAYS);
        List<Role> roles = List.of(
                roleWithExpiryDays(7),
                roleWithExpiryDate(dateExpiry)
        );

        Instant result = InvitationOperations.calculateInvitationExpiry(roles);

        // The dateExpiry (60 days from now) is later than 7 days
        assertEquals(dateExpiry, result);
    }

    @Test
    void daysWinsWhenLaterThanDate() {
        Instant before = Instant.now();
        Instant dateExpiry = Instant.now().plus(3, ChronoUnit.DAYS);
        List<Role> roles = List.of(
                roleWithExpiryDays(60),
                roleWithExpiryDate(dateExpiry)
        );

        Instant result = InvitationOperations.calculateInvitationExpiry(roles);

        // The dateExpiry (3 days from now) is later than 60 days
        assertExpiryInDays(before, result, 60);
    }

    @Test
    void rolesWithoutDaysFallBackToZeroSoDateWins() {
        Instant dateExpiry = Instant.now().plus(45, ChronoUnit.DAYS);
        List<Role> roles = List.of(
                new Role(),
                roleWithExpiryDate(dateExpiry),
                new Role()
        );

        Instant result = InvitationOperations.calculateInvitationExpiry(roles);

        assertEquals(dateExpiry, result);
    }

    @Test
    void noConstraintsFallsBackToNow() {
        Instant before = Instant.now();
        List<Role> roles = List.of(
                new Role(),
                new Role()
        );

        Instant result = InvitationOperations.calculateInvitationExpiry(roles);

        Instant after = Instant.now();
        assertTrue(!result.isBefore(before) && !result.isAfter(after),
                "Expected expiry to fall back to ~now");
    }

    @Test
    void emptyRolesFallsBackToNow() {
        Instant before = Instant.now();
        List<Role> roles = List.of();

        Instant result = InvitationOperations.calculateInvitationExpiry(roles);

        Instant after = Instant.now();
        // Preventing flaky tests where result could be equals to before or after
        assertTrue(!result.isBefore(before) && !result.isAfter(after),
                "Expected expiry to fall back to ~now");
    }

    private void assertExpiryInDays(Instant before, Instant result, int expectedDays) {
        Instant lowerBound = before.plus(expectedDays, ChronoUnit.DAYS);
        Instant upperBound = Instant.now().plus(expectedDays, ChronoUnit.DAYS);
        assertTrue(!result.isBefore(lowerBound) && !result.isAfter(upperBound),
                String.format("Expected expiry ~%d days from now but was %s", expectedDays, result));
    }
}
