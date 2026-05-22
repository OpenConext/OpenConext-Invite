package invite.mail;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.InputStream;
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
    private static final int MAX_IMAGE_BYTES = 1 * 1024 * 1024; // 1 MegaByte

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
            HttpResponse<InputStream> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream());

            Optional<byte[]> body = readBounded(response.body(), imageUrl);
            if (body.isEmpty()) {
                return Optional.empty();
            }

            String contentType = response.headers()
                    .firstValue("Content-Type")
                    .orElse(DEFAULT_CONTENT_TYPE);

            return Optional.of(toDataUrl(contentType, body.get()));
        } catch (Exception e) {
            LOG.warn(String.format("Error fetching image from %s: %s", imageUrl, e.getMessage()));
            return Optional.empty();
        }
    }

    private static Optional<byte[]> readBounded(InputStream source, String imageUrl) {
        try (InputStream in = source) {
            byte[] bytes = in.readNBytes(MAX_IMAGE_BYTES + 1);
            if (bytes.length > MAX_IMAGE_BYTES) {
                LOG.warn(String.format("Image at %s exceeds maximum size of %d bytes; aborting download",
                        imageUrl, MAX_IMAGE_BYTES));
                return Optional.empty();
            }
            return Optional.of(bytes);
        } catch (Exception e) {
            LOG.warn(String.format("Error reading image from %s: %s", imageUrl, e.getMessage()));
            return Optional.empty();
        }
    }

    private static String toDataUrl(String contentType, byte[] body) {
        return "data:" + contentType + ";base64," + Base64.getEncoder().encodeToString(body);
    }
}
