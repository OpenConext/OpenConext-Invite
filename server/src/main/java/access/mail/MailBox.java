package access.mail;

import access.model.Authority;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.MustacheFactory;
import access.model.Invitation;
import access.model.User;
import lombok.SneakyThrows;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class MailBox {

    private final JavaMailSender mailSender;
    private final String baseUrl;
    private final String emailFrom;
    private final String environment;

    private static final Log LOG = LogFactory.getLog(MailBox.class);

    private final MustacheFactory mustacheFactory = new DefaultMustacheFactory("templates");

    public MailBox(JavaMailSender mailSender, String emailFrom, String baseUrl,  String environment) {
        this.mailSender = mailSender;
        this.emailFrom = emailFrom;
        this.baseUrl = baseUrl;
        this.environment = environment;
    }

    public void sendInviteMail(User user, Invitation invitation) {
        Authority intendedAuthority = invitation.getIntendedAuthority();
        String lang = preferredLanguage().toLowerCase(Locale.ROOT);
        String title = String.format("en".equals("en") ? "Invitation for %s at eduID inviters" :
                "Uitnodiging voor %s bij eduID uitnodigingen", invitation.getRoles());
//        Optional<String> logoOptional = metaDataResolver.getLogo(entityId);

        Map<String, Object> variables = new HashMap<>();
//        logoOptional.ifPresent(logo -> variables.put("logo", logo));
        variables.put("title", title);
//        variables.put("role", role);
        variables.put("invitation", invitation);
        variables.put("user", user);
        variables.put("url", String.format("%s/invitations?h=%s", baseUrl, invitation.getHash()));

        sendMail(String.format("invitation_%s", lang),
                title,
                variables,
                invitation.getEmail());
    }

    private String preferredLanguage() {
        return LocaleContextHolder.getLocale().getLanguage();
    }

    public void sendProvisioningMail(String title, String userRequest, String email) {
        LOG.info(String.format("Send email SCIM request %s %s to %s", title, userRequest, email));

        Map<String, Object> variables = new HashMap<>();
        variables.put("userRequest", userRequest);
        sendMail("scim_provisioning_en",
                title,
                variables,
                email);
    }

    @SneakyThrows
    private void sendMail(String templateName, String subject, Map<String, Object> variables, String... to) {
        String htmlText = this.mailTemplate(templateName + ".html", variables);
        String plainText = this.mailTemplate(templateName + ".txt", variables);

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setSubject(subject);
        setText(plainText, htmlText, helper);
        helper.setTo(to);
        helper.setFrom(emailFrom);
        doSendMail(message);
    }

    protected void setText(String plainText, String htmlText, MimeMessageHelper helper) throws MessagingException {
        helper.setText(plainText, htmlText);
    }

    protected void doSendMail(MimeMessage message) {
        new Thread(() -> mailSender.send(message)).start();
    }

    private String mailTemplate(String templateName, Map<String, Object> context) {
        return mustacheFactory.compile(templateName).execute(new StringWriter(), context).toString();
    }

}
