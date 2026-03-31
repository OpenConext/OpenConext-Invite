package invite.api;

import invite.config.HashGenerator;
import invite.exception.NotFoundException;
import invite.exception.UserRestrictionException;
import invite.model.APIToken;
import invite.model.Authority;
import invite.model.User;
import invite.repository.APITokenRepository;
import invite.security.UserPermissions;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static invite.SwaggerOpenIdConfig.API_TOKENS_SCHEME_NAME;
import static invite.SwaggerOpenIdConfig.OPEN_ID_SCHEME_NAME;

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
    @Transactional(readOnly = true)
    public ResponseEntity<List<APIToken>> apiTokensByInstitution(@Parameter(hidden = true) User user) {
        LOG.debug(String.format("GET /tokens for user %s", user.getEduPersonPrincipalName()));
        UserPermissions.assertAuthority(user, Authority.INVITER);
        List<APIToken> apiTokens = user.isSuperUser() ? apiTokenRepository.findAll() :
                user.isInstitutionAdmin() ? apiTokenRepository.findByOrganizationGUIDAndSuperUserToken(user.getOrganizationGUID(), false) :
                        apiTokenRepository.findByOwner(user);
        return ResponseEntity.ok(apiTokens);
    }

    @GetMapping("generate-token")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, String>> generateToken(@Parameter(hidden = true) User user,
                                                             @Parameter(hidden = true) HttpServletRequest request) {
        LOG.debug(String.format("GET /tokens/generateToken for user %s", user.getEduPersonPrincipalName()));
        UserPermissions.assertAuthority(user, Authority.INVITER);
        String token = HashGenerator.generateToken();
        request.getSession().setAttribute(TOKEN_KEY, token);
        return ResponseEntity.ok(Map.of("token", token));
    }

    @PostMapping("")
    public ResponseEntity<APIToken> create(@Validated @RequestBody APIToken apiTokenRequest,
                                           @Parameter(hidden = true) User user,
                                           @Parameter(hidden = true) HttpServletRequest request) {
        LOG.debug(String.format("POST /tokens/create for user %s", user.getEduPersonPrincipalName()));
        UserPermissions.assertAuthority(user, Authority.INVITER);
        String token = (String) request.getSession().getAttribute(TOKEN_KEY);
        if (!StringUtils.hasText(token)) {
            throw new UserRestrictionException("Token is NULL");
        }
        if (user.isSuperUser() && !StringUtils.hasText(apiTokenRequest.getOrganizationGUID())) {
            throw new UserRestrictionException("Super user must specify API token OrganizationGUID");
        }
        APIToken apiToken;
        if (user.isSuperUser() || user.isInstitutionAdmin()) {
            apiToken = new APIToken(
                    user.isSuperUser() ? apiTokenRequest.getOrganizationGUID() : user.getOrganizationGUID(),
                    HashGenerator.hashToken(token),
                    user.isSuperUser(),
                    apiTokenRequest.getDescription(),
                    user);
        } else {
            apiToken = new APIToken(HashGenerator.hashToken(token), apiTokenRequest.getDescription(), user);
        }
        apiToken = apiTokenRepository.save(apiToken);
        return ResponseEntity.ok(apiToken);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteToken(@PathVariable("id") Long id, @Parameter(hidden = true) User user) {
        LOG.debug(String.format("DELETE /tokens/deleteToken with id %s for user %s", id.toString(), user.getEduPersonPrincipalName()));

        UserPermissions.assertAuthority(user, Authority.INVITER);
        APIToken apiToken = apiTokenRepository.findById(id).orElseThrow(() -> new NotFoundException("API token not found"));
        if (apiToken.isSuperUserToken() && !user.isSuperUser()) {
            throw new UserRestrictionException("Non super-user not allowed to delete super-user token: " + user.getEmail());
        }
        if (user.isInstitutionAdmin() && !apiToken.getOrganizationGUID().equals(user.getOrganizationGUID())) {
            throw new UserRestrictionException("User not allowed to delete token for different organization: " + user.getEmail());
        }
        if (!user.isSuperUser() && !user.isInstitutionAdmin() && !Objects.equals(user.getId(), apiToken.getOwner().getId())) {
            throw new UserRestrictionException("User not allowed to delete token for different owner: " + user.getEmail());
        }
        apiTokenRepository.delete(apiToken);
        return Results.deleteResult();
    }
}
