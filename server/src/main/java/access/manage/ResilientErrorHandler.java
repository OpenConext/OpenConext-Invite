package access.manage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseErrorHandler;

import java.io.IOException;
import java.net.URI;

public class ResilientErrorHandler implements ResponseErrorHandler {

    private static final Log LOG = LogFactory.getLog(ResilientErrorHandler.class);

    @Override
    public boolean hasError(ClientHttpResponse response) throws IOException {
        HttpStatusCode statusCode = response.getStatusCode();
        return statusCode.isError();
    }

    @Override
    public void handleError(ClientHttpResponse response) throws IOException {
        this.handleError(null, null, response);
    }

    @Override
    public void handleError(URI url, HttpMethod method, ClientHttpResponse response) throws IOException {
        //ignore
        LOG.warn(String.format("Error from Manage: %s, %s, %s, %s",
                url,
                method,
                response.getStatusText(),
                new String(response.getBody().readAllBytes())));
    }
}
