package provisioning.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.ModelAndView;
import provisioning.model.ProvisioningType;
import provisioning.repository.ProvisioningRepository;

import java.util.Map;

@Controller
public class ProvisioningController {

    private final String environment;
    private final ProvisioningRepository provisioningRepository;

    public ProvisioningController(@Value("${environment}") String environment, ProvisioningRepository provisioningRepository) {
        this.environment = environment;
        this.provisioningRepository = provisioningRepository;
    }

    @GetMapping("/")
    public ModelAndView index() {
        return new ModelAndView("index",
                Map.of("environment", environment));
    }

    @GetMapping("/provisionings/{type}")
    public ModelAndView allProvisionings(@PathVariable("type") ProvisioningType provisioningType) {
        return new ModelAndView("provisionings",
                Map.of("provisionings", provisioningRepository.findProvisioningByProvisioningType(provisioningType),
                        "provisioningType", provisioningType.name(),
                        "environment", environment));
    }

    @GetMapping("/provisioning/{id}")
    public ModelAndView provisioning(@PathVariable("id") Long id) {
        return new ModelAndView("provisioning",
                Map.of("provisioning", provisioningRepository.findById(id).get(),
                        "environment", environment));
    }

    @GetMapping("/delete/provisionings/{type}")
    public ModelAndView deleteProvisionings(@PathVariable("type") ProvisioningType provisioningType) {
        provisioningRepository.deleteAll(provisioningRepository.findProvisioningByProvisioningType(provisioningType));
        return this.allProvisionings(provisioningType);
    }

    @GetMapping("/delete/provisioning/{type}/{id}")
    public ModelAndView deleteProvisioning(@PathVariable("type") ProvisioningType provisioningType, @PathVariable("id") Long id) {
        provisioningRepository.deleteById(id);
        return this.allProvisionings(provisioningType);
    }
}
