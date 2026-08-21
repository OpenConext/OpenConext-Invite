package invite.utils;

import invite.model.Language;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

public class InstantAnalyzer {

    public static String analyze(Stream<Instant> instants, String language) {
        Optional<Instant> minOpt = instants.min(Comparator.naturalOrder());
        if (minOpt.isEmpty()) {
            return null;
        }

        Instant min = minOpt.get();
        Instant now = Instant.now();

        long days = ChronoUnit.DAYS.between(now, min);

        boolean isEnglish = language.equals(Language.en.name());

        if (days < 30) {
            return days + (isEnglish ? " days" : " dagen");
        }

        // MONTHS/YEARS aren't supported directly on Instant (it's a pure
        // epoch-seconds point), so convert to ZonedDateTime first.
        ZonedDateTime minZdt = min.atZone(ZoneId.systemDefault());
        ZonedDateTime nowZdt = now.atZone(ZoneId.systemDefault());

        if (days < 365) {
            long months = ChronoUnit.MONTHS.between(nowZdt, minZdt);
            return months + (isEnglish ? " months" : " maanden");
        }

        long years = ChronoUnit.YEARS.between(nowZdt, minZdt);
        String yearsToString = years == 1L ? (isEnglish ? " year" : " jaar")
                : (isEnglish ? " years" : " jaren");
        return years +  yearsToString;
    }
}
