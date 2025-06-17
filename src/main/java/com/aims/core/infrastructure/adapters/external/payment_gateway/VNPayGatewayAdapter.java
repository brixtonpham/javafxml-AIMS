package com.aims.core.infrastructure.adapters.external.payment_gateway;

import com.aims.core.entities.OrderEntity;
import com.aims.core.entities.PaymentMethod;
import com.aims.core.entities.CardDetails;
import com.aims.core.enums.PaymentMethodType;
import com.aims.core.shared.exceptions.PaymentException;
import com.aims.core.shared.exceptions.ValidationException;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * VNPay Gateway Adapter Implementation
 * 
 * This class implements the universal IPaymentGatewayAdapter interface
 * specifically for VNPay payment gateway, replacing the old VNPayAdapterImpl.
 * 
 * Key improvements:
 * - Implements universal interface for better abstraction
 * - Enhanced error handling and logging
 * - Standardized response mapping
 * - Better configuration management
 */
public class VNPayGatewayAdapter implements IPaymentGatewayAdapter {

    private static final String GATEWAY_TYPE = "VNPAY";
    private static final int CONNECT_TIMEOUT = 15000; // milliseconds
    private static final int READ_TIMEOUT = 15000; // milliseconds
    private final Gson gson = new Gson();
    
    // Field mappings for standardization
    private static final Map<String, String> FIELD_MAPPINGS = Map.of(
        "transaction_ref", "vnp_TxnRef",
        "response_code", "vnp_ResponseCode",
        "transaction_id", "vnp_TransactionNo",
        "amount", "vnp_Amount",
        "message", "vnp_Message",
        "payment_date", "vnp_PayDate",
        "bank_code", "vnp_BankCode"
    );

    public VNPayGatewayAdapter() {
        // Ensure config is loaded
        if (!isConfigured()) {
            System.err.println("WARNING: VNPayGatewayAdapter initialized with missing configuration. Please check vnpay_config.properties.");
        }
    }

    @Override
    public Map<String, Object> preparePaymentParameters(OrderEntity order, PaymentMethod paymentMethod, CardDetails cardDetails) throws ValidationException {
        if (order == null || order.getTotalAmountPaid() <= 0) {
            throw new ValidationException("Order information and a valid positive amount are required for VNPay payment.");
        }

        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_Version", VNPayConfig.VNP_VERSION);
        vnpParams.put("vnp_Command", "pay");
        vnpParams.put("vnp_TmnCode", VNPayConfig.VNP_TMN_CODE);
        vnpParams.put("vnp_Amount", String.valueOf((int) (order.getTotalAmountPaid() * 100))); // Amount in cents
        vnpParams.put("vnp_CurrCode", "VND");

        // Bank Code logic based on payment method type
        if (paymentMethod != null) {
            if (paymentMethod.getMethodType() == PaymentMethodType.DOMESTIC_DEBIT_CARD) {
                // For domestic cards, bank code is usually required and selected by user
                // Leave vnp_BankCode empty for VNPay's bank selection page
            } else if (paymentMethod.getMethodType() == PaymentMethodType.CREDIT_CARD) {
                // For international cards, vnp_BankCode might be "INTCARD" or empty for VNPay's selection page
                vnpParams.put("vnp_BankCode", "INTCARD");
            }
        }

        vnpParams.put("vnp_TxnRef", order.getOrderId() + "_" + System.currentTimeMillis()); // Unique transaction reference
        vnpParams.put("vnp_OrderInfo", "Thanh toan don hang AIMS: " + order.getOrderId());
        vnpParams.put("vnp_OrderType", "other"); // General type
        vnpParams.put("vnp_Locale", "vn");
        vnpParams.put("vnp_ReturnUrl", VNPayConfig.VNP_RETURN_URL);
        vnpParams.put("vnp_IpAddr", "127.0.0.1"); // TODO: Replace with actual client IP address

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        vnpParams.put("vnp_CreateDate", formatter.format(cld.getTime()));

        cld.add(Calendar.MINUTE, 15); // Payment expiry: 15 minutes from creation
        vnpParams.put("vnp_ExpireDate", formatter.format(cld.getTime()));

        // Convert Map<String, String> to Map<String, Object> for consistency
        return new HashMap<>(vnpParams);
    }

    @Override
    public Map<String, Object> prepareRefundParameters(OrderEntity order, String originalGatewayTransactionId, float refundAmount, String reason) throws ValidationException {
        if (order == null || originalGatewayTransactionId == null || originalGatewayTransactionId.trim().isEmpty() || refundAmount <= 0) {
            throw new ValidationException("Order, original gateway transaction ID, and a valid positive refund amount are required.");
        }

        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_RequestId", "REFUND_" + order.getOrderId() + "_" + System.currentTimeMillis());
        vnpParams.put("vnp_Version", VNPayConfig.VNP_VERSION);
        vnpParams.put("vnp_Command", "refund");
        vnpParams.put("vnp_TmnCode", VNPayConfig.VNP_TMN_CODE);
        
        // Determine refund type: 02 for full refund, 03 for partial refund
        vnpParams.put("vnp_TransactionType", (refundAmount == order.getTotalAmountPaid()) ? "02" : "03");
        vnpParams.put("vnp_TxnRef", order.getOrderId()); // AIMS's original transaction reference
        vnpParams.put("vnp_Amount", String.valueOf((int) (refundAmount * 100)));
        vnpParams.put("vnp_OrderInfo", "Hoan tien don hang AIMS: " + order.getOrderId() + ". Ly do: " + reason);
        vnpParams.put("vnp_TransactionNo", originalGatewayTransactionId); // VNPay's transaction number

        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String originalTxnDateStr = formatter.format(Date.from(order.getOrderDate().atZone(ZoneId.systemDefault()).toInstant()));
        vnpParams.put("vnp_TransactionDate", originalTxnDateStr);

        vnpParams.put("vnp_CreateBy", "AIMS_System");
        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        vnpParams.put("vnp_CreateDate", formatter.format(cld.getTime()));
        vnpParams.put("vnp_IpAddr", "127.0.0.1");

        return new HashMap<>(vnpParams);
    }

    @Override
    public Map<String, String> processPayment(Map<String, Object> paymentParamsObj) throws PaymentException {
        // Convert Map<String, Object> to Map<String, String> for hashing and query string
        Map<String, String> paymentParams = paymentParamsObj.entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, e -> String.valueOf(e.getValue())));

        String hashDataString = VNPayConfig.hashAllFields(paymentParams);
        String vnp_SecureHash = VNPayConfig.hmacSHA512(VNPayConfig.VNP_HASH_SECRET, hashDataString);
        paymentParams.put("vnp_SecureHash", vnp_SecureHash);

        StringBuilder query = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : paymentParams.entrySet()) {
            if (!first) {
                query.append('&');
            }
            try {
                query.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8.toString()));
                query.append('=');
                query.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8.toString()));
            } catch (UnsupportedEncodingException e) {
                throw new PaymentException("Error encoding payment URL parameters", e);
            }
            first = false;
        }
        String paymentUrl = VNPayConfig.VNP_PAY_URL + "?" + query.toString();

        Map<String, String> result = new HashMap<>();
        result.put("paymentUrl", paymentUrl);
        result.put("vnp_TxnRef", paymentParams.get("vnp_TxnRef"));
        result.put("gateway_type", GATEWAY_TYPE);
        
        System.out.println("VNPay Payment URL generated: " + paymentUrl);
        return result;
    }

    @Override
    public Map<String, String> processRefund(Map<String, Object> refundParamsObj) throws PaymentException {
        Map<String, String> refundParams = refundParamsObj.entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, e -> String.valueOf(e.getValue())));

        String hashDataString = VNPayConfig.hashAllFields(refundParams);
        String vnp_SecureHash = VNPayConfig.hmacSHA512(VNPayConfig.VNP_HASH_SECRET, hashDataString);
        refundParams.put("vnp_SecureHash", vnp_SecureHash);

        String jsonPayload = gson.toJson(refundParams);
        Map<String, String> responseMap = sendPostRequest(VNPayConfig.VNP_API_URL, jsonPayload);
        
        if (!"00".equals(responseMap.get("vnp_ResponseCode"))) {
            throw new PaymentException("VNPay refund request failed. Code: " + responseMap.get("vnp_ResponseCode") +
                                       " Message: " + responseMap.get("vnp_Message") + " (TxnRef: "+ refundParams.get("vnp_TxnRef") +")");
        }
        
        // Add gateway type to response
        responseMap.put("gateway_type", GATEWAY_TYPE);
        return responseMap;
    }

    @Override
    public Map<String, String> queryTransactionStatus(String gatewayTransactionRef, String aimsOrderId, LocalDateTime originalTransactionDate) throws PaymentException {
        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_RequestId", "QUERY_" + aimsOrderId + "_" + System.currentTimeMillis());
        vnpParams.put("vnp_Version", VNPayConfig.VNP_VERSION);
        vnpParams.put("vnp_Command", "querydr");
        vnpParams.put("vnp_TmnCode", VNPayConfig.VNP_TMN_CODE);
        vnpParams.put("vnp_TxnRef", gatewayTransactionRef);
        vnpParams.put("vnp_OrderInfo", "Truy van GD don hang " + aimsOrderId);

        DateTimeFormatter vnpFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        vnpParams.put("vnp_TransactionDate", originalTransactionDate.format(vnpFormatter));

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        vnpParams.put("vnp_CreateDate", formatter.format(cld.getTime()));
        vnpParams.put("vnp_IpAddr", "127.0.0.1");

        String hashDataString = VNPayConfig.hashAllFields(vnpParams);
        String vnp_SecureHash = VNPayConfig.hmacSHA512(VNPayConfig.VNP_HASH_SECRET, hashDataString);
        vnpParams.put("vnp_SecureHash", vnp_SecureHash);

        String jsonPayload = gson.toJson(vnpParams);
        Map<String, String> responseMap = sendPostRequest(VNPayConfig.VNP_API_URL, jsonPayload);

        if (!"00".equals(responseMap.get("vnp_ResponseCode"))) {
             throw new PaymentException("VNPay query transaction API call failed. Code: " + responseMap.get("vnp_ResponseCode") +
                                       " Message: " + responseMap.get("vnp_Message"));
        }
        
        // Add gateway type to response
        responseMap.put("gateway_type", GATEWAY_TYPE);
        return responseMap;
    }

    @Override
    public boolean validateResponseSignature(Map<String, String> responseParams) {
        String vnpSecureHashReceived = responseParams.get("vnp_SecureHash");
        if (vnpSecureHashReceived == null || vnpSecureHashReceived.isEmpty()) {
            System.err.println("VNPay response validation: Missing vnp_SecureHash.");
            return false;
        }

        Map<String, String> fieldsForHashing = new HashMap<>(responseParams);
        fieldsForHashing.remove("vnp_SecureHash");
        fieldsForHashing.remove("vnp_SecureHashType");

        String hashData = VNPayConfig.hashAllFields(fieldsForHashing);
        String calculatedHash = VNPayConfig.hmacSHA512(VNPayConfig.VNP_HASH_SECRET, hashData);
        
        boolean isValid = calculatedHash.equalsIgnoreCase(vnpSecureHashReceived);
        if(!isValid){
            System.err.println("VNPay response validation: Hash mismatch!");
            System.err.println("Expected Hash: " + calculatedHash);
            System.err.println("Received Hash: " + vnpSecureHashReceived);
            System.err.println("Data Hashed: " + hashData);
        }
        return isValid;
    }

    @Override
    public String getGatewayType() {
        return GATEWAY_TYPE;
    }

    @Override
    public boolean isConfigured() {
        return VNPayConfig.VNP_TMN_CODE != null && 
               !VNPayConfig.VNP_TMN_CODE.contains("YOUR_TMN_CODE") &&
               VNPayConfig.VNP_HASH_SECRET != null &&
               !VNPayConfig.VNP_HASH_SECRET.contains("YOUR_HASH_SECRET") &&
               VNPayConfig.VNP_PAY_URL != null &&
               VNPayConfig.VNP_API_URL != null &&
               VNPayConfig.VNP_RETURN_URL != null;
    }

    @Override
    public String mapResponseCodeToStatus(String gatewayResponseCode) {
        if (gatewayResponseCode == null) {
            return "FAILED";
        }
        
        switch (gatewayResponseCode) {
            case "00":
                return "SUCCESS";
            case "24":
                return "CANCELLED";
            case "07":
                return "PENDING";
            case "09":
            case "10":
            case "11":
            case "12":
            case "13":
            case "51":
            case "65":
            case "75":
            case "79":
            default:
                return "FAILED";
        }
    }

    @Override
    public Map<String, String> getFieldMappings() {
        return new HashMap<>(FIELD_MAPPINGS);
    }

    /**
     * Send HTTP POST request to VNPay API
     */
    private Map<String, String> sendPostRequest(String requestUrl, String payload) throws PaymentException {
        try {
            URL url = new URL(requestUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(CONNECT_TIMEOUT);
            conn.setReadTimeout(READ_TIMEOUT);

            try (DataOutputStream dos = new DataOutputStream(conn.getOutputStream())) {
                dos.writeBytes(payload);
            }

            int responseCode = conn.getResponseCode();
            StringBuilder responseContent = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(
                    (responseCode == 200) ? conn.getInputStream() : conn.getErrorStream()))) {
                String output;
                while ((output = br.readLine()) != null) {
                    responseContent.append(output);
                }
            }
            conn.disconnect();

            System.out.println("VNPay API Response (" + requestUrl + " - " + responseCode + "): " + responseContent.toString());
            
            Map<String, String> responseMap = gson.fromJson(responseContent.toString(), new TypeToken<Map<String, String>>(){}.getType());
            if (responseMap == null) {
                throw new PaymentException("Failed to parse VNPay API response. Raw: " + responseContent.toString());
            }
            return responseMap;

        } catch (IOException e) {
            throw new PaymentException("IOException during VNPay API request to " + requestUrl + ": " + e.getMessage(), e);
        } catch (Exception e) {
            throw new PaymentException("Unexpected error during VNPay API request to " + requestUrl + ": " + e.getMessage(), e);
        }
    }
}