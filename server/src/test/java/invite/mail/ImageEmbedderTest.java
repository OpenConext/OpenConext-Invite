package invite.mail;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImageEmbedderTest {

    @Test
    void toDataUrl() {
        // base64 encoding is "iVBORw0KGgo=".
        byte[] pngBytes = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};

        String dataUrl = ImageEmbedder.toDataUrl("image/png", pngBytes);

        assertEquals("data:image/png;base64,iVBORw0KGgo=", dataUrl);
    }

    @Test
    void fetchAsDataUrlReturnsEmptyOnFailure() {
        Optional<String> dataUrl = ImageEmbedder.fetchAsDataUrl("not a url");

        assertTrue(dataUrl.isEmpty());
    }
}
