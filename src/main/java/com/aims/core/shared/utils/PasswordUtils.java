package com.aims.core.shared.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

// WARNING: This is a basic hashing example.
// For production, use a strong, well-vetted library like BCrypt or Argon2.
// Spring Security's PasswordEncoder interface and implementations are excellent.
public class PasswordUtils {

    private static final String HASH_ALGORITHM = "SHA-256"; // Consider SHA-512 for more strength
    // For BCrypt or Argon2, you wouldn't manually manage salts like this.
    // The library would handle salt generation and embedding it in the hash.

    private PasswordUtils() {}

    /**
     * Hashes a password using SHA-256.
     * IMPORTANT: This basic example does NOT use salting, which is crucial for security.
     * For a real application, use a library like BCrypt that handles salting automatically.
     * @param plainPassword The password to hash.
     * @return The hashed password as a hex string, or null if error.
     */
    public static String hashPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] encodedhash = digest.digest(plainPassword.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(encodedhash);
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Error hashing password: Algorithm not found - " + HASH_ALGORITHM);
            e.printStackTrace(); // Log this critical error
            return null; // Or throw a runtime exception
        }
    }

    /**
     * Verifies a plain password against a stored hashed password.
     * IMPORTANT: This assumes the storedHash was created using the same basic (unsalted) method.
     * @param plainPassword The plain password to verify.
     * @param storedHash The stored hashed password.
     * @return true if the passwords match, false otherwise.
     */
    public static boolean verifyPassword(String plainPassword, String storedHash) {
        if (plainPassword == null || storedHash == null) {
            return false;
        }
        String hashedAttempt = hashPassword(plainPassword);
        return storedHash.equals(hashedAttempt);
    }

    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    // Example of how to use a better library like BCrypt (if you add the dependency)
    // import org.mindrot.jbcrypt.BCrypt;
    // public static String hashPasswordWithBCrypt(String plainPassword) {
    //     return BCrypt.hashpw(plainPassword, BCrypt.gensalt());
    // }
    // public static boolean verifyPasswordWithBCrypt(String plainPassword, String hashedPassword) {
    //     try {
    //         return BCrypt.checkpw(plainPassword, hashedPassword);
    //     } catch (IllegalArgumentException e) {
    //          // Handle cases where hashedPassword is not a valid BCrypt hash
    //          System.err.println("Invalid BCrypt hash format: " + e.getMessage());
    //          return false;
    //     }
    // }
}