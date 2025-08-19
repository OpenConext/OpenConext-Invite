package invite.validation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmailFormatValidatorTest {

    private final EmailFormatValidator emailFormatValidator = new EmailFormatValidator();

    @Test
    void isValid() {
        assertTrue(emailFormatValidator.isValid("o@o"));
        assertTrue(emailFormatValidator.isValid("Frits Voorbeeld <frits.fritsmans@voorbeeld.nl>"));
        assertFalse(emailFormatValidator.isValid("o@@o"));
    }
}