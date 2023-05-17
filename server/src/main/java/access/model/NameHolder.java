package access.model;

import jakarta.validation.constraints.NotNull;
import java.text.Normalizer;

public interface NameHolder {

    void nameUrnCompatibilityCheck();

    default String compatibleUrnName(@NotNull String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFKD)
                .trim()
                .replaceAll(" ", "_")
                .replaceAll("[^A-Za-z0-9_\\.]", "");
    }
}
