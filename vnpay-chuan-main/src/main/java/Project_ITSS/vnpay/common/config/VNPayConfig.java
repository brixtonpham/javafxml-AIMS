/**
 * Configuration class for VNPAY integration
 * Contains all necessary configuration properties and utility methods for VNPAY payment processing
 */
package Project_ITSS.vnpay.common.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import jakarta.servlet.http.HttpServletRequest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Configuration
@Data
/**
 * Configuration class that holds VNPAY-specific settings and utility methods
 * Handles configuration properties, hash generation, and common utilities needed for VNPAY integration
 */
public class VNPayConfig {

    @Value("${vnpay.pay-url}")
    private String payUrl;

    @Value("${vnpay.return-url}")
    private String returnUrl;

    @Value("${vnpay.tmn-code}")
    private String tmnCode;

    @Value("${vnpay.secret-key}")
    private String secretKey;

    @Value("${vnpay.api-url}")
    private String apiUrl;

    public void setTmnCode(String tmnCode) {
        this.tmnCode = tmnCode;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public void setPayUrl(String payUrl) {
        this.payUrl = payUrl;
    }

    public void setReturnUrl(String returnUrl) {
        this.returnUrl = returnUrl;
    }

    public String getPayUrl() {
        return payUrl;
    }

    public String getReturnUrl() {
        return returnUrl;
    }

    public String getTmnCode() {
        return tmnCode;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    /**
     * Generates a hash of all fields in the map for VNPAY request signing
     * @param fields Map containing the parameters to be hashed
     * @return The HMAC-SHA512 hash of all fields concatenated in sorted order
     */
    public String hashAllFields(Map fields) {
        List fieldNames = new ArrayList(fields.keySet());
        Collections.sort(fieldNames);
        StringBuilder sb = new StringBuilder();
        Iterator itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = (String) itr.next();
            String fieldValue = (String) fields.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                sb.append(fieldName);
                sb.append("=");
                sb.append(fieldValue);
            }
            if (itr.hasNext()) {
                sb.append("&");
            }
        }
        return hmacSHA512(getSecretKey(), sb.toString());
    }

    /**
     * Generates HMAC-SHA512 hash for VNPAY request signing
     * @param key The secret key used for signing (vnp_HashSecret)
     * @param data The data to be signed
     * @return Hexadecimal string of the HMAC-SHA512 hash
     */
    public String hmacSHA512(final String key, final String data) {
        try {
            if (key == null || data == null) {
                throw new NullPointerException();
            }
            final Mac hmac512 = Mac.getInstance("HmacSHA512");
            byte[] hmacKeyBytes = key.getBytes();
            final SecretKeySpec secretKey = new SecretKeySpec(hmacKeyBytes, "HmacSHA512");
            hmac512.init(secretKey);
            byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
            byte[] result = hmac512.doFinal(dataBytes);
            StringBuilder sb = new StringBuilder(2 * result.length);
            for (byte b : result) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception ex) {
            return "";
        }
    }

    /**
     * Extracts the client IP address from the HTTP request
     * Handles various proxy scenarios and headers
     * @param request The HTTP request
     * @return The client IP address
     */
    public String getIpAddress(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }

        // For multiple proxies, first address is the client
        if (ipAddress != null && ipAddress.contains(",")) {
            ipAddress = ipAddress.split(",")[0].trim();
        }

        return ipAddress;
    }

    /**
     * Generates a random number string of specified length
     * Used for creating unique transaction references
     * @param len The length of the random number string
     * @return Random number string
     */
    public String getRandomNumber(int len) {
        Random rnd = new Random();
        String chars = "0123456789";
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return sb.toString();
    }
}