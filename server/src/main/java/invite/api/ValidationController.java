package invite.api;

import invite.model.Validation;
import invite.validation.EmailFormatValidator;
import invite.validation.FormatValidator;
import invite.validation.URLFormatValidator;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;

@RestController
@RequestMapping(value = {"/api/v1/validations", "/api/external/v1/validations"}, produces = MediaType.APPLICATION_JSON_VALUE)
public class ValidationController {

    private final Map<String, FormatValidator> validators;

    public ValidationController() {
        this.validators = Stream.of(
                        new EmailFormatValidator(),
                        new URLFormatValidator())
                .collect(toMap(FormatValidator::formatName, Function.identity()));
    }

    @PostMapping("validate")
    public ResponseEntity<Map<String, Boolean>> validate(@Validated @RequestBody Validation validation) {
        boolean valid = this.validators.computeIfAbsent(validation.getType(), key -> {
            throw new IllegalArgumentException(String.format("No validation defined for %s", key));
        }).isValid(validation.getValue());
        return ResponseEntity.ok(Map.of("valid", valid));
    }


}
