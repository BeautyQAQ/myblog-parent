package com.myblog.common.security;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/** Versioned PBKDF2 hashes; legacy MD5 is accepted only for migration during login. */
public final class PasswordSecurity {
    private static final String PREFIX = "pbkdf2-sha256$";
    private static final int ITERATIONS = 600_000;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordSecurity() {}

    public static String hash(String password) {
        if (password == null || password.length() > 128) {
            throw new IllegalArgumentException("Invalid password");
        }
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return PREFIX + ITERATIONS + "$" + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(derive(password, salt, ITERATIONS));
    }

    public static boolean matches(String password, String stored) {
        if (password == null || password.length() > 128 || stored == null) {
            return false;
        }
        try {
            if (isLegacyHash(stored)) {
                byte[] digest = MessageDigest.getInstance("MD5").digest(password.getBytes(StandardCharsets.UTF_8));
                return MessageDigest.isEqual(digest, HexFormat.of().parseHex(stored));
            }
            String[] parts = stored.split("\\$", -1);
            if (parts.length != 4 || !stored.startsWith(PREFIX)) {
                return false;
            }
            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            if (iterations < 100_000 || iterations > 2_000_000 || salt.length != 16 || expected.length != 32) {
                return false;
            }
            return MessageDigest.isEqual(expected, derive(password, salt, iterations));
        } catch (IllegalArgumentException | GeneralSecurityException e) {
            return false;
        }
    }

    public static boolean isLegacyHash(String stored) {
        return stored != null && stored.matches("[0-9a-fA-F]{32}");
    }

    private static byte[] derive(String password, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, 256);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("PBKDF2 unavailable", e);
        } finally {
            spec.clearPassword();
        }
    }
}
