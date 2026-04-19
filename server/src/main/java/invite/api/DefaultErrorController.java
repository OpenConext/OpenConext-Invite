package invite.api;


import invite.exception.NotFoundException;
import invite.exception.UserRestrictionException;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Base64;
import java.util.Map;

import static org.springframework.http.HttpStatus.*;

@RestController
@Hidden
public class DefaultErrorController implements ErrorController {

    private static final Log LOG = LogFactory.getLog(DefaultErrorController.class);

    private final ErrorAttributes errorAttributes;

    @Autowired
    public DefaultErrorController(ErrorAttributes errorAttributes) {
        this.errorAttributes = errorAttributes;
    }

    @RequestMapping("/error")
    public ResponseEntity<Map<String, Object>> error(HttpServletRequest request) {
        WebRequest webRequest = new ServletWebRequest(request);
        Map<String, Object> result = this.errorAttributes.getErrorAttributes(
                webRequest,
                ErrorAttributeOptions.of(
                        ErrorAttributeOptions.Include.EXCEPTION,
                        ErrorAttributeOptions.Include.MESSAGE,
                        ErrorAttributeOptions.Include.PATH,
                        ErrorAttributeOptions.Include.ERROR,
                        ErrorAttributeOptions.Include.BINDING_ERRORS)
        );

        Throwable error = this.errorAttributes.getError(webRequest);
        HttpStatus statusCode;

        if (error == null) {
            if ("Unauthorized".equalsIgnoreCase((String) result.get("error"))) {
                statusCode = HttpStatus.UNAUTHORIZED;
                this.handleAuthorizationException(request, result);
            } else {
                statusCode = result.containsKey("status") && (int) result.get("status") != 999 ?
                        HttpStatus.valueOf((int) result.get("status")) : INTERNAL_SERVER_ERROR;

            }
        } else {
            if (!(error instanceof NotFoundException || error instanceof NoResourceFoundException)) {
                boolean logStackTrace = !(error instanceof UserRestrictionException || error instanceof invite.exception.RemoteException);
                LOG.error(String.format("Error occurred; %s", error), logStackTrace ? error : null);
            }
            if (error instanceof AccessDeniedException) {
                statusCode = FORBIDDEN;
            } else {
                //https://github.com/spring-projects/spring-boot/issues/3057
                ResponseStatus annotation = AnnotationUtils.getAnnotation(error.getClass(), ResponseStatus.class);
                statusCode = annotation != null ? annotation.value() : BAD_REQUEST;
            }
        }
        result.put("status", statusCode.value());
        return ResponseEntity.status(statusCode).body(result);
    }

    private void handleAuthorizationException(HttpServletRequest request, Map<String, Object> result) {
        String remoteIp = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(remoteIp)) {
            remoteIp = remoteIp.split(",")[0].trim(); // first entry is the originating client
        } else {
            remoteIp = request.getRemoteAddr();
        }

        String username = null;
        String authHeader = request.getHeader("Authorization");
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Basic ")) {
            String encoded = authHeader.substring(6);
            String decoded = new String(Base64.getDecoder().decode(encoded));
            username = decoded.split(":", 2)[0];
        }

        String method = request.getMethod();
        String fullPath;
        if (result.containsKey("path")) {
            fullPath = (String) result.get("path");
        } else {
            String path = request.getRequestURI();
            String query = request.getQueryString();
            fullPath = query != null ? path + "?" + query : path;
        }

        LOG.warn(String.format("Authentication error: ip=%s, user=%s, method=%s, path=%s",
                remoteIp, username, method, fullPath
        ));
    }

}
