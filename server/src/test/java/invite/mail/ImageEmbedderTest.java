package invite.mail;

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
    void fetchAsDataUrlReturnsEmptyOnFailure() {
        // URI.create rejects strings with whitespace, so the util's try/catch
        // produces Optional.empty() without making any HTTP call.
        Optional<String> dataUrl = ImageEmbedder.fetchAsDataUrl("not a url");

        assertTrue(dataUrl.isEmpty());
    }
}
