package com.aims.core.shared.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class PasswordUtils {

    private static final int SALT_LENGTH = 16; // Salt length in bytes

    // Method to hash a password with a salt
    public static String hashPassword(String password) {
        try {
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[SALT_LENGTH];
            random.nextBytes(salt);

            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            byte[] hashedPassword = md.digest(password.getBytes());

            // Combine salt and hashed password for storage
            byte[] combined = new byte[salt.length + hashedPassword.length];
            System.arraycopy(salt, 0, combined, 0, salt.length);
            System.arraycopy(hashedPassword, 0, combined, salt.length, hashedPassword.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (NoSuchAlgorithmException e) {
            // Handle the exception (e.g., log it, throw a custom exception)
            throw new RuntimeException("Error hashing password: Algorithm not found", e);
        }
    }

    // Method to verify a password against a stored hash
    public static boolean verifyPassword(String password, String storedHash) {
        try {
            byte[] combined = Base64.getDecoder().decode(storedHash);

            // Extract salt and hashed password
            byte[] salt = new byte[SALT_LENGTH];
            System.arraycopy(combined, 0, salt, 0, SALT_LENGTH);

            byte[] hashedPasswordFromStorage = new byte[combined.length - SALT_LENGTH];
            System.arraycopy(combined, SALT_LENGTH, hashedPasswordFromStorage, 0, hashedPasswordFromStorage.length);

            // Hash the input password with the extracted salt
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            byte[] hashedPasswordAttempt = md.digest(password.getBytes());

            // Compare the hashes
            return MessageDigest.isEqual(hashedPasswordFromStorage, hashedPasswordAttempt);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error verifying password: Algorithm not found", e);
        } catch (IllegalArgumentException e) {
            // Handle invalid Base64 string
            System.err.println("Invalid stored hash format: " + e.getMessage());
            return false;
        }
    }
}
