package access.api;

import access.config.HashGenerator;
import access.exception.NotFoundException;
import access.exception.UserRestrictionException;
import access.model.APIToken;
import access.model.User;
import access.repository.APITokenRepository;
import access.security.UserPermissions;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static access.SwaggerOpenIdConfig.API_TOKENS_SCHEME_NAME;
import static access.SwaggerOpenIdConfig.OPEN_ID_SCHEME_NAME;

@RestController
@RequestMapping(value = {"/api/v1/tokens", "/api/external/v1/tokens"}, produces = MediaType.APPLICATION_JSON_VALUE)
@Transactional
@SecurityRequirement(name = OPEN_ID_SCHEME_NAME, scopes = {"openid"})
@SecurityRequirement(name = API_TOKENS_SCHEME_NAME)
public class APITokenController {

    private static final Log LOG = LogFactory.getLog(APITokenController.class);
    private static final String TOKEN_KEY = "token_key";

    private final APITokenRepository apiTokenRepository;

    public APITokenController(APITokenRepository apiTokenRepository) {
        this.apiTokenRepository = apiTokenRepository;
    }

    @GetMapping("")
    public ResponseEntity<List<APIToken>> apiTokensByInstitution(@Parameter(hidden = true) User user) {
        LOG.debug("/tokens");
        UserPermissions.assertInstitutionAdmin(user);
        List<APIToken> apiTokens = user.isSuperUser() ? apiTokenRepository.findAll() : apiTokenRepository.findByOrganizationGUID(user.getOrganizationGUID());
        return ResponseEntity.ok(apiTokens);
    }

    @GetMapping("generate-token")
    public ResponseEntity<Map<String, String>> generateToken(@Parameter(hidden = true) User user,
                                                             @Parameter(hidden = true) HttpServletRequest request) {
        LOG.debug("/generateToken");
        UserPermissions.assertInstitutionAdmin(user);
        String token = HashGenerator.generateToken();
        request.getSession().setAttribute(TOKEN_KEY, token);
        return ResponseEntity.ok(Map.of("token", token));
    }

    @PostMapping("")
    public ResponseEntity<APIToken> create(@Validated @RequestBody APIToken apiTokenRequest,
                                           @Parameter(hidden = true) User user,
                                           @Parameter(hidden = true) HttpServletRequest request) {
        LOG.debug("/create");
        UserPermissions.assertInstitutionAdmin(user);
        String token = (String) request.getSession().getAttribute(TOKEN_KEY);
        if (!StringUtils.hasText(token)) {
            throw new UserRestrictionException();
        }
        APIToken apiToken = new APIToken(
                user.getOrganizationGUID(),
                HashGenerator.hashToken(token),
                user.isSuperUser(),
                apiTokenRequest.getDescription());
        return ResponseEntity.ok(apiTokenRepository.save(apiToken));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteToken(@PathVariable("id") Long id, @Parameter(hidden = true) User user) {
        LOG.debug("/deleteToken");
        UserPermissions.assertInstitutionAdmin(user);
        APIToken apiToken = apiTokenRepository.findById(id).orElseThrow(() -> new NotFoundException("API token not found"));
        if (apiToken.isSuperUserToken() && !user.isSuperUser()) {
            throw new UserRestrictionException();
        }
        if (!user.isSuperUser() && !apiToken.getOrganizationGUID().equals(user.getOrganizationGUID())) {
            throw new UserRestrictionException();
        }
        apiTokenRepository.delete(apiToken);
        return Results.deleteResult();
    }
}
