package provisioning.api;

import provisioning.model.ProvisioningType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.ModelAndView;
import provisioning.model.Provisioning;
import provisioning.repository.ProvisioningRepository;

import java.util.List;
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

    @DeleteMapping("/provisionings")
    public ResponseEntity<Void> deleteProvisionings() {
        provisioningRepository.deleteAllInBatch();
        return ResponseEntity.status(204).build();
    }
}
