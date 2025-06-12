package com.aims.core.presentation.controllers;

import com.aims.core.application.services.IPaymentService;
import com.aims.core.application.services.IOrderService; // To update order status if payment cancelled here
import com.aims.core.entities.OrderEntity;
import com.aims.core.entities.PaymentTransaction;
import com.aims.core.enums.OrderStatus;
import com.aims.core.infrastructure.webserver.VNPayCallbackServer;
import com.aims.core.infrastructure.adapters.external.payment_gateway.IVNPayAdapter;
import com.aims.core.infrastructure.adapters.external.payment_gateway.VNPayAdapterImpl;
import com.aims.core.presentation.utils.PaymentErrorHandler;
// import com.aims.presentation.utils.AlertHelper;
// import com.aims.presentation.utils.FXMLSceneManager;
import com.aims.core.shared.exceptions.PaymentException;
import com.aims.core.shared.exceptions.ValidationException;
import com.aims.core.shared.exceptions.ResourceNotFoundException;

import javafx.application.Platform;
import javafx.application.HostServices;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;

import java.util.HashMap;
import java.util.Map;
import java.awt.Desktop;
import java.net.URI;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Hyperlink;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView; // If using WebView
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.WorkerStateEvent;

import java.sql.SQLException;
import java.util.Map; // For payment result

public class PaymentProcessingScreenController {

    @FXML
    private ProgressIndicator progressIndicator;
    @FXML
    private Label statusLabel;
    @FXML
    private Label instructionLabel;
    @FXML
    private Label transactionRefLabel;
    @FXML
    private WebView vnPayWebView; // Optional, if embedding
    @FXML
    private Button checkStatusButton;
    @FXML
    private Button cancelPaymentButton;
    @FXML
    private Button copyUrlButton;
    @FXML
    private Hyperlink manualUrlLink;
    @FXML
    private VBox contentVBox;


    // @Inject
    private IPaymentService paymentService;
    // @Inject
    private IOrderService orderService; // To update order if payment attempt is cancelled here
    private MainLayoutController mainLayoutController;
    private com.aims.core.presentation.utils.FXMLSceneManager sceneManager;
    private HostServices hostServices; // For launching external browser

    private OrderEntity currentOrder;
    private String aimsTransactionId; // The AIMS internal transaction ID
    private String vnpayPaymentUrl; // URL from IPaymentService to load in WebView or open in browser

    private Task<PaymentTransaction> paymentStatusCheckTask;
    private boolean useWebView = false; // Toggle between WebView and external browser
    
    // VNPay callback handling
    private VNPayCallbackServer callbackServer;
    private IVNPayAdapter vnPayAdapter;

    public PaymentProcessingScreenController() {
        // paymentService = new PaymentServiceImpl(...); // DI
        // orderService = new OrderServiceImpl(...);   // DI
        this.vnPayAdapter = new VNPayAdapterImpl();
        this.callbackServer = new VNPayCallbackServer();
        setupCallbackServer();
    }

    public void setMainLayoutController(MainLayoutController mainLayoutController) {
        this.mainLayoutController = mainLayoutController;
        System.out.println("PaymentProcessingScreenController.setMainLayoutController: MainLayoutController injected successfully");
    }
    
    public void setSceneManager(com.aims.core.presentation.utils.FXMLSceneManager sceneManager) {
        this.sceneManager = sceneManager;
        System.out.println("PaymentProcessingScreenController.setSceneManager: SceneManager injected successfully");
    }
    
    public void setPaymentService(IPaymentService paymentService) {
        this.paymentService = paymentService;
        System.out.println("PaymentProcessingScreenController.setPaymentService: PaymentService injected successfully - Available: " + (paymentService != null));
        
        // Update callback server with payment service
        if (callbackServer != null) {
            callbackServer.setPaymentService(paymentService);
        }
    }
    
    public void setOrderService(IOrderService orderService) {
        this.orderService = orderService;
        System.out.println("PaymentProcessingScreenController.setOrderService: OrderService injected successfully - Available: " + (orderService != null));
    }

    public void setHostServices(HostServices hostServices) {
        this.hostServices = hostServices;
        System.out.println("PaymentProcessingScreenController.setHostServices: HostServices injected successfully - Available: " + (hostServices != null));
    }
    
    /**
     * Toggle between WebView and external browser mode
     * @param useWebView true to use embedded WebView, false to use external browser
     */
    public void setUseWebView(boolean useWebView) {
        this.useWebView = useWebView;
        System.out.println("PaymentProcessingScreenController: Browser mode set to " + (useWebView ? "embedded WebView" : "external browser"));
    }

    public void initialize() {
        // Hide WebView initially if it's an option
        if (vnPayWebView != null) {
            vnPayWebView.setVisible(false);
            vnPayWebView.setManaged(false);
        }
        
        // Hide manual URL controls initially
        if (copyUrlButton != null) {
            copyUrlButton.setVisible(false);
            copyUrlButton.setManaged(false);
        }
        if (manualUrlLink != null) {
            manualUrlLink.setVisible(false);
            manualUrlLink.setManaged(false);
        }
    }
    
    /**
     * Setup VNPay callback server for handling payment responses
     */
    private void setupCallbackServer() {
        if (callbackServer != null) {
            // Set callback handler to process VNPay responses
            callbackServer.setCallbackHandler(this::handleVNPayCallback);
            
            // Set payment service if available
            if (paymentService != null) {
                callbackServer.setPaymentService(paymentService);
            }
            
            System.out.println("PaymentProcessingScreenController: VNPay callback server configured");
        }
    }
    
    /**
     * Start callback server when payment processing begins
     */
    private void startCallbackServer() {
        if (callbackServer != null && !callbackServer.isRunning()) {
            try {
                callbackServer.start();
                System.out.println("PaymentProcessingScreenController: VNPay callback server started successfully");
            } catch (Exception e) {
                System.err.println("PaymentProcessingScreenController: Failed to start callback server: " + e.getMessage());
            }
        }
    }
    
    /**
     * Stop callback server when controller is disposed
     */
    private void stopCallbackServer() {
        if (callbackServer != null && callbackServer.isRunning()) {
            callbackServer.stop();
            System.out.println("PaymentProcessingScreenController: VNPay callback server stopped");
        }
    }

    /**
     * Called from PaymentMethodScreenController to pass transaction data
     */
    public void setTransactionData(OrderEntity order, String aimsTransactionId) {
        setPaymentData(order, aimsTransactionId, null);
    }
    
    /**
     * Called from PaymentMethodScreenController or IPaymentService implementation
     * to pass data and the VNPay URL if applicable.
     */
    public void setPaymentData(OrderEntity order, String aimsTransactionId, String vnpayUrl) {
        this.currentOrder = order;
        this.aimsTransactionId = aimsTransactionId;
        this.vnpayPaymentUrl = vnpayUrl;

        transactionRefLabel.setText("Your AIMS Transaction ID: " + (this.aimsTransactionId != null ? this.aimsTransactionId : "N/A"));

        // Start callback server to handle VNPay responses
        startCallbackServer();

        if (this.vnpayPaymentUrl != null && !this.vnpayPaymentUrl.isEmpty()) {
            System.out.println("PaymentProcessingScreenController: Payment URL received, initiating browser integration");
            
            if (useWebView && vnPayWebView != null) {
                // Option 1: Embedded WebView
                setupWebViewAndLoad();
            } else {
                // Option 2: External browser launch (recommended)
                launchExternalBrowser();
                showExternalBrowserInterface();
            }
        } else {
            // This case might occur if payment is processed directly without redirect
            // or if there was an error generating the URL.
            statusLabel.setText("Preparing to process your payment directly...");
            System.out.println("PaymentProcessingScreenController: No payment URL provided, using direct processing mode");
            // Potentially start polling or wait for a different kind of callback
        }
    }

    /**
     * Launch payment URL in external browser with fallback mechanisms
     */
    private void launchExternalBrowser() {
        boolean browserLaunched = false;
        
        try {
            // Primary method: HostServices (JavaFX Application)
            if (hostServices != null) {
                System.out.println("PaymentProcessingScreenController: Attempting to launch browser using HostServices");
                hostServices.showDocument(this.vnpayPaymentUrl);
                browserLaunched = true;
                System.out.println("PaymentProcessingScreenController: Browser launched successfully using HostServices");
            }
        } catch (Exception e) {
            System.err.println("PaymentProcessingScreenController: HostServices failed: " + e.getMessage());
            
            // Handle browser launch error with centralized error handling
            PaymentErrorHandler.handlePaymentProcessingError(e, this, currentOrder, aimsTransactionId);
        }
        
        if (!browserLaunched) {
            try {
                // Fallback method: Desktop.browse() for systems without HostServices
                System.out.println("PaymentProcessingScreenController: Attempting fallback browser launch using Desktop.browse()");
                if (Desktop.isDesktopSupported()) {
                    Desktop desktop = Desktop.getDesktop();
                    if (desktop.isSupported(Desktop.Action.BROWSE)) {
                        desktop.browse(new URI(this.vnpayPaymentUrl));
                        browserLaunched = true;
                        System.out.println("PaymentProcessingScreenController: Browser launched successfully using Desktop.browse()");
                    }
                }
            } catch (Exception e) {
                System.err.println("PaymentProcessingScreenController: Desktop.browse() failed: " + e.getMessage());
                
                // Handle fallback browser launch error
                PaymentErrorHandler.handlePaymentProcessingError(e, this, currentOrder, aimsTransactionId);
            }
        }
        
        if (!browserLaunched) {
            System.err.println("PaymentProcessingScreenController: All browser launch methods failed - showing manual options");
            
            // Create a browser launch exception for centralized handling
            Exception browserException = new Exception("All browser launch methods failed. Unable to open payment window.");
            PaymentErrorHandler.handlePaymentProcessingError(browserException, this, currentOrder, aimsTransactionId);
            
            // Still show manual options as fallback
            showManualUrlOptions();
        }
    }
    
    /**
     * Show interface for external browser workflow
     */
    private void showExternalBrowserInterface() {
        statusLabel.setText("✓ Payment page opened in your browser");
        instructionLabel.setText("Complete your payment on the VNPay page that just opened in your browser.\nAfter payment, return here to check your payment status.");
        
        // Show manual check button
        checkStatusButton.setVisible(true);
        checkStatusButton.setManaged(true);
        checkStatusButton.setText("Check Payment Status");
        
        // Hide progress indicator since user interaction is in external browser
        progressIndicator.setVisible(false);
    }
    
    /**
     * Show manual URL options when browser launch fails
     */
    private void showManualUrlOptions() {
        statusLabel.setText("⚠ Unable to open browser automatically");
        instructionLabel.setText("Please manually copy and open the payment URL in your browser:");
        
        // Show manual URL controls
        if (manualUrlLink != null) {
            manualUrlLink.setText(this.vnpayPaymentUrl);
            manualUrlLink.setVisible(true);
            manualUrlLink.setManaged(true);
            manualUrlLink.setOnAction(e -> {
                // Try to launch browser when user clicks the link
                launchExternalBrowser();
            });
        }
        
        if (copyUrlButton != null) {
            copyUrlButton.setVisible(true);
            copyUrlButton.setManaged(true);
        }
        
        checkStatusButton.setVisible(true);
        checkStatusButton.setManaged(true);
        
        progressIndicator.setVisible(false);
    }
    
    /**
     * Setup and load WebView for embedded payment experience
     */
    private void setupWebViewAndLoad() {
        if (vnPayWebView == null) {
            System.err.println("PaymentProcessingScreenController: WebView component not available");
            // Fallback to external browser
            launchExternalBrowser();
            showExternalBrowserInterface();
            return;
        }
        
        System.out.println("PaymentProcessingScreenController: Setting up embedded WebView");
        
        // Hide loading components and show WebView
        contentVBox.getChildren().removeAll(progressIndicator, statusLabel, instructionLabel, checkStatusButton, cancelPaymentButton);
        vnPayWebView.setVisible(true);
        vnPayWebView.setManaged(true);
        
        WebEngine webEngine = vnPayWebView.getEngine();
        
        // Setup URL change listener to detect VNPay return
        webEngine.locationProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                System.out.println("PaymentProcessingScreenController: WebView location changed: " + newValue);
                if (newValue != null && isReturnUrl(newValue)) {
                    System.out.println("PaymentProcessingScreenController: VNPay return URL detected, processing callback");
                    vnPayWebView.setVisible(false);
                    vnPayWebView.setManaged(false);
                    contentVBox.getChildren().setAll(progressIndicator, statusLabel, instructionLabel, checkStatusButton, cancelPaymentButton);
                    statusLabel.setText("Processing payment result from VNPay...");
                    
                    // Parse and handle VNPay callback
                    Map<String, String> params = parseQueryParams(newValue);
                    handleVNPayCallback(params);
                }
            }
        });
        
        // Setup loading state listener
        webEngine.getLoadWorker().stateProperty().addListener((observable, oldState, newState) -> {
            Platform.runLater(() -> {
                switch (newState) {
                    case RUNNING:
                        statusLabel.setText("Loading VNPay payment page...");
                        progressIndicator.setVisible(true);
                        break;
                    case SUCCEEDED:
                        statusLabel.setText("VNPay payment page loaded successfully");
                        progressIndicator.setVisible(false);
                        break;
                    case FAILED:
                        statusLabel.setText("Failed to load VNPay payment page");
                        progressIndicator.setVisible(false);
                        System.err.println("PaymentProcessingScreenController: WebView failed to load: " + webEngine.getLoadWorker().getException());
                        // Fallback to external browser
                        launchExternalBrowser();
                        showExternalBrowserInterface();
                        break;
                }
            });
        });
        
        // Load the payment URL
        webEngine.load(this.vnpayPaymentUrl);
        System.out.println("PaymentProcessingScreenController: WebView loading payment URL: " + this.vnpayPaymentUrl);
    }
    
    /**
     * Check if URL is a VNPay return URL
     */
    private boolean isReturnUrl(String url) {
        // This should match your VNPay return URL configuration
        // Common patterns: contains "return", "callback", or matches your configured return URL
        return url != null && (
            url.contains("/vnpay/return") || 
            url.contains("/payment/return") ||
            url.contains("vnp_ResponseCode") ||
            url.startsWith("http://localhost:8080/vnpay/return") // Adjust based on your config
        );
    }
    
    /**
     * Parse query parameters from URL
     */
    private Map<String, String> parseQueryParams(String url) {
        Map<String, String> params = new HashMap<>();
        try {
            URI uri = new URI(url);
            String query = uri.getQuery();
            if (query != null) {
                String[] pairs = query.split("&");
                for (String pair : pairs) {
                    String[] keyValue = pair.split("=", 2);
                    if (keyValue.length == 2) {
                        params.put(keyValue[0], java.net.URLDecoder.decode(keyValue[1], "UTF-8"));
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("PaymentProcessingScreenController: Error parsing URL parameters: " + e.getMessage());
        }
        return params;
    }
    
    /**
     * Enhanced VNPay callback handler with signature validation and transaction status updates
     */
    private void handleVNPayCallback(Map<String, String> params) {
        System.out.println("PaymentProcessingScreenController: Processing VNPay callback with " + params.size() + " parameters");
        
        try {
            // Step 1: Validate signature using VNPay adapter
            if (!vnPayAdapter.validateResponseSignature(params)) {
                System.err.println("PaymentProcessingScreenController: Invalid signature in VNPay response");
                
                // Handle signature validation failure with centralized error handling
                PaymentErrorHandler.handleSignatureValidationFailure(params.get("vnp_TxnRef"), params);
                
                navigateToPaymentResultScreen(false, "Payment verification failed - invalid signature", params);
                return;
            }
            
            // Step 2: Extract key parameters
            String responseCode = params.get("vnp_ResponseCode");
            String transactionNo = params.get("vnp_TransactionNo");
            String vnpTxnRef = params.get("vnp_TxnRef");
            String message = params.get("vnp_Message");
            
            System.out.println("PaymentProcessingScreenController: VNPay callback - Code: " + responseCode +
                             ", TxnRef: " + vnpTxnRef + ", TransactionNo: " + transactionNo);
            
            // Step 3: Update transaction status if payment service is available
            if (paymentService != null && vnpTxnRef != null) {
                try {
                    // Convert params to JSON for gateway response data
                    String gatewayResponseData = convertParamsToJson(params);
                    
                    PaymentTransaction updatedTransaction = paymentService.updateTransactionStatusFromCallback(
                        vnpTxnRef, responseCode, transactionNo, gatewayResponseData
                    );
                    
                    System.out.println("PaymentProcessingScreenController: Transaction status updated successfully: " +
                                     updatedTransaction.getTransactionStatus());
                    
                } catch (Exception e) {
                    System.err.println("PaymentProcessingScreenController: Error updating transaction status: " + e.getMessage());
                    e.printStackTrace();
                    // Continue with navigation even if status update fails
                }
            }
            
            // Step 4: Determine result and navigate
            boolean isSuccess = "00".equals(responseCode);
            String resultMessage = getResultMessage(responseCode, message, isSuccess);
            
            System.out.println("PaymentProcessingScreenController: VNPay callback result - Success: " + isSuccess + ", Code: " + responseCode);
            
            // Step 5: Navigate to payment result screen
            navigateToPaymentResultScreen(isSuccess, resultMessage, params);
            
        } catch (Exception e) {
            System.err.println("PaymentProcessingScreenController: Error processing VNPay callback: " + e.getMessage());
            e.printStackTrace();
            
            // Handle callback processing error with centralized error handling
            PaymentErrorHandler.handleCallbackValidationError(e, params.get("vnp_TxnRef"), params);
            
            navigateToPaymentResultScreen(false, "Error processing payment result: " + e.getMessage(), params);
        }
    }
    
    /**
     * Convert parameters map to JSON string for storage
     */
    private String convertParamsToJson(Map<String, String> params) {
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!first) json.append(",");
            json.append("\"").append(entry.getKey()).append("\":\"").append(entry.getValue()).append("\"");
            first = false;
        }
        json.append("}");
        return json.toString();
    }
    
    /**
     * Get user-friendly result message based on VNPay response
     */
    private String getResultMessage(String responseCode, String vnpMessage, boolean isSuccess) {
        if (isSuccess) {
            return "Payment completed successfully!";
        }
        
        // Map specific error codes to user-friendly messages
        switch (responseCode) {
            case "07":
                return "Payment is being processed. Please wait a few minutes.";
            case "09":
                return "Your bank has declined the transaction. Please try with a different card.";
            case "10":
                return "Card authentication failed. Please verify your card details.";
            case "11":
                return "Transaction has expired. Please try again.";
            case "12":
                return "Account is temporarily locked. Please contact your bank.";
            case "13":
                return "Invalid OTP entered. Please try again.";
            case "24":
                return "Transaction was cancelled by user.";
            case "51":
                return "Insufficient funds in account.";
            case "65":
                return "Daily transaction limit exceeded.";
            case "75":
                return "Payment bank is under maintenance. Please try again later.";
            case "79":
                return "Transaction amount is incorrect.";
            default:
                return "Payment failed: " + (vnpMessage != null ? vnpMessage : "Code " + responseCode);
        }
    }

    @FXML
    void handleCopyUrlAction(ActionEvent event) {
        try {
            // Copy URL to system clipboard
            javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
            javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
            content.putString(this.vnpayPaymentUrl);
            clipboard.setContent(content);
            
            // Update button text to show success
            copyUrlButton.setText("✓ URL Copied!");
            
            // Reset button text after 2 seconds
            Platform.runLater(() -> {
                new Thread(() -> {
                    try {
                        Thread.sleep(2000);
                        Platform.runLater(() -> copyUrlButton.setText("Copy Payment URL"));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }).start();
            });
            
            System.out.println("PaymentProcessingScreenController: Payment URL copied to clipboard");
        } catch (Exception e) {
            System.err.println("PaymentProcessingScreenController: Error copying URL to clipboard: " + e.getMessage());
            copyUrlButton.setText("Copy Failed");
        }
    }


    @FXML
    void handleCheckStatusAction(ActionEvent event) {
        if (aimsTransactionId == null || paymentService == null) {
            // Create validation exception for missing dependencies
            Exception missingDataException = new ValidationException("Cannot check payment status. Required information is missing.");
            PaymentErrorHandler.handlePaymentProcessingError(missingDataException, this, currentOrder, aimsTransactionId);
            return;
        }
        statusLabel.setText("Checking payment status with VNPay, please wait...");
        progressIndicator.setVisible(true);
        checkStatusButton.setDisable(true);
        cancelPaymentButton.setDisable(true);

        paymentStatusCheckTask = new Task<>() {
            @Override
            protected PaymentTransaction call() throws Exception {
                // In a real app, use the externalTransactionId from the PaymentTransaction if known
                return paymentService.checkPaymentStatus(aimsTransactionId, null); // Pass external ID if available
            }

            @Override
            protected void succeeded() {
                super.succeeded();
                progressIndicator.setVisible(false);
                checkStatusButton.setDisable(false);
                cancelPaymentButton.setDisable(false);
                PaymentTransaction result = getValue();
                
                // Check payment status and navigate accordingly
                boolean isSuccess = "SUCCESS".equals(result.getTransactionStatus());
                String message = isSuccess ? "Payment successful!" : "Payment status: " + result.getTransactionStatus();
                
                System.out.println("Status check result: " + result.getTransactionStatus());
                
                // Convert PaymentTransaction to Map for compatibility with existing navigation method
                Map<String, String> resultMap = new HashMap<>();
                resultMap.put("transactionId", result.getTransactionId());
                resultMap.put("status", result.getTransactionStatus());
                resultMap.put("externalTransactionId", result.getExternalTransactionId());
                resultMap.put("amount", String.valueOf(result.getAmount()));
                
                navigateToPaymentResultScreen(isSuccess, message, resultMap);
            }

            @Override
            protected void failed() {
                super.failed();
                progressIndicator.setVisible(false);
                checkStatusButton.setDisable(false);
                cancelPaymentButton.setDisable(false);
                Throwable cause = getException();
                
                // Handle status check failure with centralized error handling
                Exception statusException = cause instanceof Exception ? (Exception) cause : new Exception(cause);
                PaymentErrorHandler.handlePaymentProcessingError(statusException, PaymentProcessingScreenController.this, currentOrder, aimsTransactionId);
                
                statusLabel.setText("Failed to retrieve payment status: " + cause.getMessage());
                cause.printStackTrace();
            }
        };
        new Thread(paymentStatusCheckTask).start();
    }

    @FXML
    void handleCancelPaymentAttemptAction(ActionEvent event) {
        System.out.println("Payment attempt cancelled by user for order: " + (currentOrder != null ? currentOrder.getOrderId() : "N/A"));
        if (paymentStatusCheckTask != null && paymentStatusCheckTask.isRunning()) {
            paymentStatusCheckTask.cancel(true);
        }
        
        // Stop callback server when cancelling payment
        stopCallbackServer();
        
        // Navigate back to payment method screen so customer can try again
        if (mainLayoutController != null) {
            try {
                Object controller = mainLayoutController.loadContent("/com/aims/presentation/views/payment_method_screen.fxml");
                mainLayoutController.setHeaderTitle("Select Payment Method");
                
                // Pass order data back to payment method controller
                if (controller instanceof PaymentMethodScreenController && currentOrder != null) {
                    ((PaymentMethodScreenController) controller).setOrderData(currentOrder);
                }
                
                System.out.println("Successfully navigated back to payment method selection after cancellation");
            } catch (Exception e) {
                System.err.println("Error navigating back to payment method: " + e.getMessage());
                // Fallback: Navigate to order summary as alternative
                try {
                    Object summaryController = mainLayoutController.loadContent("/com/aims/presentation/views/order_summary_screen.fxml");
                    mainLayoutController.setHeaderTitle("Order Summary & Confirmation");
                    
                    if (summaryController instanceof OrderSummaryController && currentOrder != null) {
                        ((OrderSummaryController) summaryController).setOrderData(currentOrder);
                    }
                    
                    System.out.println("Fallback navigation to order summary successful");
                } catch (Exception fallbackException) {
                    System.err.println("Fallback navigation also failed: " + fallbackException.getMessage());
                    statusLabel.setText("Payment cancelled. Please refresh to try again.");
                }
            }
        } else {
            System.err.println("MainLayoutController not available for cancel navigation");
            statusLabel.setText("Payment cancelled. Please refresh to try again.");
        }
        
        progressIndicator.setVisible(false);
        checkStatusButton.setVisible(false);
    }

    private void navigateToPaymentResultScreen(boolean success, String message, Map<String, String> gatewayData) {
        System.out.println("Navigating to Payment Result Screen. Success: " + success + ", Message: " + message);
        
        // Stop callback server when navigating to result screen
        stopCallbackServer();
        
        if (mainLayoutController != null) {
            try {
                Object controller = mainLayoutController.loadContent("/com/aims/presentation/views/payment_result_screen.fxml");
                mainLayoutController.setHeaderTitle(success ? "Payment Successful" : "Payment Failed");
                
                // Pass payment result data to the result controller
                if (controller instanceof PaymentResultScreenController) {
                    ((PaymentResultScreenController) controller).setPaymentResult(currentOrder, success, message, gatewayData, aimsTransactionId);
                }
                
                System.out.println("Successfully navigated to payment result screen");
            } catch (Exception e) {
                System.err.println("Error navigating to payment result screen: " + e.getMessage());
                e.printStackTrace();
                
                // Fallback: Show error message on current screen
                statusLabel.setText("Navigation error occurred. " + (success ? "Payment was successful" : "Payment failed") + ": " + message);
                progressIndicator.setVisible(false);
                checkStatusButton.setVisible(true);
                cancelPaymentButton.setVisible(false);
            }
        } else {
            System.err.println("MainLayoutController not available for navigation to payment result");
            statusLabel.setText("System error: Cannot navigate to result screen. " + (success ? "Payment was successful" : "Payment failed"));
        }
    }
    
    /**
     * Cleanup method to be called when the controller is disposed or screen is changed
     * This ensures the VNPay callback server is properly stopped
     */
    public void cleanup() {
        System.out.println("PaymentProcessingScreenController: Performing cleanup");
        
        // Stop callback server
        stopCallbackServer();
        
        // Cancel any running payment status check tasks
        if (paymentStatusCheckTask != null && paymentStatusCheckTask.isRunning()) {
            paymentStatusCheckTask.cancel(true);
            System.out.println("PaymentProcessingScreenController: Cancelled running payment status check task");
        }
    }

    // private void handleVNPayCallback(Map<String, String> params) {
    //     if (paymentService == null) return;
    //
    //     boolean isValidSignature = vnPayAdapter.validateResponseSignature(params); // Assuming adapter is accessible
    //     if (!isValidSignature) {
    //         AlertHelper.showErrorAlert("Security Warning", "Payment response signature is invalid. Please contact support.");
    //         navigateToPaymentResultScreen(false, "Invalid payment response from gateway.", params);
    //         return;
    //     }
    //
    //     String responseCode = params.get("vnp_ResponseCode");
    //     String transactionNoVNP = params.get("vnp_TransactionNo");
    //     String aimsTxnRef = params.get("vnp_TxnRef"); // This should match our orderId_timestamp
    //
    //     // Update local PaymentTransaction using aimsTxnRef or a mapping
    //     // For now, we assume aimsTransactionId from setPaymentData is the one to update
    //
    //     try {
    //         if ("00".equals(responseCode)) { // Successful payment
    //             paymentService.updateLocalTransactionStatus(aimsTransactionId, "SUCCESS", transactionNoVNP, "Payment completed via VNPay callback.");
    //             // This is where the full order processing logic (stock update, clear cart, notifications) via OrderService would be finalized.
    //             // OrderService.finalizeSuccessfulOrder(currentOrder.getOrderId(), aimsTransactionId, transactionNoVNP_from_callback);
    //             navigateToPaymentResultScreen(true, "Payment successfully processed by VNPay.", params);
    //         } else {
    //             paymentService.updateLocalTransactionStatus(aimsTransactionId, "FAILED", transactionNoVNP, "Payment failed at VNPay. Code: " + responseCode);
    //             navigateToPaymentResultScreen(false, "Payment failed at VNPay. Reason: " + params.get("vnp_Message"), params);
    //         }
    //     } catch (SQLException | ResourceNotFoundException e) {
    //         AlertHelper.showErrorAlert("Error", "Failed to update local transaction status: " + e.getMessage());
    //     }
    // }
}