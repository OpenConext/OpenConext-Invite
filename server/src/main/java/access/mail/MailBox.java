package access.mail;

import access.cron.IdPMetaDataResolver;
import access.model.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.MustacheFactory;
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
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@SuppressWarnings("unchecked")
public class MailBox {

    private final JavaMailSender mailSender;
    private final String clientUrl;
    private final String welcomeUrl;
    private final String emailFrom;
    private final String contactEmail;
    private final String environment;

    private final Map<String, Map<String, String>> subjects;

    private final MustacheFactory mustacheFactory = new DefaultMustacheFactory("templates");
    private final IdPMetaDataResolver idPMetaDataResolver;

    public MailBox(ObjectMapper objectMapper,
                   IdPMetaDataResolver idPMetaDataResolver,
                   JavaMailSender mailSender,
                   String emailFrom,
                   String contactEmail,
                   String clientUrl,
                   String welcomeUrl,
                   String environment) throws IOException {
        this.mailSender = mailSender;
        this.idPMetaDataResolver = idPMetaDataResolver;
        this.emailFrom = emailFrom;
        this.contactEmail = contactEmail;
        this.clientUrl = clientUrl;
        this.welcomeUrl = welcomeUrl;
        this.environment = environment;
        this.subjects = objectMapper.readValue(new ClassPathResource("/templates/subjects.json").getInputStream(), new TypeReference<>() {
        });
    }

    @SneakyThrows
    public void sendInviteMail(User user, Invitation invitation, List<GroupedProviders> groupedProviders) {
        Authority intendedAuthority = invitation.getIntendedAuthority();
        String lang = preferredLanguage().toLowerCase();
        String title = String.format(subjects.get(lang).get("newInvitation"),
                invitation.getRoles().stream().map(role -> role.getRole().getName()).collect(Collectors.joining(", ")));
        Map<String, Object> variables = new HashMap<>();
        variables.put("groupedProviders", groupedProviders);
        variables.put("title", title);
        variables.put("institutionName", idPMetaDataResolver
                .getIdentityProvider(user.getSchacHomeOrganization())
                .map(idp -> idp.getName())
                .orElse(user.getSchacHomeOrganization()));
        variables.put("roles", splitListSemantically(invitation.getRoles().stream().map(invitationRole -> invitationRole.getRole().getName()).toList()));
        if (StringUtils.hasText(invitation.getMessage())) {
            variables.put("message", invitation.getMessage().replaceAll("\n", "<br/>"));
        }

            variables.put("invitation", invitation);
        variables.put("intendedAuthority", invitation.getIntendedAuthority().translate(lang));
        variables.put("user", user);
        if (!environment.equalsIgnoreCase("prod")) {
            variables.put("environment", environment);
        }
        String url = intendedAuthority.equals(Authority.GUEST) ? welcomeUrl : clientUrl;

        variables.put("url", String.format("%s/invitation/accept?hash=%s", url, invitation.getHash()));
        variables.put("useEduID", invitation.isEduIDOnly() && invitation.getIntendedAuthority().equals(Authority.GUEST));

        sendMail(String.format("invitation_%s", lang),
                title,
                variables,
                invitation.getEmail());
    }

    @SneakyThrows
    public void sendUserRoleExpirationNotificationMail(UserRole userRole,
                                                       GroupedProviders groupedProvider,
                                                       int nbrOfDays) {
        String lang = preferredLanguage().toLowerCase();
        String title = String.format(subjects.get(lang).get("roleExpirationNotification"),
                userRole.getAuthority().translate(lang),
                userRole.getRole().getName());
        Map<String, Object> variables = new HashMap<>();
        variables.put("title", title);
        variables.put("userRole", userRole);
        variables.put("groupedProvider", groupedProvider);
        variables.put("groupedProviders", List.of(groupedProvider));
        variables.put("nbrOfDays", nbrOfDays);
        variables.put("contactEmail", contactEmail);
        variables.put("authority", userRole.getAuthority().translate(lang));
        if (!environment.equalsIgnoreCase("prod")) {
            variables.put("environment", environment);
        }
        sendMail(String.format("role_expiration_%s", lang),
                title,
                variables,
                userRole.getUser().getEmail());
    }

    private String preferredLanguage() {
        return LocaleContextHolder.getLocale().getLanguage();
    }

    private void sendMail(String templateName, String subject, Map<String, Object> variables, String... to) throws MessagingException, IOException {
        String htmlText = this.mailTemplate(templateName + ".html", variables);
        String plainText = this.mailTemplate(templateName + ".txt", variables);

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setSubject(subject);
        setText(plainText, htmlText, helper);
        helper.setTo(to);
        helper.setFrom(emailFrom);
//        //Add logo, if there
//        if (variables.containsKey("groupedProviders")) {
//            List<GroupedProviders> groupedProviders = (List<GroupedProviders>) variables.get("groupedProviders");
//            groupedProviders.stream()
//                    .filter(groupedProvider -> StringUtils.hasText(groupedProvider.getLogo()))
//                    .forEach(groupedProvider -> {
//                        try {
//                            helper.addInline(groupedProvider.logoName(), new UrlResource(new URI(groupedProvider.getLogo())));
//                        } catch (Exception e) {
//                            //Can't be helped
//                        }
//                    });
//        }
        doSendMail(message);
    }

    protected void setText(String plainText, String htmlText, MimeMessageHelper helper) throws MessagingException, IOException {
        helper.setText(plainText, htmlText);
    }

    protected void doSendMail(MimeMessage message) {
        new Thread(() -> mailSender.send(message)).start();
    }

    private String mailTemplate(String templateName, Map<String, Object> context) {
        return mustacheFactory.compile(templateName).execute(new StringWriter(), context).toString();
    }

    private String splitListSemantically(List<String> values) {
        if (CollectionUtils.isEmpty(values)) {
            return "";
        }
        if (values.size() == 1) {
            return values.get(0);
        }
        String separator = preferredLanguage().toLowerCase().equals("en") ? " and " : " en ";
        return values.subList(0, values.size() - 1).stream()
                .collect(Collectors.joining(", ")) + separator +
                values.get(values.size() - 1);

    }

}
