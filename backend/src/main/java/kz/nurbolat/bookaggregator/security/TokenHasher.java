package kz.nurbolat.bookaggregator.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Opaque tokens (refresh, email links): 32 random bytes for the user, their SHA-256 hash for the database,
 * so that a leaked table gives no working token.
 */
public final class TokenHasher {

    private static final SecureRandom RANDOM = new SecureRandom();

    private TokenHasher() {
    }

    /** 32 random bytes, Base64URL without padding (43 characters). */
    public static String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** SHA-256 of the token as 64 hex characters. */
    public static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    /** Compares in constant time, so that the time taken does not tell how much of the hash matched. */
    public static boolean matches(String token, String expectedHash) {
        return MessageDigest.isEqual(hash(token).getBytes(StandardCharsets.US_ASCII),
                expectedHash.getBytes(StandardCharsets.US_ASCII));
    }
}
