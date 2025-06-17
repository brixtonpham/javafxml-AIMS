package com.aims.core.presentation.controllers;

import java.util.Map;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import com.aims.core.application.services.IPaymentService;
import com.aims.core.application.services.IOrderService;
import com.aims.core.entities.OrderEntity;
import com.aims.core.entities.PaymentTransaction;
import com.aims.core.shared.NavigationService;
import com.aims.core.shared.ServiceFactory;
import com.aims.core.infrastructure.adapters.external.payment_gateway.IPaymentGatewayAdapter;

public class PaymentReturnController {

    @FXML private Label headerLabel;
    @FXML private Label statusLabel;
    @FXML private Label amountLabel;
    @FXML private Label orderIdLabel;
    @FXML private Label transactionIdLabel;
    @FXML private Label paymentMethodLabel;
    @FXML private Label messageLabel;
    @FXML private VBox errorDetailsBox;
    @FXML private Label errorCodeLabel;
    @FXML private Label errorMessageLabel;
    @FXML private Button retryButton;
    @FXML private Button continueButton;
    @FXML private Button viewOrderButton;

    private IPaymentService paymentService;
    private IOrderService orderService;
    private IPaymentGatewayAdapter gatewayAdapter;
    private Map<String, String> returnParams;
    private String orderId;

    public PaymentReturnController() {
        this.paymentService = ServiceFactory.getPaymentService();
        this.orderService = ServiceFactory.getOrderService();
        this.gatewayAdapter = ServiceFactory.getPaymentGatewayAdapter();
    }

    public void initialize(Map<String, String> returnParams) {
        this.returnParams = returnParams;
        this.orderId = returnParams.get("orderId");
        
        if (!gatewayAdapter.validateResponseSignature(returnParams)) {
            showError("Payment verification failed", "The payment response signature is invalid. Please contact support.");
            return;
        }

        processPaymentReturn();
    }

    private void processPaymentReturn() {
        try {
            OrderEntity order = orderService.getOrderById(orderId);
            if (order == null) {
                showError("Order not found", "Unable to find order details. Please contact support.");
                return;
            }

            // Get the standardized payment status from gateway-specific response code
            String responseCode = returnParams.get("responseCode");
            String paymentStatus = gatewayAdapter.mapResponseCodeToStatus(responseCode);

            PaymentTransaction transaction = paymentService.findTransactionByExternalId(
                returnParams.get("transactionId")
            );

            updateUIWithPaymentResult(order, transaction, paymentStatus);

        } catch (Exception e) {
            showError("Error processing payment result", e.getMessage());
        }
    }

    private void updateUIWithPaymentResult(OrderEntity order, PaymentTransaction transaction, String paymentStatus) {
        orderIdLabel.setText("Order ID: " + order.getOrderId());
        amountLabel.setText(String.format("Amount: %.2f VND", order.getTotalAmountPaid()));
        
        if (transaction != null) {
            transactionIdLabel.setText("Transaction ID: " + transaction.getTransactionId());
            paymentMethodLabel.setText("Payment Method: " + transaction.getPaymentMethod().getMethodType());
        }

        switch (paymentStatus) {
            case "SUCCESS":
                showSuccess();
                break;
            case "PENDING":
                showPending();
                break;
            case "CANCELLED":
                showCancelled();
                break;
            default:
                showFailed();
        }
    }

    private void showSuccess() {
        headerLabel.setText("Payment Successful");
        statusLabel.setText("Your payment has been processed successfully!");
        messageLabel.setText("Thank you for your purchase. Your order will be processed shortly.");
        styleSuccess();
    }

    private void showPending() {
        headerLabel.setText("Payment Pending");
        statusLabel.setText("Your payment is being processed.");
        messageLabel.setText("Please wait while we confirm your payment with the bank.");
        stylePending();
        retryButton.setVisible(true);
    }

    private void showCancelled() {
        headerLabel.setText("Payment Cancelled");
        statusLabel.setText("Your payment was cancelled.");
        messageLabel.setText("You can try the payment again or choose a different payment method.");
        styleCancelled();
        retryButton.setVisible(true);
    }

    private void showFailed() {
        headerLabel.setText("Payment Failed");
        statusLabel.setText("Your payment could not be processed.");
        messageLabel.setText("Please try again or choose a different payment method.");
        styleFailed();
        retryButton.setVisible(true);
        
        // Show error details if available
        String errorCode = returnParams.get("errorCode");
        String errorMessage = returnParams.get("message");
        if (errorCode != null || errorMessage != null) {
            errorDetailsBox.setVisible(true);
            errorCodeLabel.setText("Error Code: " + errorCode);
            errorMessageLabel.setText("Error Message: " + errorMessage);
        }
    }

    private void showError(String title, String message) {
        headerLabel.setText(title);
        statusLabel.setText("An error occurred");
        messageLabel.setText(message);
        styleFailed();
        retryButton.setVisible(true);
    }

    // Style methods using CSS classes
    private void styleSuccess() {
        statusLabel.getStyleClass().setAll("status-label", "success");
    }

    private void stylePending() {
        statusLabel.getStyleClass().setAll("status-label", "pending");
    }

    private void styleCancelled() {
        statusLabel.getStyleClass().setAll("status-label", "cancelled");
    }

    private void styleFailed() {
        statusLabel.getStyleClass().setAll("status-label", "failed");
    }

    // Event handlers
    @FXML
    private void handleRetryPayment() {
        NavigationService.navigateToPaymentMethod(orderId);
    }

    @FXML
    private void handleContinueShopping() {
        NavigationService.navigateToHome();
    }

    @FXML
    private void handleViewOrder() {
        NavigationService.navigateToOrderDetails(orderId);
    }
}