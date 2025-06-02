package com.aims.core.infrastructure.adapters.external.payment_gateway;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class VNPayConfig {
    public static String VNP_TMN_CODE;
    public static String VNP_HASH_SECRET;
    public static String VNP_PAY_URL;
    public static String VNP_API_URL; // For query and refund
    public static String VNP_VERSION = "2.1.0";
    public static String VNP_RETURN_URL;

    static {
        try (InputStream input = VNPayConfig.class.getClassLoader().getResourceAsStream("vnpay_config.properties")) {
            Properties prop = new Properties();
            if (input == null) {
                System.err.println("CRITICAL: Unable to find vnpay_config.properties. VNPay integration will likely fail.");
                // Fallback to placeholder values if file not found - NOT FOR PRODUCTION
                VNP_TMN_CODE = "YOUR_TMN_CODE_HERE"; // Placeholder
                VNP_HASH_SECRET = "YOUR_HASH_SECRET_HERE"; // Placeholder
                VNP_PAY_URL = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
                VNP_API_URL = "https://sandbox.vnpayment.vn/merchant_webapi/api/transaction";
                VNP_RETURN_URL = "http://localhost:8080/aims_project/vnpay_return"; // Example, adjust to your app
            } else {
                prop.load(input);
                VNP_TMN_CODE = prop.getProperty("vnp.tmnCode");
                VNP_HASH_SECRET = prop.getProperty("vnp.hashSecret");
                VNP_PAY_URL = prop.getProperty("vnp.payUrl");
                VNP_API_URL = prop.getProperty("vnp.apiUrl");
                VNP_VERSION = prop.getProperty("vnp.version", "2.1.0");
                VNP_RETURN_URL = prop.getProperty("vnp.returnUrl");

                if (VNP_TMN_CODE == null || VNP_HASH_SECRET == null || VNP_PAY_URL == null || VNP_API_URL == null || VNP_RETURN_URL == null) {
                    System.err.println("CRITICAL: One or more VNPay configuration properties are missing in vnpay_config.properties.");
                }
            }
        } catch (Exception ex) {
            System.err.println("CRITICAL: Error loading VNPay configuration: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    public static String hmacSHA512(String key, String data) {
        try {
            Mac hmacSha512 = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            hmacSha512.init(secretKeySpec);
            byte[] hash = hmacSha512.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            // RuntimeException for critical failure in hashing.
            throw new RuntimeException("Failed to generate HMACSHA512 signature: " + e.getMessage(), e);
        }
    }

    public static String hashAllFields(Map<String, String> fields) {
        List<String> fieldNames = new ArrayList<>(fields.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        boolean first = true;
        for (String fieldName : fieldNames) {
            String fieldValue = fields.get(fieldName);
            if (fieldValue != null && fieldValue.length() > 0) {
                if (!first) {
                    hashData.append('&');
                }
                try {
                    hashData.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString()));
                    hashData.append('=');
                    hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                } catch (UnsupportedEncodingException e) {
                    // This should not happen with US_ASCII
                    throw new RuntimeException("Failed to URL encode field: " + fieldName, e);
                }
                first = false;
            }
        }
        return hashData.toString();
    }
}