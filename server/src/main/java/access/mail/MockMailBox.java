package access.mail;

import lombok.SneakyThrows;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.util.FileCopyUtils;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.File;

public class MockMailBox extends MailBox {

    private final String environment;

    public MockMailBox(JavaMailSender mailSender, String emailFrom, String baseUrl, String environment) {
        super(mailSender, emailFrom, baseUrl, environment);
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
