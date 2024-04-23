package access.cron;

import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.scheduling.support.CronExpression;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

class CronExpressionTest {

    @Test
    void validate() {
        assertFalse(CronExpression.isValidExpression("*/5 * * * *"));
        assertTrue(CronExpression.isValidExpression("0 */5 * ? * *"));
    }
}