package access.api;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
public class EnvironmentController {


    private final String disclaimerBackgroundColor;
    private final String disclaimerContent;

    public EnvironmentController(@Value("${gui.disclaimer.background-color}") String disclaimerBackgroundColor,
                                 @Value("${gui.disclaimer.content}") String disclaimerContent) {
        this.disclaimerBackgroundColor = disclaimerBackgroundColor;
        this.disclaimerContent = disclaimerContent;
    }

    @GetMapping("/api/v1/disclaimer")
    public void disclaimer(HttpServletResponse response) throws IOException {
        response.setContentType("text/css");
        response.getWriter().write("body::after {background: " + disclaimerBackgroundColor + ";content: \"" +
                disclaimerContent + "\";}");
        response.getWriter().flush();

    }

}
