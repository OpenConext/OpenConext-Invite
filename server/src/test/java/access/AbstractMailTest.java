package access;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.List;

import static org.awaitility.Awaitility.await;


@ActiveProfiles(value = "prod", inheritProfiles = false)
public class AbstractMailTest extends AbstractTest {

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP);

    @BeforeEach
    protected void beforeEach() throws Exception {
        super.beforeEach();
        greenMail.start();
        greenMail.purgeEmailFromAllMailboxes();
    }

    @AfterEach
    void afterEach() {
        greenMail.stop();
    }

    protected MimeMessage mailMessage() throws Exception {
        await().until(() -> greenMail.getReceivedMessages().length != 0);
        return greenMail.getReceivedMessages()[0];
    }

    protected List<MimeMessage> allMailMessages(int expectedLength) throws Exception {
        await().until(() -> greenMail.getReceivedMessages().length == expectedLength);
        return Arrays.asList(greenMail.getReceivedMessages());
    }


}