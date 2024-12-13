package access.api;

import access.exception.InvalidInputException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FullSearchQueryParserTest {

    @Test
    void parse() {
        String parsed = FullSearchQueryParser.parse("This *is+  +a ** test for + the john.+doe@example.com *query*");
        assertEquals("+test +john +doe +example +query*", parsed);

        String emailParsed = FullSearchQueryParser.parse("brand+ms@play.com");
        assertEquals("+brand +ms +play*", emailParsed);

        String strippedWhiteSpace = FullSearchQueryParser.parse(" Leitndhireedisvea@example.com  ");
        assertEquals("+Leitndhireedisvea +example*", strippedWhiteSpace);
    }

    @Test
    void parseEmpty() {
        assertThrows(InvalidInputException.class, () -> FullSearchQueryParser.parse(null));
        assertThrows(InvalidInputException.class, () -> FullSearchQueryParser.parse(""));
        assertThrows(InvalidInputException.class, () -> FullSearchQueryParser.parse("  "));
    }

}