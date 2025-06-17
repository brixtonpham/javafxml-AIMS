/**
 * Controller handling VNPAY payment integration endpoints
 * Provides REST APIs for payment creation, confirmation, query and refund operations
 */
package Project_ITSS.vnpay.common.controller;

import Project_ITSS.vnpay.common.service.VNPayService;
import Project_ITSS.vnpay.common.service.VNPayService.PaymentResponse;
import Project_ITSS.vnpay.common.service.VNPayService.QueryResponse;
import Project_ITSS.vnpay.common.service.VNPayService.RefundResponse;
import Project_ITSS.vnpay.common.dto.IPNResponse;
import Project_ITSS.vnpay.common.dto.PaymentRequest;
import Project_ITSS.vnpay.common.dto.PaymentReturnResponse;
import Project_ITSS.vnpay.common.dto.QueryRequest;
import Project_ITSS.vnpay.common.dto.RefundRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
public class VNPayController {

    private static final Logger logger = LoggerFactory.getLogger(VNPayController.class);
    private final VNPayService vnPayService;

    @Autowired
    public VNPayController(VNPayService vnPayService) {
        this.vnPayService = vnPayService;
    }

    /**
     * View Controllers for rendering JSP pages
     */

    /**
     * API endpoint for application info
     * @return application information
     */
    @GetMapping("/")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> index() {
        Map<String, Object> info = new HashMap<>();
        info.put("application", "AIMS VNPay Integration");
        info.put("status", "running");
        info.put("endpoints", Map.of(
            "payment", "/api/payment",
            "query", "/api/payment/query",
            "refund", "/api/payment/refund",
            "ipn", "/ipn"
        ));
        return ResponseEntity.ok(info);
    }

    /**
     * API endpoint for payment form information
     * @return payment form info
     */
    @GetMapping("/pay")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> paymentPage() {
        Map<String, Object> paymentInfo = new HashMap<>();
        paymentInfo.put("endpoint", "/api/payment");
        paymentInfo.put("method", "POST");
        paymentInfo.put("description", "VNPay Payment Integration");
        paymentInfo.put("required_fields", List.of("amount", "bankCode", "language"));
        return ResponseEntity.ok(paymentInfo);
    }

    /**
     * API endpoint for transaction query information
     * @return query form info
     */
    @GetMapping("/querydr")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> queryPage() {
        Map<String, Object> queryInfo = new HashMap<>();
        queryInfo.put("endpoint", "/api/payment/query");
        queryInfo.put("method", "POST");
        queryInfo.put("description", "VNPay Transaction Query");
        queryInfo.put("required_fields", List.of("orderId", "transDate"));
        return ResponseEntity.ok(queryInfo);
    }

    /**
     * API endpoint for refund request information
     * @return refund form info
     */
    @GetMapping("/refund")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> refundPage() {
        Map<String, Object> refundInfo = new HashMap<>();
        refundInfo.put("endpoint", "/api/payment/refund");
        refundInfo.put("method", "POST");
        refundInfo.put("description", "VNPay Refund Request");
        refundInfo.put("required_fields", List.of("orderId", "amount", "transDate", "user"));
        return ResponseEntity.ok(refundInfo);
    }

    /**
     * Handles the return URL from VNPAY after payment
     * Validates the payment response signature and displays the result
     *
     * @param requestParams Parameters returned from VNPAY
     * @param request HTTP request
     * @param model Spring MVC model
     * @return return page view name
     */
    @GetMapping("/return")
    public String returnPage(
            @RequestParam Map<String, String> requestParams,
            HttpServletRequest request,
            org.springframework.ui.Model model) {
        
        // Create copy of params for hash calculation
        Map<String, String> fields = new HashMap<>(requestParams);
        
        // Get and remove hash from param map before recalculating
        String vnp_SecureHash = fields.get("vnp_SecureHash");
        fields.remove("vnp_SecureHashType");
        fields.remove("vnp_SecureHash");

        // Create response object
        PaymentReturnResponse response = new PaymentReturnResponse();

        // Validate hash
        String signValue = vnPayService.hashAllFields(fields);
        response.setValidHash(signValue.equals(vnp_SecureHash));

        if (response.isValidHash()) {
            // Parse and validate required fields
            try {
                response.setTransactionId(fields.get("vnp_TxnRef"));
                response.setAmount(Long.parseLong(fields.getOrDefault("vnp_Amount", "0")));
                response.setOrderInfo(fields.get("vnp_OrderInfo"));
                response.setResponseCode(fields.get("vnp_ResponseCode"));
                response.setVnpayTransactionId(fields.get("vnp_TransactionNo"));
                response.setBankCode(fields.get("vnp_BankCode"));
                
                // Parse payment date
                String payDate = fields.get("vnp_PayDate");
                if (payDate != null && payDate.length() >= 14) {
                    LocalDateTime dateTime = LocalDateTime.parse(
                        payDate.substring(0, 14),
                        DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                    );
                    response.setPaymentDate(dateTime);
                }
                
                response.setTransactionStatus(fields.get("vnp_TransactionStatus"));

            } catch (Exception e) {
                // Log the error
                logger.error("Error processing return URL parameters", e);
                response.setMessage("Lỗi xử lý thông tin thanh toán");
                response.setValidHash(false);
            }
        }

        // Log transaction details
        logger.info("Payment return - TxnId: {}, Amount: {}, Status: {}, ResponseCode: {}",
            response.getTransactionId(),
            response.getAmount(),
            response.getTransactionStatus(),
            response.getResponseCode()
        );

        model.addAttribute("payment", response);
        return "vnpay_return";
    }

    /**
     * Renders the IPN test page
     * @return IPN test view name
     */
    @GetMapping("/ipn")
    public String ipnPage() {
        return "vnpay_ipn";
    }

    /**
     * REST API Controllers for VNPAY integration
     */
    /**
     * API endpoint for creating a new payment
     * Generates payment URL with VNPAY signature
     *
     * @param request Payment request with amount and other details
     * @param servletRequest HTTP request for client IP
     * @return ResponseEntity with payment URL and status
     */
    @PostMapping("/api/payment")
    @ResponseBody
    public ResponseEntity<PaymentResponse> createPayment(
            @RequestBody PaymentRequest request,
            HttpServletRequest servletRequest) {
        PaymentResponse response = vnPayService.createPayment(request, servletRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * API endpoint for querying transaction status
     *
     * @param request Query parameters with order ID
     * @param servletRequest HTTP request for client IP
     * @return ResponseEntity with transaction details
     */
    @PostMapping("/api/payment/query")
    @ResponseBody
    public ResponseEntity<QueryResponse> queryTransaction(
            @RequestBody QueryRequest request,
            HttpServletRequest servletRequest) {
        QueryResponse response = vnPayService.queryTransaction(request, servletRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * API endpoint for refund requests
     *
     * @param request Refund details including amount
     * @param servletRequest HTTP request for client IP
     * @return ResponseEntity with refund status
     */
    @PostMapping("/api/payment/refund")
    @ResponseBody
    public ResponseEntity<RefundResponse> refundTransaction(
            @RequestBody RefundRequest request,
            HttpServletRequest servletRequest) {
        RefundResponse response = vnPayService.refundTransaction(request, servletRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * Handles IPN (Instant Payment Notification) from VNPAY
     * Validates signature and processes payment confirmation
     *
     * @param requestParams Parameters sent by VNPAY
     * @return ResponseEntity with processing status
     */
    @PostMapping("/ipn")
    @ResponseBody
    public ResponseEntity<IPNResponse> handleIpnNotification(
            @RequestParam MultiValueMap<String, String> requestParams) {
        
        // Convert MultiValueMap to Map<String, String> for VNPay service
        Map<String, String> vnpParams = new HashMap<>();
        requestParams.forEach((key, value) -> {
            if (value != null && !value.isEmpty()) {
                vnpParams.put(key, value.get(0));
            }
        });

        IPNResponse response = vnPayService.handleIpnRequest(vnpParams);
        return ResponseEntity.ok(response);
    }
}