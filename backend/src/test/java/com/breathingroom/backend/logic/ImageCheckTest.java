package com.breathingroom.backend.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class ImageCheckTest {

    private static byte[] withHeader(int... head) {
        byte[] b = new byte[64];
        for (int i = 0; i < head.length; i++) b[i] = (byte) head[i];
        return b;
    }

    @Test
    void recognisesRealPictureTypesByTheirFirstBytes() {
        assertEquals(Optional.of("image/jpeg"), ImageCheck.detect(withHeader(0xFF, 0xD8, 0xFF, 0xE0)));
        assertEquals(Optional.of("image/png"), ImageCheck.detect(withHeader(0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A)));
        assertEquals(Optional.of("image/webp"), ImageCheck.detect(withHeader('R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P')));
    }

    @Test
    void rejectsScriptsHtmlAndTinyFilesEvenIfNamedLikePictures() {
        assertTrue(ImageCheck.detect("<html><script>alert(1)</script></html>".getBytes()).isEmpty());
        assertTrue(ImageCheck.detect(new byte[] {1, 2, 3}).isEmpty());
        assertTrue(ImageCheck.detect(null).isEmpty());
    }

    @Test
    void problemMessagesAreFriendly() {
        assertEquals(Optional.of("Choose a picture first."), ImageCheck.problem(new byte[0]));
        assertEquals(Optional.of("Use a JPEG, PNG or WebP picture."), ImageCheck.problem(new byte[200]));
        assertTrue(ImageCheck.problem(new byte[ImageCheck.MAX_BYTES + 1]).get().contains("too large"));
        assertTrue(ImageCheck.problem(withHeader(0xFF, 0xD8, 0xFF, 0xE0)).isEmpty());
    }
}
