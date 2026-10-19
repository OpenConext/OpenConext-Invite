package invite.api;

import invite.exception.InvalidInputException;
import invite.mail.MailBox;
import invite.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Base64;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/feedback")
public class FeedbackController {

    private static final long MAX_SCREENSHOT_BYTES = 5L * 1024L * 1024L;
    private static final String SCREENSHOT_CONTENT_TYPE = "image/png";

    private final MailBox mailBox;

    public FeedbackController(MailBox mailBox) {
        this.mailBox = mailBox;
    }

    @PostMapping
    @Operation(summary = "Send feedback", description = "Mail the feedback of the current user, with an optional screenshot, to the support address")
    public ResponseEntity<Map<String, Integer>> feedback(@Parameter(hidden = true) User user, @RequestBody Map<String, Object> body) throws IOException {
        String message = (String) body.get("message");
        String url = (String) body.get("url");
        boolean includeScreenshot = Boolean.parseBoolean(String.valueOf(body.getOrDefault("includeScreenshot", "false")));
        String screenshotBase64 = (String) body.get("screenshotBase64");
        String screenshotContentType = (String) body.getOrDefault("screenshotContentType", SCREENSHOT_CONTENT_TYPE);

        if (!StringUtils.hasText(message)) {
            throw new InvalidInputException("Feedback message is required");
        }

        byte[] screenshotBytes = null;
        String screenshotName = null;

        if (includeScreenshot && StringUtils.hasText(screenshotBase64)) {
            if (!SCREENSHOT_CONTENT_TYPE.equalsIgnoreCase(screenshotContentType)) {
                throw new InvalidInputException("Screenshot must be a PNG image");
            }
            try {
                screenshotBytes = Base64.getDecoder().decode(screenshotBase64);
            } catch (IllegalArgumentException exception) {
                throw new InvalidInputException("Screenshot is not valid base64");
            }
            if (screenshotBytes.length > MAX_SCREENSHOT_BYTES) {
                throw new InvalidInputException("Screenshot exceeds max size");
            }
            screenshotName = "feedback.png";
        }

        mailBox.sendFeedbackMail(user, message, url, screenshotBytes, screenshotName, screenshotContentType);
        return Results.okResult();
    }
}
