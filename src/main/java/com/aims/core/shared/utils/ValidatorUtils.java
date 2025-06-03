package com.aims.core.shared.utils;

import java.util.regex.Pattern;

public class ValidatorUtils {

    // Basic email validation regex
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@" +
            "(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    );


    private ValidatorUtils() {} // Private constructor to prevent instantiation

    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    public static boolean isNotEmpty(String text) {
        return text != null && !text.trim().isEmpty();
    }
    
    public static boolean isNotEmpty(javafx.scene.control.TextInputControl textField) {
        return textField != null && isNotEmpty(textField.getText());
    }

    public static boolean isValidPhoneNumber(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return false; // Or true if phone is optional
        }
        // Example: Basic check for 10-11 digits for Vietnam, allowing common characters
        // This is a very simplified check. For robust validation, consider a library.
        String cleanedPhone = phone.replaceAll("[^\\d]", ""); // Remove non-digits
        return cleanedPhone.matches("^0\\d{9,10}$") || cleanedPhone.matches("^84\\d{9}$");
        // return PHONE_PATTERN.matcher(phone.trim()).matches(); // If using the more generic regex
    }

    public static boolean isPositiveNumber(String numberText) {
        if (!isNotEmpty(numberText)) return false;
        try {
            double num = Double.parseDouble(numberText.trim());
            return num > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    public static boolean isNonNegativeNumber(String numberText) {
        if (!isNotEmpty(numberText)) return false; // or true if 0 is allowed for an empty field interpreted as 0
        try {
            double num = Double.parseDouble(numberText.trim());
            return num >= 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean isInteger(String numberText) {
        if (!isNotEmpty(numberText)) return false;
        try {
            Integer.parseInt(numberText.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    public static boolean isPositiveInteger(String numberText) {
        if(!isInteger(numberText)) return false;
        return Integer.parseInt(numberText.trim()) > 0;
    }

    public static boolean isNonNegativeInteger(String numberText) {
         if(!isInteger(numberText)) return false;
        return Integer.parseInt(numberText.trim()) >= 0;
    }


    public static boolean isWithinLength(String text, int minLength, int maxLength) {
        if (text == null && minLength == 0) return true;
        if (text == null) return false;
        int length = text.trim().length();
        return length >= minLength && length <= maxLength;
    }

    // Example: Password strength (very basic)
    public static boolean isSufficientlyStrongPassword(String password) {
        if (password == null) return false;
        // At least 8 characters, contains a digit, an uppercase, a lowercase
        // return password.length() >= 8 &&
        //        password.matches(".*\\d.*") &&
        //        password.matches(".*[a-z].*") &&
        //        password.matches(".*[A-Z].*");
        return password.length() >= 8; // Simplified for now
    }
}