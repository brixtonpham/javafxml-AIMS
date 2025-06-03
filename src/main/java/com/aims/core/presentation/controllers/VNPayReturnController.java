package com.aims.core.presentation.controllers;

import com.aims.core.infrastructure.adapters.external.payment_gateway.IVNPayAdapter;
import com.aims.core.infrastructure.adapters.external.payment_gateway.VNPayAdapterImpl;
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

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;

import java.util.Map;
import java.util.List;
import java.sql.SQLException;

/**
 * VNPay Return Controller
 * Handles user redirects from VNPay after payment completion
 * This displays the payment result to the user
 */
public class VNPayReturnController {
    
    @FXML private Label statusLabel;
    @FXML private Label transactionIdLabel;
    @FXML private Label amountLabel;
    @FXML private Label orderIdLabel;
    @FXML private Label messageLabel;
    @FXML private Button continueButton;
    @FXML private Button retryButton;
    @FXML private VBox contentContainer;
    
    private final IVNPayAdapter vnPayAdapter;
    private final IPaymentTransactionDAO paymentTransactionDAO;
    private PaymentTransaction currentTransaction;
    
    public VNPayReturnController() {
        this.vnPayAdapter = new VNPayAdapterImpl();
        
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
        try {
            // Step 1: Validate signature
            if (!vnPayAdapter.validateResponseSignature(returnParams)) {
                showError("Payment verification failed", "The payment response could not be verified. Please contact support.");
                return;
            }
            
            // Step 2: Extract payment information
            String vnpTxnRef = returnParams.get("vnp_TxnRef");
            String vnpResponseCode = returnParams.get("vnp_ResponseCode");
            String vnpTransactionNo = returnParams.get("vnp_TransactionNo");
            String vnpAmount = returnParams.get("vnp_Amount");
            String vnpBankCode = returnParams.get("vnp_BankCode");
            String vnpPayDate = returnParams.get("vnp_PayDate");
            
            // Step 3: Find the transaction
            currentTransaction = findTransactionByReference(vnpTxnRef);
            if (currentTransaction == null) {
                showError("Transaction not found", "Could not find the payment transaction. Please contact support.");
                return;
            }
            
            // Step 4: Display result based on response code
            displayPaymentResult(vnpResponseCode, vnpTransactionNo, vnpAmount, vnpBankCode, vnpPayDate);
            
        } catch (Exception e) {
            System.err.println("Error processing VNPay return: " + e.getMessage());
            e.printStackTrace();
            showError("Processing Error", "An error occurred while processing the payment result.");
        }
    }
    
    /**
     * Display payment result to user
     */
    private void displayPaymentResult(String responseCode, String transactionNo, 
                                    String amount, String bankCode, String payDate) {
        
        // Update basic transaction info
        if (currentTransaction != null) {
            orderIdLabel.setText("Order ID: " + currentTransaction.getOrder().getOrderId());
            transactionIdLabel.setText("Transaction ID: " + currentTransaction.getTransactionId());
        }
        
        // Format amount (VNPay returns in cents)
        double amountValue = Double.parseDouble(amount) / 100.0;
        amountLabel.setText(String.format("Amount: %.0f VND", amountValue));
        
        // Display result based on response code
        switch (responseCode) {
            case "00":
                showSuccess(transactionNo, bankCode, payDate);
                break;
            case "07":
                showError("Payment Pending", "Your payment is being processed. Please wait a few minutes and check again.");
                break;
            case "09":
                showError("Transaction Failed", "Your bank has declined the transaction. Please try with a different card or contact your bank.");
                break;
            case "10":
                showError("Authentication Failed", "Card authentication failed. Please verify your card details and try again.");
                break;
            case "11":
                showError("Transaction Timeout", "The transaction has expired. Please try again.");
                break;
            case "12":
                showError("Account Locked", "Your account is temporarily locked. Please contact your bank.");
                break;
            case "13":
                showError("Invalid OTP", "The OTP you entered is incorrect. Please try again.");
                break;
            case "24":
                showError("Transaction Canceled", "You have canceled the transaction.");
                break;
            case "51":
                showError("Insufficient Funds", "Your account does not have sufficient funds for this transaction.");
                break;
            case "65":
                showError("Transaction Limit Exceeded", "You have exceeded your daily transaction limit.");
                break;
            case "75":
                showError("Payment Bank Maintenance", "The payment bank is under maintenance. Please try again later.");
                break;
            case "79":
                showError("Incorrect Transaction Amount", "The transaction amount is incorrect. Please try again.");
                break;
            default:
                showError("Payment Failed", "Payment failed with code: " + responseCode + ". Please try again or contact support.");
                break;
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
        statusLabel.setText("✗ " + title);
        statusLabel.setStyle("-fx-text-fill: red; -fx-font-size: 18px; -fx-font-weight: bold;");
        
        messageLabel.setText(message);
        
        continueButton.setText("Back to Cart");
        continueButton.setVisible(true);
        retryButton.setText("Try Again");
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
    private PaymentTransaction findTransactionByReference(String vnpTxnRef) {
        try {
            // Extract order ID from vnpTxnRef (format: orderId_timestamp)
            String orderId = vnpTxnRef.split("_")[0];
            List<PaymentTransaction> transactions = paymentTransactionDAO.getByOrderId(orderId);
            // Return the first transaction for this order (assuming one payment per order)
            return transactions.isEmpty() ? null : transactions.get(0);
        } catch (SQLException e) {
            System.err.println("Error finding transaction: " + e.getMessage());
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
