package com.aims.core.presentation.controllers;

import com.aims.core.infrastructure.adapters.external.payment_gateway.IPaymentGatewayAdapter;
import com.aims.core.infrastructure.adapters.external.payment_gateway.VNPayGatewayAdapter;
import com.aims.core.infrastructure.database.dao.IPaymentTransactionDAO;
import com.aims.core.infrastructure.database.dao.PaymentTransactionDAOImpl;
import com.aims.core.infrastructure.database.dao.IOrderEntityDAO;
import com.aims.core.infrastructure.database.dao.OrderEntityDAOImpl;
import com.aims.core.infrastructure.database.dao.IPaymentMethodDAO;
import com.aims.core.infrastructure.database.dao.PaymentMethodDAOImpl;
import com.aims.core.infrastructure.database.dao.IUserAccountDAO;
import com.aims.core.infrastructure.database.dao.UserAccountDAOImpl;
import com.aims.core.infrastructure.database.dao.IProductDAO;
import com.aims.core.infrastructure.database.dao.ProductDAOImpl;
import com.aims.core.infrastructure.database.dao.IOrderItemDAO;
import com.aims.core.infrastructure.database.dao.OrderItemDAOImpl;
import com.aims.core.infrastructure.database.dao.ICardDetailsDAO;
import com.aims.core.infrastructure.database.dao.CardDetailsDAOImpl;
import com.aims.core.entities.PaymentTransaction;
import com.aims.core.presentation.utils.PaymentErrorHandler;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;

import java.util.Map;
import java.util.List;
import java.sql.SQLException;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * VNPay Return Controller
 * Handles user redirects from VNPay after payment completion
 * This displays the payment result to the user
 */
public class VNPayReturnController {
    
    private static final Logger logger = Logger.getLogger(VNPayReturnController.class.getName());
    
    @FXML private Label statusLabel;
    @FXML private Label transactionIdLabel;
    @FXML private Label amountLabel;
    @FXML private Label orderIdLabel;
    @FXML private Label messageLabel;
    @FXML private Button continueButton;
    @FXML private Button retryButton;
    @FXML private VBox contentContainer;
    
    private final IPaymentGatewayAdapter paymentGatewayAdapter;
    private final IPaymentTransactionDAO paymentTransactionDAO;
    private PaymentTransaction currentTransaction;
    
    public VNPayReturnController() {
        this.paymentGatewayAdapter = new VNPayGatewayAdapter();
        
        // Create required DAO dependencies
        IUserAccountDAO userAccountDAO = new UserAccountDAOImpl();
        IProductDAO productDAO = new ProductDAOImpl();
        IOrderItemDAO orderItemDAO = new OrderItemDAOImpl(productDAO);
        ICardDetailsDAO cardDetailsDAO = new CardDetailsDAOImpl();
        
        // Create DAOs with proper dependencies
        IOrderEntityDAO orderDAO = new OrderEntityDAOImpl(orderItemDAO, userAccountDAO);
        IPaymentMethodDAO paymentMethodDAO = new PaymentMethodDAOImpl(userAccountDAO, cardDetailsDAO);
        this.paymentTransactionDAO = new PaymentTransactionDAOImpl(orderDAO, paymentMethodDAO);
    }
    
    /**
     * Initialize the controller with VNPay return parameters
     * This method should be called when user returns from VNPay
     */
    public void initialize() {
        // This will be called automatically by JavaFX
        setupUI();
    }
    
    /**
     * Process VNPay return parameters and display result to user
     * 
     * @param returnParams Parameters returned from VNPay after payment
     */
    public void processVNPayReturn(Map<String, String> returnParams) {
        String vnpTxnRef = null;
        
        try {
            // Enhanced input validation
            if (returnParams == null || returnParams.isEmpty()) {
                logger.log(Level.SEVERE, "VNPay Return: Received null or empty return parameters");
                showError("Invalid Payment Response", "No payment information received. Please contact support if you completed the payment.");
                return;
            }
            
            vnpTxnRef = returnParams.get("vnp_TxnRef");
            logger.log(Level.INFO, "VNPay Return: Processing return for transaction: " + vnpTxnRef);
            
            // Step 1: Validate signature with enhanced error handling
            try {
                if (!paymentGatewayAdapter.validateResponseSignature(returnParams)) {
                    logger.log(Level.SEVERE, "VNPay Return: Invalid signature for transaction: " + vnpTxnRef);
                    
                    // Handle signature validation failure with centralized error handling
                    PaymentErrorHandler.handleSignatureValidationFailure(vnpTxnRef, returnParams);
                    
                    showError("Payment verification failed",
                             "The payment response could not be verified. This may indicate a security issue.",
                             "Please contact support immediately with reference: " + PaymentErrorHandler.generateErrorReference());
                    return;
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "VNPay Return: Error during signature validation for transaction: " + vnpTxnRef, e);
                
                PaymentErrorHandler.handleCallbackValidationError(e, vnpTxnRef, returnParams);
                
                showError("Payment Verification Error",
                         "Unable to verify payment response due to a technical error.",
                         "Please contact support with reference: " + PaymentErrorHandler.generateErrorReference());
                return;
            }
            
            // Step 2: Extract and validate payment information
            String vnpResponseCode = returnParams.get("vnp_ResponseCode");
            String vnpTransactionNo = returnParams.get("vnp_TransactionNo");
            String vnpAmount = returnParams.get("vnp_Amount");
            String vnpBankCode = returnParams.get("vnp_BankCode");
            String vnpPayDate = returnParams.get("vnp_PayDate");
            
            // Validate critical parameters
            if (vnpTxnRef == null || vnpTxnRef.trim().isEmpty()) {
                logger.log(Level.SEVERE, "VNPay Return: Missing transaction reference");
                showError("Invalid Payment Response", "Transaction reference is missing from payment response.");
                return;
            }
            
            if (vnpResponseCode == null || vnpResponseCode.trim().isEmpty()) {
                logger.log(Level.SEVERE, "VNPay Return: Missing response code for transaction: " + vnpTxnRef);
                showError("Invalid Payment Response", "Payment status is missing from response.");
                return;
            }
            
            // Step 3: Find the transaction with enhanced error handling
            try {
                currentTransaction = findTransactionByReference(vnpTxnRef);
                if (currentTransaction == null) {
                    logger.log(Level.WARNING, "VNPay Return: Transaction not found for reference: " + vnpTxnRef);
                    showError("Transaction Not Found",
                             "Could not find the payment transaction in our system.",
                             "This may occur if the payment was processed but not properly recorded. Please contact support with transaction reference: " + vnpTxnRef);
                    return;
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "VNPay Return: Error finding transaction for reference: " + vnpTxnRef, e);
                
                PaymentErrorHandler.handleCallbackValidationError(e, vnpTxnRef, returnParams);
                
                showError("System Error",
                         "Unable to retrieve transaction information due to a system error.",
                         "Please contact support with reference: " + PaymentErrorHandler.generateErrorReference());
                return;
            }
            
            // Step 4: Display result based on response code
            displayPaymentResult(vnpResponseCode, vnpTransactionNo, vnpAmount, vnpBankCode, vnpPayDate);
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "VNPay Return: Unexpected error processing return for transaction: " + vnpTxnRef, e);
            
            // Handle unexpected processing errors with centralized error handling
            PaymentErrorHandler.handleCallbackValidationError(e, vnpTxnRef, returnParams);
            
            String errorRef = PaymentErrorHandler.generateErrorReference();
            showError("Processing Error",
                     "An unexpected error occurred while processing the payment result.",
                     "Please contact support with reference: " + errorRef);
        }
    }
    
    /**
     * Display payment result to user
     */
    private void displayPaymentResult(String responseCode, String transactionNo,
                                    String amount, String bankCode, String payDate) {
        
        try {
            // Update basic transaction info with enhanced validation
            if (currentTransaction != null) {
                if (currentTransaction.getOrder() != null) {
                    orderIdLabel.setText("Order ID: " + currentTransaction.getOrder().getOrderId());
                } else {
                    orderIdLabel.setText("Order ID: Not Available");
                }
                transactionIdLabel.setText("Transaction ID: " + currentTransaction.getTransactionId());
            } else {
                orderIdLabel.setText("Order ID: Not Available");
                transactionIdLabel.setText("Transaction ID: Not Available");
            }
            
            // Format amount (VNPay returns in cents) with error handling
            try {
                if (amount != null && !amount.trim().isEmpty()) {
                    double amountValue = Double.parseDouble(amount) / 100.0;
                    amountLabel.setText(String.format("Amount: %.0f VND", amountValue));
                } else {
                    amountLabel.setText("Amount: Not Available");
                }
            } catch (NumberFormatException e) {
                logger.log(Level.WARNING, "VNPay Return: Invalid amount format: " + amount, e);
                amountLabel.setText("Amount: Invalid Format");
            }
            
            logger.log(Level.INFO, "VNPay Return: Displaying result for response code: " + responseCode);
            
            // Display result based on response code with enhanced user messaging
            switch (responseCode) {
                case "00":
                    showSuccess(transactionNo, bankCode, payDate);
                    break;
                case "07":
                    showPendingPayment("Payment Processing",
                                     "Your payment is being processed by the bank. This may take a few minutes.",
                                     "Please wait and check your payment status again, or contact support if this persists.");
                    break;
                case "09":
                    showRetryableError("Transaction Declined",
                                     "Your bank has declined the transaction.",
                                     "Please try with a different payment method or contact your bank for assistance.");
                    break;
                case "10":
                    showRetryableError("Authentication Failed",
                                     "Card authentication failed during payment.",
                                     "Please verify your card details and try again, or use a different payment method.");
                    break;
                case "11":
                    showRetryableError("Session Expired",
                                     "Your payment session has expired.",
                                     "Please start a new payment to complete your order.");
                    break;
                case "12":
                    showError("Account Issue",
                             "Your account is temporarily locked.",
                             "Please contact your bank to resolve this issue before attempting payment again.");
                    break;
                case "13":
                    showRetryableError("OTP Verification Failed",
                                     "The OTP (One-Time Password) you entered is incorrect.",
                                     "Please try the payment again and enter the correct OTP when prompted.");
                    break;
                case "24":
                    showCancelledPayment("Payment Cancelled",
                                       "You have cancelled the payment process.",
                                       "You can try again or choose a different payment method.");
                    break;
                case "51":
                    showError("Insufficient Funds",
                             "Your account does not have sufficient funds for this transaction.",
                             "Please check your account balance or use a different payment method.");
                    break;
                case "65":
                    showError("Transaction Limit Exceeded",
                             "You have exceeded your daily transaction limit.",
                             "Please try again tomorrow or contact your bank to increase your limit.");
                    break;
                case "75":
                    showRetryableError("Bank Maintenance",
                                     "The payment bank is currently under maintenance.",
                                     "Please try again later or use a different payment method.");
                    break;
                case "79":
                    showError("Amount Mismatch",
                             "The transaction amount does not match the expected value.",
                             "Please contact support to resolve this issue.");
                    break;
                default:
                    showError("Payment Failed",
                             "Payment failed with an unknown error.",
                             "Error Code: " + responseCode + ". Please try again or contact support for assistance.");
                    break;
            }
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "VNPay Return: Error displaying payment result", e);
            showError("Display Error",
                     "Unable to display payment result due to a system error.",
                     "Please contact support with your transaction details.");
        }
    }
    
    /**
     * Show success message
     */
    private void showSuccess(String transactionNo, String bankCode, String payDate) {
        statusLabel.setText("✓ Payment Successful");
        statusLabel.setStyle("-fx-text-fill: green; -fx-font-size: 18px; -fx-font-weight: bold;");
        
        messageLabel.setText(String.format(
            "Your payment has been processed successfully!\n\n" +
            "VNPay Transaction: %s\n" +
            "Bank: %s\n" +
            "Payment Date: %s",
            transactionNo, bankCode, formatPayDate(payDate)
        ));
        
        continueButton.setText("Continue Shopping");
        continueButton.setVisible(true);
        retryButton.setVisible(false);
    }
    
    /**
     * Show error message
     */
    private void showError(String title, String message) {
        showError(title, message, null);
    }
    
    private void showError(String title, String message, String details) {
        statusLabel.setText("✗ " + title);
        statusLabel.setStyle("-fx-text-fill: red; -fx-font-size: 18px; -fx-font-weight: bold;");
        
        String fullMessage = message;
        if (details != null && !details.trim().isEmpty()) {
            fullMessage += "\n\n" + details;
        }
        messageLabel.setText(fullMessage);
        
        continueButton.setText("Back to Cart");
        continueButton.setVisible(true);
        retryButton.setText("Contact Support");
        retryButton.setVisible(true);
    }
    
    private void showRetryableError(String title, String message, String details) {
        statusLabel.setText("⚠ " + title);
        statusLabel.setStyle("-fx-text-fill: orange; -fx-font-size: 18px; -fx-font-weight: bold;");
        
        String fullMessage = message;
        if (details != null && !details.trim().isEmpty()) {
            fullMessage += "\n\n" + details;
        }
        messageLabel.setText(fullMessage);
        
        continueButton.setText("Back to Cart");
        continueButton.setVisible(true);
        retryButton.setText("Try Again");
        retryButton.setVisible(true);
    }
    
    private void showPendingPayment(String title, String message, String details) {
        statusLabel.setText("⏳ " + title);
        statusLabel.setStyle("-fx-text-fill: blue; -fx-font-size: 18px; -fx-font-weight: bold;");
        
        String fullMessage = message;
        if (details != null && !details.trim().isEmpty()) {
            fullMessage += "\n\n" + details;
        }
        messageLabel.setText(fullMessage);
        
        continueButton.setText("Check Status");
        continueButton.setVisible(true);
        retryButton.setText("Contact Support");
        retryButton.setVisible(true);
    }
    
    private void showCancelledPayment(String title, String message, String details) {
        statusLabel.setText("🚫 " + title);
        statusLabel.setStyle("-fx-text-fill: gray; -fx-font-size: 18px; -fx-font-weight: bold;");
        
        String fullMessage = message;
        if (details != null && !details.trim().isEmpty()) {
            fullMessage += "\n\n" + details;
        }
        messageLabel.setText(fullMessage);
        
        continueButton.setText("Back to Cart");
        continueButton.setVisible(true);
        retryButton.setText("Try Different Method");
        retryButton.setVisible(true);
    }
    
    /**
     * Setup initial UI state
     */
    private void setupUI() {
        statusLabel.setText("Processing payment result...");
        messageLabel.setText("Please wait while we verify your payment.");
        continueButton.setVisible(false);
        retryButton.setVisible(false);
    }
    
    /**
     * Format VNPay date string for display
     */
    private String formatPayDate(String vnpPayDate) {
        if (vnpPayDate == null || vnpPayDate.length() != 14) {
            return vnpPayDate;
        }
        
        try {
            // VNPay format: yyyyMMddHHmmss
            String year = vnpPayDate.substring(0, 4);
            String month = vnpPayDate.substring(4, 6);
            String day = vnpPayDate.substring(6, 8);
            String hour = vnpPayDate.substring(8, 10);
            String minute = vnpPayDate.substring(10, 12);
            String second = vnpPayDate.substring(12, 14);
            
            return String.format("%s/%s/%s %s:%s:%s", day, month, year, hour, minute, second);
        } catch (Exception e) {
            return vnpPayDate;
        }
    }
    
    /**
     * Find payment transaction by VNPay transaction reference
     */
    private PaymentTransaction findTransactionByReference(String vnpTxnRef) throws SQLException {
        try {
            // Enhanced validation of transaction reference format
            if (vnpTxnRef == null || !vnpTxnRef.contains("_")) {
                logger.log(Level.WARNING, "VNPay Return: Invalid transaction reference format: " + vnpTxnRef);
                return null;
            }
            
            // Extract order ID from vnpTxnRef (format: orderId_timestamp)
            String orderId = vnpTxnRef.split("_")[0];
            if (orderId.trim().isEmpty()) {
                logger.log(Level.WARNING, "VNPay Return: Empty order ID extracted from reference: " + vnpTxnRef);
                return null;
            }
            
            List<PaymentTransaction> transactions = paymentTransactionDAO.getByOrderId(orderId);
            
            // Return the first transaction for this order (assuming one payment per order)
            PaymentTransaction result = transactions.isEmpty() ? null : transactions.get(0);
            
            if (result != null) {
                logger.log(Level.INFO, "VNPay Return: Found transaction: " + result.getTransactionId() + " for order: " + orderId);
            } else {
                logger.log(Level.WARNING, "VNPay Return: No transaction found for order: " + orderId);
            }
            
            return result;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "VNPay Return: Database error finding transaction for reference: " + vnpTxnRef, e);
            throw e; // Re-throw to be handled by caller
        } catch (Exception e) {
            logger.log(Level.SEVERE, "VNPay Return: Unexpected error finding transaction for reference: " + vnpTxnRef, e);
            return null;
        }
    }
    
    /**
     * Handle continue button click
     */
    @FXML
    private void handleContinue() {
        // Navigate back to main application
        // This should be implemented based on your navigation system
        System.out.println("Continue button clicked - navigate to appropriate screen");
    }
    
    /**
     * Handle retry button click
     */
    @FXML
    private void handleRetry() {
        // Navigate back to payment screen for retry
        // This should be implemented based on your navigation system
        System.out.println("Retry button clicked - navigate back to payment");
    }
}
