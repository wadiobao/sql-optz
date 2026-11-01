package io.github.wadiobao.sqloptz.engine;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Produces a short identifier for a SQL text that is safe to log.
 *
 * <p>
 * This is a hash, not a normalised statement: it reveals nothing about the
 * text. A proper
 * normalised fingerprint (literals replaced by {@code ?}) is added later for
 * statements that
 * parse.
 */
final class Fingerprints {

    private Fingerprints() {
    }

    static String hash(String sql) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(sql.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 6);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
