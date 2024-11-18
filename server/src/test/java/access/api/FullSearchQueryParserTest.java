package access.api;

import access.exception.InvalidInputException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FullSearchQueryParserTest {

    @Test
    void parse() {
        String parsed = FullSearchQueryParser.parse("This *is+  +a ** test for + the john.+doe@example.com *query*");
        assertEquals("+test +john +doe +example +query*", parsed);
    }

    @Test
    void parseEmpty() {
        assertThrows(InvalidInputException.class, () -> FullSearchQueryParser.parse(null));
        assertThrows(InvalidInputException.class, () -> FullSearchQueryParser.parse(""));
        assertThrows(InvalidInputException.class, () -> FullSearchQueryParser.parse("  "));
    }

}