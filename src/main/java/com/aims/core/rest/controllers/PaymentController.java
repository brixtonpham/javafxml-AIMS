package com.aims.core.rest.controllers;

import com.aims.core.application.services.IPaymentService;
import com.aims.core.application.services.IOrderService;
import com.aims.core.entities.OrderEntity;
import com.aims.core.entities.PaymentMethod;
import com.aims.core.entities.PaymentTransaction;
import com.aims.core.shared.ServiceFactory;
import com.aims.core.shared.exceptions.ResourceNotFoundException;
import com.aims.core.shared.exceptions.ValidationException;
import com.aims.core.shared.exceptions.PaymentException;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.util.Map;
import java.util.HashMap;

/**
 * REST controller for payment processing operations
 */
@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "http://localhost:3000")
public class PaymentController extends BaseController {

    private final IPaymentService paymentService;
    private final IOrderService orderService;

    public PaymentController() {
        this.paymentService = ServiceFactory.getPaymentService();
        this.orderService = ServiceFactory.getOrderService();
    }

    /**
     * Process payment for an order
     */
    @PostMapping("/process")
    public ResponseEntity<ApiResponse<PaymentTransaction>> processPayment(@RequestBody ProcessPaymentRequest request) {
        try {
            OrderEntity order = orderService.getOrderDetails(request.getOrderId());
            PaymentTransaction transaction = paymentService.processPayment(order, request.getPaymentMethodId());
            return success(transaction, "Payment processed successfully");
        } catch (ResourceNotFoundException e) {
            return error("Order or payment method not found: " + e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (PaymentException e) {
            return error("Payment processing failed: " + e.getMessage(), HttpStatus.PAYMENT_REQUIRED);
        } catch (ValidationException e) {
            Map<String, String> errors = new HashMap<>();
            errors.put("validation", e.getMessage());
            return validationError("Payment validation failed", errors);
        } catch (Exception e) {
            return error("An error occurred while processing payment: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Process payment with additional parameters
     */
    @PostMapping("/process-with-params")
    public ResponseEntity<ApiResponse<PaymentTransaction>> processPaymentWithParams(@RequestBody ProcessPaymentWithParamsRequest request) {
        try {
            OrderEntity order = orderService.getOrderDetails(request.getOrderId());
            PaymentTransaction transaction = paymentService.processPaymentWithParams(
                order, 
                request.getPaymentMethodId(), 
                request.getAdditionalParams()
            );
            return success(transaction, "Payment processed successfully");
        } catch (ResourceNotFoundException e) {
            return error("Order or payment method not found: " + e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (PaymentException e) {
            return error("Payment processing failed: " + e.getMessage(), HttpStatus.PAYMENT_REQUIRED);
        } catch (ValidationException e) {
            Map<String, String> errors = new HashMap<>();
            errors.put("validation", e.getMessage());
            return validationError("Payment validation failed", errors);
        } catch (Exception e) {
            return error("An error occurred while processing payment: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Process refund for a transaction
     */
    @PostMapping("/refund")
    public ResponseEntity<ApiResponse<PaymentTransaction>> processRefund(@RequestBody ProcessRefundRequest request) {
        try {
            PaymentTransaction refundTransaction = paymentService.processRefund(
                request.getOrderIdToRefund(),
                request.getOriginalGatewayTransactionId(),
                request.getRefundAmount(),
                request.getReason()
            );
            return success(refundTransaction, "Refund processed successfully");
        } catch (ResourceNotFoundException e) {
            return error("Transaction not found: " + e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (PaymentException e) {
            return error("Refund processing failed: " + e.getMessage(), HttpStatus.PAYMENT_REQUIRED);
        } catch (ValidationException e) {
            Map<String, String> errors = new HashMap<>();
            errors.put("validation", e.getMessage());
            return validationError("Refund validation failed", errors);
        } catch (Exception e) {
            return error("An error occurred while processing refund: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Save or update payment method
     */
    @PostMapping("/methods")
    public ResponseEntity<ApiResponse<PaymentMethod>> savePaymentMethod(@RequestBody PaymentMethod paymentMethod) {
        try {
            PaymentMethod savedMethod = paymentService.savePaymentMethod(paymentMethod);
            return success(savedMethod, "Payment method saved successfully");
        } catch (ValidationException e) {
            Map<String, String> errors = new HashMap<>();
            errors.put("validation", e.getMessage());
            return validationError("Payment method validation failed", errors);
        } catch (Exception e) {
            return error("An error occurred while saving payment method: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Delete payment method
     */
    @DeleteMapping("/methods/{paymentMethodId}")
    public ResponseEntity<ApiResponse<String>> deletePaymentMethod(@PathVariable String paymentMethodId) {
        try {
            paymentService.deletePaymentMethod(paymentMethodId);
            return success("Payment method deleted", "Payment method deleted successfully");
        } catch (ResourceNotFoundException e) {
            return error("Payment method not found: " + e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (ValidationException e) {
            Map<String, String> errors = new HashMap<>();
            errors.put("validation", e.getMessage());
            return validationError("Payment method deletion failed", errors);
        } catch (Exception e) {
            return error("An error occurred while deleting payment method: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get payment method by ID
     */
    @GetMapping("/methods/{paymentMethodId}")
    public ResponseEntity<ApiResponse<PaymentMethod>> getPaymentMethodById(@PathVariable String paymentMethodId) {
        try {
            PaymentMethod paymentMethod = paymentService.getPaymentMethodById(paymentMethodId);
            return success(paymentMethod, "Payment method retrieved successfully");
        } catch (ResourceNotFoundException e) {
            return error("Payment method not found: " + e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return error("An error occurred while retrieving payment method: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Find transaction by ID
     */
    @GetMapping("/transactions/{transactionId}")
    public ResponseEntity<ApiResponse<PaymentTransaction>> findTransactionById(@PathVariable String transactionId) {
        try {
            PaymentTransaction transaction = paymentService.findTransactionById(transactionId);
            return success(transaction, "Transaction retrieved successfully");
        } catch (ResourceNotFoundException e) {
            return error("Transaction not found: " + e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return error("An error occurred while retrieving transaction: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Find transaction by external (gateway) ID
     */
    @GetMapping("/transactions/external/{externalTransactionId}")
    public ResponseEntity<ApiResponse<PaymentTransaction>> findTransactionByExternalId(@PathVariable String externalTransactionId) {
        try {
            PaymentTransaction transaction = paymentService.findTransactionByExternalId(externalTransactionId);
            return success(transaction, "Transaction retrieved successfully");
        } catch (ResourceNotFoundException e) {
            return error("Transaction not found: " + e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return error("An error occurred while retrieving transaction: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Update transaction status from gateway callback
     */
    @PostMapping("/transactions/callback")
    public ResponseEntity<ApiResponse<PaymentTransaction>> updateTransactionStatusFromCallback(@RequestBody CallbackUpdateRequest request) {
        try {
            PaymentTransaction transaction = paymentService.updateTransactionStatusFromCallback(
                request.getGatewayTransactionRef(),
                request.getResponseCode(),
                request.getGatewayMessage(),
                request.getSecureHash()
            );
            return success(transaction, "Transaction status updated successfully");
        } catch (PaymentException e) {
            return error("Callback processing failed: " + e.getMessage(), HttpStatus.PAYMENT_REQUIRED);
        } catch (ValidationException e) {
            Map<String, String> errors = new HashMap<>();
            errors.put("validation", e.getMessage());
            return validationError("Callback validation failed", errors);
        } catch (Exception e) {
            return error("An error occurred while processing callback: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Check payment status with gateway
     */
    @GetMapping("/transactions/{transactionId}/status")
    public ResponseEntity<ApiResponse<PaymentTransaction>> checkPaymentStatus(
            @PathVariable String transactionId,
            @RequestParam(required = false) String externalTransactionId) {
        try {
            PaymentTransaction transaction = paymentService.checkPaymentStatus(transactionId, externalTransactionId);
            return success(transaction, "Payment status checked successfully");
        } catch (ResourceNotFoundException e) {
            return error("Transaction not found: " + e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (PaymentException e) {
            return error("Status check failed: " + e.getMessage(), HttpStatus.PAYMENT_REQUIRED);
        } catch (Exception e) {
            return error("An error occurred while checking payment status: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Update transaction status
     */
    @PutMapping("/transactions/{transactionId}/status")
    public ResponseEntity<ApiResponse<String>> updateTransactionStatus(
            @PathVariable String transactionId,
            @RequestBody UpdateTransactionStatusRequest request) {
        try {
            paymentService.updateTransactionStatus(transactionId, request.getStatus());
            return success("Status updated", "Transaction status updated successfully");
        } catch (ResourceNotFoundException e) {
            return error("Transaction not found: " + e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return error("An error occurred while updating transaction status: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get latest transaction for an order
     */
    @GetMapping("/orders/{orderId}/latest-transaction")
    public ResponseEntity<ApiResponse<PaymentTransaction>> getLatestTransactionForOrder(@PathVariable String orderId) {
        try {
            PaymentTransaction transaction = paymentService.getLatestTransactionForOrder(orderId);
            return success(transaction, "Latest transaction retrieved successfully");
        } catch (ResourceNotFoundException e) {
            return error("Transaction not found for order: " + e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return error("An error occurred while retrieving latest transaction: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Request DTOs
    public static class ProcessPaymentRequest {
        private String orderId;
        private String paymentMethodId;

        public String getOrderId() { return orderId; }
        public void setOrderId(String orderId) { this.orderId = orderId; }

        public String getPaymentMethodId() { return paymentMethodId; }
        public void setPaymentMethodId(String paymentMethodId) { this.paymentMethodId = paymentMethodId; }
    }

    public static class ProcessPaymentWithParamsRequest {
        private String orderId;
        private String paymentMethodId;
        private Map<String, Object> additionalParams;

        public String getOrderId() { return orderId; }
        public void setOrderId(String orderId) { this.orderId = orderId; }

        public String getPaymentMethodId() { return paymentMethodId; }
        public void setPaymentMethodId(String paymentMethodId) { this.paymentMethodId = paymentMethodId; }

        public Map<String, Object> getAdditionalParams() { return additionalParams; }
        public void setAdditionalParams(Map<String, Object> additionalParams) { this.additionalParams = additionalParams; }
    }

    public static class ProcessRefundRequest {
        private String orderIdToRefund;
        private String originalGatewayTransactionId;
        private float refundAmount;
        private String reason;

        public String getOrderIdToRefund() { return orderIdToRefund; }
        public void setOrderIdToRefund(String orderIdToRefund) { this.orderIdToRefund = orderIdToRefund; }

        public String getOriginalGatewayTransactionId() { return originalGatewayTransactionId; }
        public void setOriginalGatewayTransactionId(String originalGatewayTransactionId) { this.originalGatewayTransactionId = originalGatewayTransactionId; }

        public float getRefundAmount() { return refundAmount; }
        public void setRefundAmount(float refundAmount) { this.refundAmount = refundAmount; }

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    public static class CallbackUpdateRequest {
        private String gatewayTransactionRef;
        private String responseCode;
        private String gatewayMessage;
        private String secureHash;

        public String getGatewayTransactionRef() { return gatewayTransactionRef; }
        public void setGatewayTransactionRef(String gatewayTransactionRef) { this.gatewayTransactionRef = gatewayTransactionRef; }

        public String getResponseCode() { return responseCode; }
        public void setResponseCode(String responseCode) { this.responseCode = responseCode; }

        public String getGatewayMessage() { return gatewayMessage; }
        public void setGatewayMessage(String gatewayMessage) { this.gatewayMessage = gatewayMessage; }

        public String getSecureHash() { return secureHash; }
        public void setSecureHash(String secureHash) { this.secureHash = secureHash; }
    }

    public static class UpdateTransactionStatusRequest {
        private String status;

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}