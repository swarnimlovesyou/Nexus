package com.nexus.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Security utilities — SHA-256 with a random 16-byte salt.
 * Format stored: Base64(salt):Base64(SHA-256(salt+password))
 */
public class SecurityUtils {

    private static final int SALT_BYTES = 16;

    public static String hashPassword(String password) {
        try {
            // Generate a random salt
            SecureRandom rng = new SecureRandom();
            byte[] salt = new byte[SALT_BYTES];
            rng.nextBytes(salt);

            byte[] hash = sha256(salt, password);
            return Base64.getEncoder().encodeToString(salt)
                    + ":" + Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Hashing algorithm unavailable: " + e.getMessage());
        }
    }

    public static boolean verifyPassword(String password, String stored) {
        try {
            if (stored == null || !stored.contains(":")) {
                // Legacy plain-text or old unsalted hash — reject to force re-registration
                return false;
            }
            String[] parts = stored.split(":", 2);
            byte[] salt = Base64.getDecoder().decode(parts[0]);
            byte[] expected = Base64.getDecoder().decode(parts[1]);
            byte[] actual = sha256(salt, password);
            // Constant-time comparison to prevent timing attacks
            if (actual.length != expected.length) return false;
            int diff = 0;
            for (int i = 0; i < actual.length; i++) diff |= (actual[i] ^ expected[i]);
            return diff == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private static byte[] sha256(byte[] salt, String password) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(salt);
        return digest.digest(password.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
