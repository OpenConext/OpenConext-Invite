package access.validation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class URLFormatValidatorTest {

    private static final URLFormatValidator urlFormatValidator = new URLFormatValidator();

    @Test
    void isValid() {
        assertFalse(urlFormatValidator.isValid("javascript:alert(42)"));
    }
}