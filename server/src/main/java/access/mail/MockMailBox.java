package access.mail;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.util.FileCopyUtils;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.File;
import java.io.IOException;

public class MockMailBox extends MailBox {

    private final String environment;

    public MockMailBox(ObjectMapper objectMapper , JavaMailSender mailSender, String emailFrom, String baseUrl, String welcomeUrl, String environment) throws IOException {
        super(objectMapper, mailSender, emailFrom, baseUrl, welcomeUrl, environment);
        this.environment = environment;
    }

    @Override
    protected void doSendMail(MimeMessage message) {
        //nope
    }

    @Override
    protected void setText(String plainText, String htmlText, MimeMessageHelper helper) throws MessagingException {
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("mac os x") && environment.equals("local")) {
            openInBrowser(htmlText);
        }
    }

    @SneakyThrows
    private void openInBrowser(String html) {
        File tempFile = File.createTempFile("javamail", ".html");
        FileCopyUtils.copy(html.getBytes(), tempFile);
        new ProcessBuilder("open " + tempFile.getAbsolutePath()).start();
//        Runtime.getRuntime().exec();
    }
}
