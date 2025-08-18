package invite.cron;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.support.CronExpression;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CronExpressionTest {

    @Test
    void validate() {
        assertFalse(CronExpression.isValidExpression("*/5 * * * *"));
        assertTrue(CronExpression.isValidExpression("0 */5 * ? * *"));
    }
}