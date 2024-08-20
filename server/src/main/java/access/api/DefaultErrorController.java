package access.api;


import access.exception.NotFoundException;
import access.exception.UserRestrictionException;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

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
                        ErrorAttributeOptions.Include.BINDING_ERRORS)
        );

        Throwable error = this.errorAttributes.getError(webRequest);
        HttpStatus statusCode;

        if (error == null) {
            statusCode = result.containsKey("status") && (int) result.get("status") != 999 ?
                    HttpStatus.valueOf((int) result.get("status")) : INTERNAL_SERVER_ERROR;
        } else {
            if (!(error instanceof NotFoundException)) {
                boolean logStackTrace = !(error instanceof UserRestrictionException || error instanceof access.exception.RemoteException);
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

}
