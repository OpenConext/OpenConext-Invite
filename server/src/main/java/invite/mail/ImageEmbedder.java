package invite.mail;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.Optional;

public class ImageEmbedder {

    private static final Log LOG = LogFactory.getLog(ImageEmbedder.class);
    private static final String DEFAULT_CONTENT_TYPE = "image/png";
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    private ImageEmbedder() {
    }

    /**
     * Fetches a remote image and returns it as a data: URL for use in an HTML.
     *
     * @param imageUrl the absolute URL of the image to fetch
     * @return the data: URL, or empty if the image cannot be fetched
     */
    public static Optional<String> fetchAsDataUrl(String imageUrl) {
        try {
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(imageUrl)).build();
            HttpResponse<byte[]> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());

            String contentType = response.headers()
                    .firstValue("Content-Type")
                    .orElse(DEFAULT_CONTENT_TYPE);
            String base64Data = Base64.getEncoder().encodeToString(response.body());

            return Optional.of("data:" + contentType + ";base64," + base64Data);
        } catch (Exception e) {
            LOG.warn(String.format("Error fetching image from %s: %s", imageUrl, e.getMessage()));
            return Optional.empty();
        }
    }
}
