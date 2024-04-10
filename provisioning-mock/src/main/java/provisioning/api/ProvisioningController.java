package provisioning.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.ModelAndView;
import provisioning.model.Provisioning;
import provisioning.model.ProvisioningType;
import provisioning.repository.ProvisioningRepository;

import java.util.Map;

@Controller
public class ProvisioningController {

    private final String environment;
    private final ProvisioningRepository provisioningRepository;
    private final ObjectMapper objectMapper;

    public ProvisioningController(@Value("${environment}") String environment,
                                  ProvisioningRepository provisioningRepository,
                                  ObjectMapper objectMapper) {
        this.environment = environment;
        this.provisioningRepository = provisioningRepository;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/")
    public ModelAndView index() {
        return new ModelAndView("index",
                Map.of("environment", environment));
    }

    @GetMapping("/provisionings/{type}")
    public ModelAndView allProvisionings(@PathVariable("type") ProvisioningType provisioningType) {
        return new ModelAndView("provisionings",
                Map.of("provisionings", provisioningRepository.findByProvisioningTypeOrderByCreatedAtDesc(provisioningType),
                        "provisioningType", provisioningType.name(),
                        "environment", environment));
    }

    @GetMapping("/provisioning/{id}")
    public ModelAndView provisioning(@PathVariable("id") Long id) throws JsonProcessingException {
        Provisioning provisioning = provisioningRepository.findById(id).orElseThrow(IllegalArgumentException::new);
        String s = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(provisioning.getMessage())
                .replaceAll("\n", "<br/>");
        provisioning.setPrettyMessage(s);
        return new ModelAndView("provisioning",
                Map.of("provisioning", provisioning,
                        "environment", environment));
    }

    @GetMapping("/delete/provisionings/{type}")
    public ModelAndView deleteProvisionings(@PathVariable("type") ProvisioningType provisioningType) {
        provisioningRepository.deleteAll(provisioningRepository.findByProvisioningTypeOrderByCreatedAtDesc(provisioningType));
        return this.allProvisionings(provisioningType);
    }

    @GetMapping("/delete/provisioning/{type}/{id}")
    public ModelAndView deleteProvisioning(@PathVariable("type") ProvisioningType provisioningType, @PathVariable("id") Long id) {
        provisioningRepository.deleteById(id);
        return this.allProvisionings(provisioningType);
    }
}
