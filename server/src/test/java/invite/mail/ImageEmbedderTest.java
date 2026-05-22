package invite.mail;

import com.github.tomakehurst.wiremock.http.Fault;
import invite.WireMockExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImageEmbedderTest {

    @RegisterExtension
    WireMockExtension mockServer = new WireMockExtension(8093);

    @Test
    void fetchAsDataUrl() {
        // base64 encoding: "iVBORw0KGgo="
        byte[] pngBytes = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
        stubFor(get(urlPathEqualTo("/logo.png")).willReturn(aResponse()
                .withHeader("Content-Type", "image/png")
                .withBody(pngBytes)));

        Optional<String> dataUrl = ImageEmbedder.fetchAsDataUrl("http://localhost:8093/logo.png");

        assertTrue(dataUrl.isPresent());
        assertEquals("data:image/png;base64,iVBORw0KGgo=", dataUrl.get());
        verify(getRequestedFor(urlPathEqualTo("/logo.png")));
    }

    @Test
    void fetchAsDataUrlReturnsEmptyWhenImageExceedsMaxSize() {
        // One byte over the 1 MB MAX_IMAGE_BYTES cap
        byte[] oversizedBody = new byte[1024 * 1024 + 1];
        stubFor(get(urlPathEqualTo("/huge.png")).willReturn(aResponse()
                .withHeader("Content-Type", "image/png")
                .withBody(oversizedBody)));

        Optional<String> dataUrl = ImageEmbedder.fetchAsDataUrl("http://localhost:8093/huge.png");

        assertTrue(dataUrl.isEmpty());
        verify(getRequestedFor(urlPathEqualTo("/huge.png")));
    }

    @Test
    void fetchAsDataUrlReturnsEmptyOnBodyReadFailure() {
        stubFor(get(urlPathEqualTo("/broken.png")).willReturn(aResponse()
                .withFault(Fault.MALFORMED_RESPONSE_CHUNK)));

        Optional<String> dataUrl = ImageEmbedder.fetchAsDataUrl("http://localhost:8093/broken.png");

        assertTrue(dataUrl.isEmpty());
        verify(getRequestedFor(urlPathEqualTo("/broken.png")));
    }

    @Test
    void fetchAsDataUrlReturnsEmptyOnFailure() {
        Optional<String> dataUrl = ImageEmbedder.fetchAsDataUrl("not a url");

        assertTrue(dataUrl.isEmpty());
    }
}
