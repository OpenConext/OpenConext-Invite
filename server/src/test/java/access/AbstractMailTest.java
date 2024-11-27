package access;

import access.mail.MimeMessageParser;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetup;
import jakarta.mail.internet.MimeMessage;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.awaitility.Awaitility.await;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "oidcng.introspect-url=http://localhost:8081/introspect",
                "spring.security.oauth2.client.provider.oidcng.authorization-uri=http://localhost:8081/authorization",
                "spring.security.oauth2.client.provider.oidcng.token-uri=http://localhost:8081/token",
                "spring.security.oauth2.client.provider.oidcng.user-info-uri=http://localhost:8081/user-info",
                "spring.security.oauth2.client.provider.oidcng.jwk-set-uri=http://localhost:8081/jwk-set",
                "email.enabled=true",
                "manage.url: http://localhost:8081",
                "manage.enabled: true"
        })
public class AbstractMailTest extends AbstractTest {

    private static final ServerSetup serverSetup = new ServerSetup(1025, "localhost", ServerSetup.PROTOCOL_SMTP);

    static {
        serverSetup.setServerStartupTimeout(500000);
    }

    @RegisterExtension
    static final GreenMailExtension greenMail = new GreenMailExtension(serverSetup);

    @BeforeEach
    protected void beforeEach() throws Exception {
        super.beforeEach();
        greenMail.purgeEmailFromAllMailboxes();
    }

    @SneakyThrows
    protected MimeMessageParser mailMessage() {
        await().until(() -> greenMail.getReceivedMessages().length != 0);
        MimeMessage receivedMessage = greenMail.getReceivedMessages()[0];
        MimeMessageParser parser = new MimeMessageParser(receivedMessage);
        return parser.parse();
    }

    protected List<MimeMessageParser> allMailMessages(int expectedLength) throws Exception {
        await().until(() -> greenMail.getReceivedMessages().length == expectedLength);
        MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
        return Stream.of(receivedMessages)
                .map(this::mimeMessageParser)
                .collect(Collectors.toList());
    }

    @SneakyThrows
    protected MimeMessageParser mimeMessageParser(MimeMessage mimeMessage) {
        return new MimeMessageParser(mimeMessage).parse();
    }

}