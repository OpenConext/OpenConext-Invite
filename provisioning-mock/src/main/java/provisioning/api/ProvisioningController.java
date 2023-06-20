package provisioning.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;

@Controller
public class ProvisioningController {

    private final String environment;

    public ProvisioningController(@Value("${environment}") String environment) {
        this.environment = environment;
    }

    @GetMapping("/")
    public ModelAndView index() {
        return new ModelAndView("index",
                Map.of("environment", environment));
    }

}
