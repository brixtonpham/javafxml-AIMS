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

public class VNPayAdapterImpl implements IVNPayAdapter {

    private static final int CONNECT_TIMEOUT = 15000; // milliseconds
    private static final int READ_TIMEOUT = 15000; // milliseconds
    private final Gson gson = new Gson();

    public VNPayAdapterImpl() {
        // Ensure config is loaded
        if (VNPayConfig.VNP_TMN_CODE == null || VNPayConfig.VNP_TMN_CODE.contains("YOUR_TMN_CODE")) {
             System.err.println("WARNING: VNPayAdapterImpl initialized with placeholder or missing VNPayConfig. Please check vnpay_config.properties.");
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
                // For domestic cards, bank code is usually required and selected by user.
                // Assume it's passed via paymentParams or stored in CardDetails.issuingBank (needs mapping to VNPay codes)
                // String bankCode = cardDetails != null ? cardDetails.getIssuingBank() : null; // Example
                // if (bankCode == null || bankCode.isEmpty()) {
                //     throw new ValidationException("Bank code is required for Domestic Debit Card payments via VNPay.");
                // }
                // vnpParams.put("vnp_BankCode", bankCode);
                // If user selects from VNPay list, leave vnp_BankCode empty.
            } else if (paymentMethod.getMethodType() == PaymentMethodType.CREDIT_CARD) {
                // For international cards, vnp_BankCode might be "INTCARD" or empty for VNPay's selection page
                vnpParams.put("vnp_BankCode", "INTCARD"); // Or leave empty
            }
        }
        // If vnp_BankCode is empty, VNPay will display a list of banks for the user to choose.

        vnpParams.put("vnp_TxnRef", order.getOrderId() + "_" + System.currentTimeMillis()); // Unique transaction reference for AIMS
        vnpParams.put("vnp_OrderInfo", "Thanh toan don hang AIMS: " + order.getOrderId());
        vnpParams.put("vnp_OrderType", "other"); // General type, or more specific if applicable
        vnpParams.put("vnp_Locale", "vn");
        vnpParams.put("vnp_ReturnUrl", VNPayConfig.VNP_RETURN_URL);
        vnpParams.put("vnp_IpAddr", "127.0.0.1"); // TODO: Replace with actual client IP address from request

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh")); // Use Asia/Ho_Chi_Minh for VNPay
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        vnpParams.put("vnp_CreateDate", formatter.format(cld.getTime()));

        cld.add(Calendar.MINUTE, 15); // Payment expiry: 15 minutes from creation
        vnpParams.put("vnp_ExpireDate", formatter.format(cld.getTime()));

        // Convert Map<String, String> to Map<String, Object> for consistency if needed by strategies
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
        vnpParams.put("vnp_Command", "refund"); // Use " पूर्ण वापसी" for full refund, "partialrefund" for partial
        vnpParams.put("vnp_TmnCode", VNPayConfig.VNP_TMN_CODE);
        // For refund type: 02 for full refund after transaction date, 03 for partial refund
        // This depends on whether the refundAmount matches the original order.getTotalAmountPaid()
        // and whether it's same day or later. For simplicity, let's assume type based on amount.
        vnpParams.put("vnp_TransactionType", (refundAmount == order.getTotalAmountPaid()) ? "02" : "03");
        vnpParams.put("vnp_TxnRef", order.getOrderId()); // AIMS's original transaction reference for the order
        vnpParams.put("vnp_Amount", String.valueOf((int) (refundAmount * 100)));
        vnpParams.put("vnp_OrderInfo", "Hoan tien don hang AIMS: " + order.getOrderId() + ". Ly do: " + reason);
        vnpParams.put("vnp_TransactionNo", originalGatewayTransactionId); // VNPay's transaction number for the original payment

        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        // vnp_TransactionDate: Original transaction date at VNPay (YYYYMMDDHHMMSS)
        // This needs to be fetched from the original payment transaction record.
        // Assuming order.getOrderDate() is close enough for example or you store vnp_PayDate from payment.
        String originalTxnDateStr = formatter.format(Date.from(order.getOrderDate().atZone(ZoneId.systemDefault()).toInstant())); // Placeholder
        vnpParams.put("vnp_TransactionDate", originalTxnDateStr);

        vnpParams.put("vnp_CreateBy", "AIMS_System"); // User performing refund, or system
        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        vnpParams.put("vnp_CreateDate", formatter.format(cld.getTime()));
        vnpParams.put("vnp_IpAddr", "127.0.0.1"); // Server IP making the request

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
        // This method primarily returns the URL. The actual status comes via callback.
        // No direct HTTP call here for payment URL generation, as it's a redirect.
        System.out.println("VNPay Payment URL to redirect: " + paymentUrl);
        return result;
    }

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
        
        if (!"00".equals(responseMap.get("vnp_ResponseCode"))) { // "00" for success, other codes indicate specific errors
            throw new PaymentException("VNPay refund request failed. Code: " + responseMap.get("vnp_ResponseCode") +
                                       " Message: " + responseMap.get("vnp_Message") + " (TxnRef: "+ refundParams.get("vnp_TxnRef") +")");
        }
        return responseMap;
    }

    @Override
    public Map<String, String> queryTransactionStatus(String vnpAimsTxnRef, String aimsOrderId, LocalDateTime originalAimsTransactionDate) throws PaymentException {
        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_RequestId", "QUERY_" + aimsOrderId + "_" + System.currentTimeMillis());
        vnpParams.put("vnp_Version", VNPayConfig.VNP_VERSION);
        vnpParams.put("vnp_Command", "querydr");
        vnpParams.put("vnp_TmnCode", VNPayConfig.VNP_TMN_CODE);
        vnpParams.put("vnp_TxnRef", vnpAimsTxnRef); // Mã giao dịch của AIMS đã gửi cho VNPay khi tạo GD
        vnpParams.put("vnp_OrderInfo", "Truy van GD don hang " + aimsOrderId);

        DateTimeFormatter vnpFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        vnpParams.put("vnp_TransactionDate", originalAimsTransactionDate.format(vnpFormatter)); // Ngày giao dịch gốc của AIMS

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        vnpParams.put("vnp_CreateDate", formatter.format(cld.getTime()));
        vnpParams.put("vnp_IpAddr", "127.0.0.1"); // IP của server AIMS

        String hashDataString = VNPayConfig.hashAllFields(vnpParams);
        String vnp_SecureHash = VNPayConfig.hmacSHA512(VNPayConfig.VNP_HASH_SECRET, hashDataString);
        vnpParams.put("vnp_SecureHash", vnp_SecureHash);

        String jsonPayload = gson.toJson(vnpParams);
        Map<String, String> responseMap = sendPostRequest(VNPayConfig.VNP_API_URL, jsonPayload);

        // Example success condition based on VNPay documentation for querydr
        // vnp_ResponseCode: "00" is success of API call
        // vnp_TransactionStatus: "00" is successful payment transaction
        if (!"00".equals(responseMap.get("vnp_ResponseCode")) ) {
             throw new PaymentException("VNPay query transaction API call failed. Code: " + responseMap.get("vnp_ResponseCode") +
                                       " Message: " + responseMap.get("vnp_Message"));
        }
        // If vnp_ResponseCode is "00", then check vnp_TransactionStatus
        // if (!"00".equals(responseMap.get("vnp_TransactionStatus"))) {
        //     // Transaction itself might not be successful or found
        //     // Handle based on specific vnp_TransactionStatus codes
        // }
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
        fieldsForHashing.remove("vnp_SecureHashType"); // VNPay might use this, remove if present

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
}