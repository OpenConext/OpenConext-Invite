package invite.api;

import invite.exception.InvalidInputException;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FullSearchQueryParser {

    //SELECT * FROM INFORMATION_SCHEMA.INNODB_FT_DEFAULT_STOPWORD;
    private static final List<String> stopWords = List.of(
            "a", "about", "an", "are",
            "as", "at", "be", "by",
            "com", "de", "en", "for",
            "from", "how", "i", "in",
            "is", "it", "la", "of",
            "on", "or", "that", "the",
            "this", "to", "was", "what",
            "when", "where", "who", "will",
            "with", "und", "the", "www"
    );

    private FullSearchQueryParser() {
    }

    public static String parse(String query) {
        if (!StringUtils.hasText(query)) {
            throw new InvalidInputException("Full text query parameter has @NotNull @NotBlank requirement");
        }
        String parsedQuery = Stream.of(query.split("[ @.,+*-]"))
                //MariaDB does not tokenize words shorter than 3
                .filter(part -> !(part.isEmpty() || part.length() < 3 || stopWords.contains(part.toLowerCase()) ))
                .map(part -> "+" + part)
                .collect(Collectors.joining(" "));
        return parsedQuery + "*";
    }
}
