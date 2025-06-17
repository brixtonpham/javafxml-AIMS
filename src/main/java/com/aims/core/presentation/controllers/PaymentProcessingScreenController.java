package com.aims.core.presentation.controllers;

import javafx.application.HostServices;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Hyperlink;
import javafx.scene.web.WebView;
import javafx.scene.layout.VBox;

import com.aims.core.application.services.IPaymentService;
import com.aims.core.application.services.IOrderService;
import com.aims.core.entities.OrderEntity;
import com.aims.core.entities.PaymentTransaction;
import com.aims.core.presentation.controllers.base.MainLayoutController;
import com.aims.core.shared.NavigationService;
import com.aims.core.shared.ServiceFactory;
import com.aims.core.shared.exceptions.PaymentException;

public class PaymentProcessingScreenController {

    @FXML private Label statusLabel;
    @FXML private Label instructionLabel;
    @FXML private Label transactionRefLabel;
    @FXML private WebView paymentWebView;
    @FXML private VBox contentVBox;
    @FXML private ProgressIndicator progressIndicator;
    @FXML private Button checkStatusButton;
    @FXML private Button cancelPaymentButton;
    @FXML private Button copyUrlButton;
    @FXML private Hyperlink manualUrlLink;

    private IPaymentService paymentService;
    private IOrderService orderService;
    private MainLayoutController mainLayoutController;
    private OrderEntity currentOrder;
    private String paymentType;
    private String paymentUrl;
    private String transactionRef;
    private HostServices hostServices;
    private boolean useWebView = true;

    public PaymentProcessingScreenController() {
        this.paymentService = ServiceFactory.getPaymentService();
        this.orderService = ServiceFactory.getOrderService();
    }

    public void setMainLayoutController(MainLayoutController controller) {
        this.mainLayoutController = controller;
    }

    public void setHostServices(HostServices hostServices) {
        this.hostServices = hostServices;
    }

    public void setUseWebView(boolean useWebView) {
        this.useWebView = useWebView;
        if (paymentWebView != null) {
            paymentWebView.setVisible(useWebView);
            paymentWebView.setManaged(useWebView);
        }
        if (manualUrlLink != null) {
            manualUrlLink.setVisible(!useWebView);
            manualUrlLink.setManaged(!useWebView);
        }
    }

    public void setPaymentData(OrderEntity order, String transactionId, String paymentUrl) {
        this.currentOrder = order;
        this.transactionRef = transactionId;
        this.paymentUrl = paymentUrl;
        
        if (transactionRefLabel != null) {
            transactionRefLabel.setText("Transaction ID: " + transactionId);
        }
        
        if (paymentUrl != null) {
            if (useWebView && paymentWebView != null) {
                paymentWebView.getEngine().load(paymentUrl);
            } else if (manualUrlLink != null) {
                manualUrlLink.setText("Click here to open payment page");
                manualUrlLink.setOnAction(e -> hostServices.showDocument(paymentUrl));
            }
        }
    }

    public void initPaymentFlow(String orderId, String paymentMethodId) {
        try {
            this.currentOrder = orderService.getOrderById(orderId);
            this.paymentType = paymentMethodId;
            startPaymentProcess();
        } catch (Exception e) {
            handleError("Failed to initialize payment: " + e.getMessage());
        }
    }
    
    public void initPaymentFlow(OrderEntity order, String paymentMethodId) {
        try {
            this.currentOrder = order;
            this.paymentType = paymentMethodId;
            startPaymentProcess();
        } catch (Exception e) {
            handleError("Failed to initialize payment: " + e.getMessage());
        }
    }

    private void startPaymentProcess() {
        showLoading(true);
        statusLabel.setText("Initializing payment...");
        
        try {
            // Use PaymentService to process payment and get transaction data
            PaymentTransaction paymentResult = paymentService.processPayment(currentOrder, paymentType);
            
            if (paymentResult != null) {
                this.transactionRef = paymentResult.getTransactionId();
                
                // Extract payment URL from gateway data if available
                if (paymentResult.getGatewayResponseData() != null) {
                    String paymentUrl = extractPaymentUrlFromGatewayData(paymentResult.getGatewayResponseData());
                    this.paymentUrl = paymentUrl;
                    
                    if (paymentUrl != null) {
                        setPaymentData(currentOrder, transactionRef, paymentUrl);
                        showProcessingStatus();
                        return;
                    }
                }
                
                // If no URL, show direct processing status
                showProcessingStatus();
                
            } else {
                handleError("Failed to initiate payment with gateway");
            }
            
        } catch (PaymentException e) {
            handleError("Payment service error: " + e.getMessage());
        } catch (Exception e) {
            handleError("Payment initiation failed: " + e.getMessage());
        }
    }

    private void showProcessingStatus() {
        statusLabel.setText("Please complete your payment in the opened window");
        instructionLabel.setText("Do not close this window. You will be redirected automatically once payment is complete.");
        showLoading(false);
        checkStatusButton.setVisible(true);
    }

    @FXML
    private void handleCheckStatusAction() {
        showLoading(true);
        statusLabel.setText("Checking payment status...");
        
        try {
            var transaction = paymentService.checkPaymentStatus(transactionRef, null);
            if (transaction != null) {
                NavigationService.navigateToOrderSummary(currentOrder.getOrderId());
            } else {
                showProcessingStatus();
            }
        } catch (Exception e) {
            handleError("Failed to check payment status: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancelPaymentAttemptAction() {
        try {
            cancelPaymentButton.setDisable(true);
            NavigationService.navigateToOrderSummary(currentOrder.getOrderId());
        } catch (Exception e) {
            handleError("Failed to cancel payment: " + e.getMessage());
        }
    }

    @FXML
    private void handleCopyUrlAction() {
        if (paymentUrl != null) {
            // Implement clipboard copy
            java.awt.Toolkit.getDefaultToolkit()
                .getSystemClipboard()
                .setContents(new java.awt.datatransfer.StringSelection(paymentUrl), null);
            
            statusLabel.setText("Payment URL copied to clipboard");
        }
    }

    private void showLoading(boolean show) {
        progressIndicator.setVisible(show);
        progressIndicator.setManaged(show);
    }

    private void handleError(String message) {
        showLoading(false);
        statusLabel.setText("Error");
        instructionLabel.setText(message);
        cancelPaymentButton.setVisible(true);
        checkStatusButton.setVisible(false);
    }
    
    /**
     * Extract payment URL from gateway response data (JSON format)
     */
    private String extractPaymentUrlFromGatewayData(String gatewayResponseData) {
        if (gatewayResponseData == null || gatewayResponseData.trim().isEmpty()) {
            return null;
        }
        
        try {
            // Simple JSON parsing for payment URL
            // In a real application, you might want to use a proper JSON library like Jackson or Gson
            if (gatewayResponseData.contains("\"paymentUrl\"")) {
                int startIndex = gatewayResponseData.indexOf("\"paymentUrl\"") + "\"paymentUrl\"".length();
                startIndex = gatewayResponseData.indexOf("\"", startIndex) + 1;
                int endIndex = gatewayResponseData.indexOf("\"", startIndex);
                
                if (startIndex > 0 && endIndex > startIndex) {
                    return gatewayResponseData.substring(startIndex, endIndex);
                }
            }
            
            // Alternative: check for direct URL pattern (VNPay URLs typically start with https://sandbox.vnpayment.vn)
            if (gatewayResponseData.contains("https://") && gatewayResponseData.contains("vnpayment.vn")) {
                int urlStart = gatewayResponseData.indexOf("https://");
                int urlEnd = gatewayResponseData.indexOf(" ", urlStart);
                if (urlEnd == -1) urlEnd = gatewayResponseData.indexOf("\"", urlStart);
                if (urlEnd == -1) urlEnd = gatewayResponseData.indexOf("}", urlStart);
                if (urlEnd == -1) urlEnd = gatewayResponseData.length();
                
                return gatewayResponseData.substring(urlStart, urlEnd);
            }
            
        } catch (Exception e) {
            System.err.println("Error extracting payment URL from gateway data: " + e.getMessage());
        }
        
        return null;
    }
}