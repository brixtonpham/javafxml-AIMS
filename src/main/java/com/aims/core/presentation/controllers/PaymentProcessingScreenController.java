package com.aims.core.presentation.controllers;

import com.aims.core.application.services.IPaymentService;
import com.aims.core.application.services.IOrderService; // To update order status if payment cancelled here
import com.aims.core.entities.OrderEntity;
import com.aims.core.entities.PaymentTransaction;
import com.aims.core.enums.OrderStatus;
// import com.aims.presentation.utils.AlertHelper;
// import com.aims.presentation.utils.FXMLSceneManager;
import com.aims.core.shared.exceptions.PaymentException;
import com.aims.core.shared.exceptions.ResourceNotFoundException;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;

import java.util.HashMap;
import java.util.Map;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView; // If using WebView
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

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
    private VBox contentVBox;


    // @Inject
    private IPaymentService paymentService;
    // @Inject
    private IOrderService orderService; // To update order if payment attempt is cancelled here
    private MainLayoutController mainLayoutController;
    private com.aims.core.presentation.utils.FXMLSceneManager sceneManager;

    private OrderEntity currentOrder;
    private String aimsTransactionId; // The AIMS internal transaction ID
    private String vnpayPaymentUrl; // URL from IPaymentService to load in WebView or open in browser

    private Task<PaymentTransaction> paymentStatusCheckTask;

    public PaymentProcessingScreenController() {
        // paymentService = new PaymentServiceImpl(...); // DI
        // orderService = new OrderServiceImpl(...);   // DI
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
    }
    
    public void setOrderService(IOrderService orderService) {
        this.orderService = orderService;
        System.out.println("PaymentProcessingScreenController.setOrderService: OrderService injected successfully - Available: " + (orderService != null));
    }


    public void initialize() {
        // Hide WebView initially if it's an option
        if (vnPayWebView != null) {
            vnPayWebView.setVisible(false);
            vnPayWebView.setManaged(false);
        }
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

        if (this.vnpayPaymentUrl != null && !this.vnpayPaymentUrl.isEmpty()) {
            // Option 1: If using WebView (uncomment FXML WebView and this block)
            // setupWebView();
            // contentVBox.getChildren().removeAll(progressIndicator, statusLabel, instructionLabel, checkStatusButton, cancelPaymentButton); // Hide loading stuff
            // vnPayWebView.setVisible(true);
            // vnPayWebView.setManaged(true);
            // WebEngine webEngine = vnPayWebView.getEngine();
            // webEngine.load(this.vnpayPaymentUrl);

            // Option 2: If opening external browser (HostServices would be used by previous controller)
            // This screen then becomes a waiting screen.
            statusLabel.setText("You have been redirected to VNPay. Please complete your payment.");
            instructionLabel.setText("After completing payment on VNPay's site, please return to this application. We will attempt to verify your payment. You can also check manually.");
            // Show manual check button after some time
            checkStatusButton.setVisible(true);
            checkStatusButton.setManaged(true);
        } else {
            // This case might occur if payment is processed directly without redirect
            // or if there was an error generating the URL.
            statusLabel.setText("Preparing to process your payment directly...");
            // Potentially start polling or wait for a different kind of callback
        }
    }

    private void setupWebView() {
        // WebEngine webEngine = vnPayWebView.getEngine();

        // // Listener for URL changes to detect VNPay's return URL
        // webEngine.locationProperty().addListener(new ChangeListener<String>() {
        //     @Override
        //     public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
        //         System.out.println("WebView location changed: " + newValue);
        //         if (newValue != null && newValue.startsWith(VNPayConfig.VNP_RETURN_URL)) { // VNPayConfig should be accessible
        //             vnPayWebView.setVisible(false); // Hide webview
        //             vnPayWebView.setManaged(false);
        //             contentVBox.getChildren().setAll(progressIndicator, statusLabel, instructionLabel, checkStatusButton, cancelPaymentButton); // Show loading again
        //             statusLabel.setText("Processing payment result from VNPay...");
        //             // Parse parameters from newValue (the return URL)
        //             // Validate signature
        //             // Then navigate to PaymentResultScreen
        //             // Example: Map<String, String> params = parseQueryParams(newValue);
        //             // handleVNPayCallback(params);
        //         }
        //     }
        // });

        // webEngine.getLoadWorker().stateProperty().addListener(...); // To show loading progress in WebView
    }


    @FXML
    void handleCheckStatusAction(ActionEvent event) {
        if (aimsTransactionId == null || paymentService == null) {
            // AlertHelper.showErrorAlert("Error", "Cannot check status. Transaction ID or service is missing.");
            statusLabel.setText("Error: Cannot check status. Missing information.");
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
                // AlertHelper.showErrorAlert("Status Check Failed", "Could not retrieve payment status: " + cause.getMessage());
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
        // Update order status to PAYMENT_FAILED or CANCELLED if applicable
        // if (currentOrder != null && orderService != null) {
        //     try {
        //         orderService.updateOrderStatus(currentOrder.getOrderId(), OrderStatus.PAYMENT_FAILED, currentOrder.getUserAccount() != null ? currentOrder.getUserAccount().getUserId() : "SYSTEM_CANCEL");
        //     } catch (SQLException | ResourceNotFoundException | ValidationException e) {
        //         // AlertHelper.showErrorAlert("Error", "Failed to update order status after cancellation: " + e.getMessage());
        //         System.err.println("Failed to update order status for " + currentOrder.getOrderId() + " after payment cancel: " + e.getMessage());
        //     }
        // }
        // Navigate back or to a payment failed screen
        // if (sceneManager != null && mainLayoutController != null) {
        //    mainLayoutController.loadContent(FXMLSceneManager.PAYMENT_RESULT_SCREEN); // Or cart screen
        //    // Pass data to PaymentResultScreenController indicating cancellation
        //    mainLayoutController.setHeaderTitle("Payment Cancelled");
        // }
        statusLabel.setText("Payment attempt cancelled.");
        progressIndicator.setVisible(false);
        checkStatusButton.setVisible(false); // Hide manual check
    }

    private void navigateToPaymentResultScreen(boolean success, String message, Map<String, String> gatewayData) {
        System.out.println("Navigating to Payment Result Screen. Success: " + success + ", Message: " + message);
        // if (sceneManager != null && mainLayoutController != null) {
        //     PaymentResultScreenController resultCtrl = (PaymentResultScreenController) sceneManager.loadFXMLIntoPane(
        //         mainLayoutController.getContentPane(), FXMLSceneManager.PAYMENT_RESULT_SCREEN
        //     );
        //     resultCtrl.setPaymentResult(currentOrder, success, message, gatewayData, aimsTransactionId);
        //     resultCtrl.setMainLayoutController(mainLayoutController);
        //     mainLayoutController.setHeaderTitle(success ? "Payment Successful" : "Payment Failed");
        // }
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