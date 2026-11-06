package invite.mail;

import com.fasterxml.jackson.databind.ObjectMapper;
import invite.WithApplicationTest;
import invite.cron.IdPMetaDataResolver;
import invite.cron.IdentityProvider;
import invite.manage.EntityType;
import invite.manage.Manage;
import invite.model.Application;
import invite.model.InvitationApplication;
import invite.model.Authority;
import invite.model.GroupedProviders;
import invite.model.Invitation;
import invite.model.InvitationRole;
import invite.model.Language;
import invite.model.Role;
import invite.model.User;
import invite.model.UserRole;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Renders every mail template in both languages and checks the properties that matter for email clients.
 * The rendered mails are written to target/mail-preview for visual inspection.
 */
class MailBoxTemplatesTest extends WithApplicationTest {

    @AfterEach
    void resetLocale() {
        //The locale is thread bound: don't leak 'nl' into other tests
        LocaleContextHolder.resetLocaleContext();
    }

    private final BlockingQueue<MimeMessage> sent = new LinkedBlockingQueue<>();

    private MailBox mailBox(String logoUrl) throws Exception {
        JavaMailSenderImpl sender = new JavaMailSenderImpl() {
            @Override
            public void send(MimeMessage... mimeMessages) {
                sent.addAll(List.of(mimeMessages));
            }
        };
        sender.setSession(Session.getInstance(new java.util.Properties()));
        IdPMetaDataResolver resolver = Mockito.mock(IdPMetaDataResolver.class);
        when(resolver.getIdentityProvider(anyString()))
                .thenReturn(Optional.of(new IdentityProvider("Uni EN", "Uni NL", logoUrl)));
        Manage manage = Mockito.mock(Manage.class);
        when(manage.providerById(any(), anyString()))
                .thenReturn(Map.of("name:en", "Calendar EN", "name:nl", "Agenda NL"));
        return new MailBox(new ObjectMapper(), resolver, sender, "no-reply@surf.nl", "contact@surf.nl",
                "support@surf.nl", "http://localhost:3000", "http://localhost:3001", "test",
                "SURFconext Invite", manage);
    }

    private MimeMessageParser next(String name) throws Exception {
        MimeMessage message = sent.poll(5, TimeUnit.SECONDS);
        assertNotNull(message, name);
        MimeMessageParser parser = new MimeMessageParser(message).parse();
        String html = parser.getHtmlContent();
        assertFalse(html.contains("{{") || html.contains("}}"), name + " has unresolved tags");
        assertFalse(html.contains("display:flex") || html.contains("display: flex"), name + " uses flex");
        assertFalse(html.contains("<svg"), name + " has inline svg");
        assertEquals(1, html.split("<h1", -1).length - 1, name + " must have exactly one h1");
        assertTrue(html.contains("cid:surfLogo"), name);
        assertTrue(parser.getContentIds().contains("surfLogo"), name + " logo must be attached inline");
        assertFalse(parser.getPlainContent().isBlank(), name);
        assertFalse(parser.getPlainContent().contains("{{"), name);
        Files.createDirectories(Path.of("target/mail-preview"));
        Files.writeString(Path.of("target/mail-preview/" + name + ".html"),
                html.replace("cid:surfLogo", "../classes/templates/surf_logo.png"));
        return parser;
    }

    private Invitation invitation(Language language, boolean eduIDOnly, String message, Authority authority) {
        User user = new User(false, "eppn", "sub", "example.com", "John", "Doe", "jdoe@example.com");
        return new Invitation(authority, "hash", "invitee@example.com", false, eduIDOnly,
                "https://eduid.nl/trust/linked-institution", false, message, language, user,
                Instant.now().plus(30, ChronoUnit.DAYS), Instant.now().plus(365, ChronoUnit.DAYS),
                Set.of(new InvitationRole(new Role("Wiki", "desc", application("1", EntityType.SAML20_SP), 365, false, false))),
                Set.of(), null);
    }

    private Invitation applicationInvitation(Language language, boolean eduIDOnly) {
        User user = new User(false, "eppn", "sub", "example.com", "John", "Doe", "jdoe@example.com");
        return new Invitation(Authority.APPLICATION_MANAGER, "hash", "invitee@example.com", false, eduIDOnly,
                "https://eduid.nl/trust/linked-institution", false, null, language, user,
                Instant.now().plus(30, ChronoUnit.DAYS), null,
                Set.of(), Set.of(new InvitationApplication(new Application("1", EntityType.SAML20_SP))), null);
    }

    private List<GroupedProviders> groupedProviders(Invitation invitation) {
        return List.of(new GroupedProviders(
                Map.of("name:en", "Wiki EN", "name:nl", "Wiki NL", "OrganizationName:en", "Org", "OrganizationName:nl", "Org"),
                invitation.getRoles().stream().map(InvitationRole::getRole).toList(), "logo"));
    }

    @Test
    void allTemplates() throws Exception {
        MailBox mailBox = mailBox("https://static.surfconext.nl/media/idp/leiden.png");
        User inviter = new User(false, "eppn", "sub", "example.com", "John", "Doe", "jdoe@example.com");
        User invitee = new User(false, "eppn2", "sub2", "eduid.nl", "Sam", "de Boer", "sam@example.com");

        for (Language language : Language.values()) {
            LocaleContextHolder.setLocale(Locale.of(language.name()));
            String name = language.name();

            for (boolean eduIDOnly : new boolean[]{false, true}) {
                Invitation invitation = invitation(language, eduIDOnly, "Line one\nLine two <b>x</b>", Authority.INVITER);
                mailBox.sendInviteMail(inviter, invitation, groupedProviders(invitation), language, Optional.empty());
                MimeMessageParser mail = next("invitation_" + name + "_edu_" + eduIDOnly);
                String html = mail.getHtmlContent();
                assertTrue(html.contains("Line one<br/>Line two &lt;b&gt;x&lt;/b&gt;"), "message must be escaped with <br/>");
                assertTrue(mail.getPlainContent().contains("Line two <b>x</b>"), "plain text contains the raw message");
                assertTrue(html.contains("leiden.png"), "remote institution logo");
                assertTrue(html.contains("http://localhost:3000/invitation/accept?hash&#61;hash"), "escaped link in href");
                assertTrue(mail.getPlainContent().contains("http://localhost:3000/invitation/accept?hash=hash"));
            }

            Invitation noMessage = invitation(language, false, null, Authority.GUEST);
            mailBox.sendInviteMail(inviter, noMessage, groupedProviders(noMessage), language, Optional.empty());
            assertFalse(next("invitation_nomessage_" + name).getHtmlContent().contains("Personal message"));

            Role role = new Role("Wiki", "desc", application("1", EntityType.SAML20_SP), 365, false, false);
            UserRole userRole = new UserRole("inviter", invitee, role, Authority.GUEST);
            mailBox.sendUserRoleExpirationNotificationMail(userRole, groupedProviders(noMessage).getFirst(), 10);
            String html = next("role_expiration_" + name).getHtmlContent();
            assertTrue(html.contains("Wiki"));
            assertTrue(html.contains("10"));

            mailBox.sendInvitationAcceptedMail(noMessage, inviter, invitee);
            assertTrue(next("invitation_accepted_" + name).getHtmlContent().contains("Sam de Boer"));
        }
        mailBox.sendFeedbackMail(inviter, "Hello\nworld", "https://invite.example.com/home", null, null, null);
        MimeMessageParser feedback = next("feedback_en");
        assertTrue(feedback.getHtmlContent().contains("Hello<br/>world"));
        assertFalse(feedback.getHtmlContent().contains("Do you have any questions"), "internal mail has no support sign-off");
    }

    @Test
    void roleAndApplicationInvitationsAreDistinct() throws Exception {
        MailBox mailBox = mailBox(null);
        User inviter = new User(false, "eppn", "sub", "example.com", "John", "Doe", "jdoe@example.com");
        for (Language language : Language.values()) {
            LocaleContextHolder.setLocale(Locale.of(language.name()));
            boolean en = language == Language.en;

            Invitation roles = invitation(language, false, null, Authority.MANAGER);
            mailBox.sendInviteMail(inviter, roles, groupedProviders(roles), language, Optional.empty());
            String html = next("distinct_roles_" + language.name()).getHtmlContent();
            assertTrue(html.contains(en ? "You have been invited for the role Wiki" : "Je bent uitgenodigd voor de rol Wiki"));
            assertTrue(html.contains(en ? "As role manager" : "Als rolmanager"));
            assertTrue(html.contains(en ? "SURFconext." : "SURFconext."), "SURFconext login hint");
            assertFalse(html.contains("Role creator"));

            Invitation applications = applicationInvitation(language, true);
            mailBox.sendInviteMail(inviter, applications, List.of(), language, Optional.empty());
            MimeMessageParser mail = next("distinct_applications_" + language.name());
            html = mail.getHtmlContent();
            assertTrue(html.contains(en ? "You have been invited as Role creator" : "Je bent uitgenodigd als Role creator"));
            assertTrue(html.contains(en ? "Calendar EN" : "Agenda NL"), "application name");
            assertTrue(html.contains("eduID"), "eduID login");
            assertFalse(html.contains("As role manager"));
            assertFalse(mail.getPlainContent().contains("{{"));
        }
    }

    @Test
    void specialCharactersAndInjection() throws Exception {
        MailBox mailBox = mailBox("https://x.nl/a.png\" onerror=\"alert(1)");
        User inviter = new User(false, "eppn", "sub", "example.com", "Brown & O'Sons", "", "jdoe@example.com");

        LocaleContextHolder.setLocale(Locale.of("nl"));
        Invitation invitation = invitation(Language.nl, false, "<script>alert(1)</script>", Authority.INVITER);
        mailBox.sendInviteMail(inviter, invitation, groupedProviders(invitation), Language.nl, Optional.empty());
        MimeMessageParser mail = next("invitation_special_nl");
        assertTrue(mail.getPlainContent().contains("Brown & O'Sons"), "plain text must not be HTML-escaped");
        assertFalse(mail.getPlainContent().contains("&amp;"));
        assertTrue(mail.getHtmlContent().contains("Brown &amp; O&#39;Sons"), "html must be escaped");
        assertFalse(mail.getHtmlContent().contains("<script>"), "message must be escaped");
        assertFalse(mail.getHtmlContent().contains("onerror"), "unsafe logo url must be skipped");
        assertTrue(mail.getHtmlContent().contains("supportteam"), "signoff must be localized");

        mailBox.sendFeedbackMail(inviter, "m", "javascript:alert(1)", null, null, null);
        assertFalse(next("feedback_js").getHtmlContent().contains("javascript:"), "non http(s) url must be dropped");
    }
}
