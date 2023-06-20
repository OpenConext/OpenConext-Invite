package access.api;

import access.model.Validation;
import access.validation.EmailFormatValidator;
import access.validation.FormatValidator;
import access.validation.URLFormatValidator;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import static access.SwaggerOpenIdConfig.OPEN_ID_SCHEME_NAME;
import static java.util.stream.Collectors.toMap;

@RestController
@RequestMapping(value = {"/api/v1/validations", "/api/external/v1/validations"}, produces = MediaType.APPLICATION_JSON_VALUE)
@SecurityRequirement(name = OPEN_ID_SCHEME_NAME, scopes = {"openid"})
public class ValidationController {

    private final Map<String, FormatValidator> validators;

    public ValidationController() {
        this.validators = Stream.of(
                        new EmailFormatValidator(),
                        new URLFormatValidator())
                .collect(toMap(FormatValidator::formatName, Function.identity()));
    }

    @PostMapping("validate")
    public ResponseEntity<Map<String, Boolean>> validate(@RequestBody Validation validation) {
        boolean valid = this.validators.computeIfAbsent(validation.getType(), key -> {
            throw new IllegalArgumentException(String.format("No validation defined for %s", key));
        }).isValid(validation.getValue());
        return ResponseEntity.ok(Collections.singletonMap("valid", valid));
    }


}