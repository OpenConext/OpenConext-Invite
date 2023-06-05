package access.mail;

import access.model.Authority;
import access.model.GroupedProviders;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.MustacheFactory;
import access.model.Invitation;
import access.model.User;
import lombok.SneakyThrows;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.UrlResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MailBox {

    private final JavaMailSender mailSender;
    private final String baseUrl;
    private final String welcomeUrl;
    private final String emailFrom;
    private final String environment;
    private final Map<String, Map<String, String>> subjects;

    private static final Log LOG = LogFactory.getLog(MailBox.class);

    private final MustacheFactory mustacheFactory = new DefaultMustacheFactory("templates");

    public MailBox(ObjectMapper objectMapper,
                   JavaMailSender mailSender,
                   String emailFrom,
                   String baseUrl,
                   String welcomeUrl,
                   String environment) throws IOException {
        this.mailSender = mailSender;
        this.emailFrom = emailFrom;
        this.baseUrl = baseUrl;
        this.welcomeUrl = welcomeUrl;
        this.environment = environment;
        this.subjects = objectMapper.readValue(new ClassPathResource("/templates/subjects.json").getInputStream(), new TypeReference<>() {
        });
    }

    public void sendInviteMail(User user, Invitation invitation, List<GroupedProviders> groupedProviders) {
        Authority intendedAuthority = invitation.getIntendedAuthority();
        String lang = preferredLanguage().toLowerCase();
        String title = String.format(subjects.get(lang).get("newInvitation"),
                invitation.getRoles().stream().map(role -> role.getRole().getName()).collect(Collectors.joining(", ")));
        Map<String, Object> variables = new HashMap<>();
        variables.put("groupedProviders", groupedProviders);
        variables.put("title", title);
        variables.put("invitation", invitation);
        variables.put("intendedAuthority", invitation.getIntendedAuthority().translate(lang));
        variables.put("user", user);
        String url = intendedAuthority.equals(Authority.GUEST) ? welcomeUrl : baseUrl;

        variables.put("url", String.format("%s/invitations?h=%s", url, invitation.getHash()));

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
        //Add logo, if there
        if (variables.containsKey("groupedProviders")) {
            List<GroupedProviders> groupedProviders = (List<GroupedProviders>) variables.get("groupedProviders");
            groupedProviders.stream()
                    .filter(groupedProvider -> StringUtils.hasText(groupedProvider.getLogo()))
                    .forEach(groupedProvider -> {
                        try {
                            helper.addInline(groupedProvider.logoName(), new UrlResource(new URI(groupedProvider.getLogo())));
                        } catch (Exception e) {
                            //Can't be helped
                        }
                    });
            helper.addInline("not_found", new ClassPathResource("templates/image-not-found.svg"));
        }
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
