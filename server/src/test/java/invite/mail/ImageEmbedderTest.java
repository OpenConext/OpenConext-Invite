package invite.mail;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ImageEmbedderTest {

    @AfterEach
    void resetClient() {
        ImageEmbedder.HTTP_CLIENT = HttpClient.newHttpClient();
    }

    @Test
    @SuppressWarnings("unchecked")
    void fetchAsDataUrl() throws Exception {
        // Given

        // base64 encoding is "iVBORw0KGgo=".
        byte[] pngBytes = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};

        HttpHeaders headers = HttpHeaders.of(
                Map.of("Content-Type", List.of("image/png")),
                (name, value) -> true);
        HttpResponse<byte[]> response = mock(HttpResponse.class);
        when(response.headers()).thenReturn(headers);
        when(response.body()).thenReturn(pngBytes);

        HttpClient mockClient = mock(HttpClient.class);
        doReturn(response).when(mockClient).send(any(HttpRequest.class), any());
        ImageEmbedder.HTTP_CLIENT = mockClient;

        // When
        Optional<String> dataUrl = ImageEmbedder.fetchAsDataUrl("http://example.com/logo.png");

        // Then
        assertTrue(dataUrl.isPresent());
        assertEquals("data:image/png;base64,iVBORw0KGgo=", dataUrl.get());

        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(mockClient).send(requestCaptor.capture(), any());

        HttpRequest sentRequest = requestCaptor.getValue();
        assertEquals(URI.create("http://example.com/logo.png"), sentRequest.uri());
        assertEquals("GET", sentRequest.method());
    }

    @Test
    void fetchAsDataUrlReturnsEmptyOnFailure() {
        Optional<String> dataUrl = ImageEmbedder.fetchAsDataUrl("not a url");

        assertTrue(dataUrl.isEmpty());
    }
}
