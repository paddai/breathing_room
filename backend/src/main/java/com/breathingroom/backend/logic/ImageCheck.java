package com.breathingroom.backend.logic;

import java.util.Optional;

/** Decides whether uploaded bytes really are a small JPEG, PNG or WebP picture. The file's own first bytes decide, not what the browser claims. */
public final class ImageCheck {

    public static final int MAX_BYTES = 512 * 1024;

    private ImageCheck() {}

    public static Optional<String> detect(byte[] b) {
        if (b == null || b.length < 12) return Optional.empty();
        if ((b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) return Optional.of("image/jpeg");
        if ((b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G' && b[4] == 0x0D && b[5] == 0x0A && b[6] == 0x1A && b[7] == 0x0A) {
            return Optional.of("image/png");
        }
        if (b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F' && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return Optional.of("image/webp");
        }
        return Optional.empty();
    }

    /** @return a message the user can act on, or empty when the picture is acceptable. */
    public static Optional<String> problem(byte[] data) {
        if (data == null || data.length == 0) return Optional.of("Choose a picture first.");
        if (data.length > MAX_BYTES) return Optional.of("That picture is too large. Choose a smaller one.");
        if (detect(data).isEmpty()) return Optional.of("Use a JPEG, PNG or WebP picture.");
        return Optional.empty();
    }
}
