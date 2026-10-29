package com.breathingroom.backend.logic;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Issues and checks signed login tokens (standard HS256 JWT format: header.payload.signature).
 * Uses only the JDK, so it is easy to test. Security rules built in:
 *  - the header must be exactly the HS256 header (so "alg":"none" tricks are rejected),
 *  - the signature is compared in constant time,
 *  - the signature is checked BEFORE the payload is trusted,
 *  - tokens expire.
 */
public final class TokenService {

    private static final String HEADER_JSON = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
    private static final Base64.Encoder ENC = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DEC = Base64.getUrlDecoder();
    private static final Pattern SUB = Pattern.compile("\"sub\":\"(\\d{1,18})\"");
    private static final Pattern EXP = Pattern.compile("\"exp\":(\\d{1,12})");

    private final byte[] secret;
    private final long ttlSeconds;
    private final Clock clock;

    public TokenService(byte[] secret, Duration ttl, Clock clock) {
        if (secret == null || secret.length < 32) {
            throw new IllegalArgumentException("The token secret must be at least 32 bytes long.");
        }
        this.secret = secret.clone();
        this.ttlSeconds = ttl.getSeconds();
        this.clock = clock;
    }

    public long ttlSeconds() {
        return ttlSeconds;
    }

    public String issue(long userId) {
        long now = clock.instant().getEpochSecond();
        String payload = "{\"sub\":\"" + userId + "\",\"iat\":" + now + ",\"exp\":" + (now + ttlSeconds) + "}";
        String signingInput = ENC.encodeToString(HEADER_JSON.getBytes(StandardCharsets.UTF_8)) + "."
                + ENC.encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        return signingInput + "." + ENC.encodeToString(hmac(signingInput));
    }

    /** @return the user id if the token is genuine and not expired, otherwise empty. */
    public Optional<Long> verify(String token) {
        try {
            if (token == null || token.length() > 2048) return Optional.empty();
            String[] parts = token.split("\\.", -1);
            if (parts.length != 3) return Optional.empty();
            if (!HEADER_JSON.equals(new String(DEC.decode(parts[0]), StandardCharsets.UTF_8))) return Optional.empty();
            byte[] expected = hmac(parts[0] + "." + parts[1]);
            if (!MessageDigest.isEqual(expected, DEC.decode(parts[2]))) return Optional.empty();
            String payload = new String(DEC.decode(parts[1]), StandardCharsets.UTF_8);
            Matcher sub = SUB.matcher(payload);
            Matcher exp = EXP.matcher(payload);
            if (!sub.find() || !exp.find()) return Optional.empty();
            if (clock.instant().getEpochSecond() >= Long.parseLong(exp.group(1))) return Optional.empty();
            return Optional.of(Long.parseLong(sub.group(1)));
        } catch (IllegalArgumentException e) { // bad base64 or number
            return Optional.empty();
        }
    }

    private byte[] hmac(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 is not available", e);
        }
    }
}
