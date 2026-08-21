package invite.utils;

import invite.model.Language;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InstantAnalyzerTest {

    @Test
    void emptyStreamReturnsNull() {
        assertNull(InstantAnalyzer.analyze(Stream.empty(), Language.en.name()));
    }

    @Test
    void daysEnglish() {
        //Small margin so a few ms of test execution time can't flip this to 4 complete days
        Instant in5Days = Instant.now().plus(5, ChronoUnit.DAYS).plus(1, ChronoUnit.HOURS);
        assertEquals("5 days", InstantAnalyzer.analyze(Stream.of(in5Days), Language.en.name()));
    }

    @Test
    void daysDutch() {
        Instant in5Days = Instant.now().plus(5, ChronoUnit.DAYS).plus(1, ChronoUnit.HOURS);
        assertEquals("5 dagen", InstantAnalyzer.analyze(Stream.of(in5Days), Language.nl.name()));
    }

    @Test
    void monthsEnglish() {
        Instant in100Days = Instant.now().plus(100, ChronoUnit.DAYS);
        String analyzed = InstantAnalyzer.analyze(Stream.of(in100Days), Language.en.name());
        assertTrue(analyzed.matches("\\d+ months"), analyzed);
    }

    @Test
    void monthsDutch() {
        Instant in100Days = Instant.now().plus(100, ChronoUnit.DAYS);
        String analyzed = InstantAnalyzer.analyze(Stream.of(in100Days), Language.nl.name());
        assertTrue(analyzed.matches("\\d+ maanden"), analyzed);
    }

    @Test
    void yearSingularEnglish() {
        Instant in400Days = Instant.now().plus(400, ChronoUnit.DAYS);
        assertEquals("1 year", InstantAnalyzer.analyze(Stream.of(in400Days), Language.en.name()));
    }

    @Test
    void yearSingularDutch() {
        Instant in400Days = Instant.now().plus(400, ChronoUnit.DAYS);
        assertEquals("1 jaar", InstantAnalyzer.analyze(Stream.of(in400Days), Language.nl.name()));
    }

    @Test
    void yearsPluralEnglish() {
        Instant in800Days = Instant.now().plus(800, ChronoUnit.DAYS);
        assertEquals("2 years", InstantAnalyzer.analyze(Stream.of(in800Days), Language.en.name()));
    }

    @Test
    void yearsPluralDutch() {
        Instant in800Days = Instant.now().plus(800, ChronoUnit.DAYS);
        assertEquals("2 jaren", InstantAnalyzer.analyze(Stream.of(in800Days), Language.nl.name()));
    }

    @Test
    void usesEarliestExpirationAcrossMultipleRoles() {
        Instant in5Days = Instant.now().plus(5, ChronoUnit.DAYS).plus(1, ChronoUnit.HOURS);
        Instant in100Days = Instant.now().plus(100, ChronoUnit.DAYS);
        assertEquals("5 days", InstantAnalyzer.analyze(Stream.of(in100Days, in5Days), Language.en.name()));
    }

}
