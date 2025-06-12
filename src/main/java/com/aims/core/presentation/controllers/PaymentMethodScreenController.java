package com.aims.core.presentation.controllers;

import com.aims.core.entities.OrderEntity;
import com.aims.core.entities.PaymentMethod;
import com.aims.core.entities.PaymentTransaction;
import com.aims.core.enums.PaymentMethodType;
import com.aims.core.application.services.IPaymentService;
import com.aims.core.shared.exceptions.PaymentException;
import com.aims.core.shared.exceptions.ValidationException;
import com.aims.core.shared.exceptions.ResourceNotFoundException;
import com.aims.core.presentation.utils.PaymentErrorHandler;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
// import com.aims.presentation.utils.AlertHelper;
// import com.aims.presentation.utils.FXMLSceneManager;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.application.HostServices; // Để mở trình duyệt

import java.sql.SQLException;
import java.util.UUID;

public class PaymentMethodScreenController implements MainLayoutController.IChildController {

    @FXML
    private ToggleGroup paymentMethodToggleGroup;
    @FXML
    private RadioButton vnpayCreditCardRadio;
    // @FXML private RadioButton vnpayDomesticCardRadio; // For future
    @FXML
    private Label selectedMethodDescriptionLabel;
    @FXML
    private Label errorMessageLabel;
    @FXML
    private Button proceedButton;

    private MainLayoutController mainLayoutController;
    // private FXMLSceneManager sceneManager;
    private IPaymentService paymentService; // Service for payment processing
    private OrderEntity currentOrder;
    private HostServices hostServices; // Để mở URL trong trình duyệt mặc định
    private final Gson gson = new Gson(); // For parsing JSON response data

    public PaymentMethodScreenController() {
        // paymentService = new PaymentServiceImpl(...); // DI
    }

    @Override
    public void setMainLayoutController(MainLayoutController mainLayoutController) {
        this.mainLayoutController = mainLayoutController;
        System.out.println("PaymentMethodScreenController: MainLayoutController injected successfully");
    }
    
    // public void setSceneManager(FXMLSceneManager sceneManager) { this.sceneManager = sceneManager; }
    public void setPaymentService(IPaymentService paymentService) {
        this.paymentService = paymentService;
        System.out.println("PaymentMethodScreenController: PaymentService injected successfully");
    }
    
    public void setHostServices(HostServices hostServices) { this.hostServices = hostServices; }


    public void initialize() {
        System.out.println("PaymentMethodScreenController.initialize: Starting initialization");
        
        // Enhanced initialization with error handling
        try {
            errorMessageLabel.setText("");
            errorMessageLabel.setVisible(false);
            
            // Validate UI components before setting up listeners
            if (paymentMethodToggleGroup == null) {
                System.err.println("PaymentMethodScreenController.initialize: CRITICAL - paymentMethodToggleGroup is null");
                setErrorMessage("Payment method selection is not available. Please reload the page.", true);
                return;
            }
            
            if (vnpayCreditCardRadio == null) {
                System.err.println("PaymentMethodScreenController.initialize: CRITICAL - vnpayCreditCardRadio is null");
                setErrorMessage("Payment options are not available. Please reload the page.", true);
                return;
            }

            paymentMethodToggleGroup.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
                try {
                    if (newValue != null) {
                        String selectedMethod = (String) newValue.getUserData();
                        updateDescriptionAndButton(selectedMethod);
                        // Clear any previous error messages when selection changes
                        if (errorMessageLabel.isVisible()) {
                            setErrorMessage("", false);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("PaymentMethodScreenController.initialize: Error in selection listener: " + e.getMessage());
                    setErrorMessage("Error updating payment method selection", true);
                }
            });
            
            // Initialize description for the default selected radio button
            if(vnpayCreditCardRadio.isSelected()){
                updateDescriptionAndButton((String) vnpayCreditCardRadio.getUserData());
            }
            
            System.out.println("PaymentMethodScreenController.initialize: Initialization completed successfully");
            
        } catch (Exception e) {
            System.err.println("PaymentMethodScreenController.initialize: CRITICAL initialization error: " + e.getMessage());
            e.printStackTrace();
            setErrorMessage("Failed to initialize payment method screen. Please try refreshing the page.", true);
        }
    }

    /**
     * Được gọi từ controller trước đó (OrderSummaryController) để truyền dữ liệu đơn hàng.
     */
    public void setOrderData(OrderEntity order) {
        this.currentOrder = order;
        if (currentOrder == null) {
            // AlertHelper.showErrorAlert("Error", "No order data received for payment method selection.");
            System.err.println("PaymentMethodScreen: No order data received.");
            proceedButton.setDisable(true);
        } else {
            proceedButton.setDisable(false);
        }
    }

    private void updateDescriptionAndButton(String selectedMethodUserData) {
        switch (selectedMethodUserData) {
            case "VNPAY_CREDIT_CARD":
                selectedMethodDescriptionLabel.setText("You will be redirected to the secure VNPay gateway to complete your payment using your Credit/Debit Card.");
                proceedButton.setText("Proceed with VNPay (Card)");
                proceedButton.setDisable(false);
                break;
            // case "VNPAY_DOMESTIC_CARD":
            //     selectedMethodDescriptionLabel.setText("Select your bank and pay using Domestic ATM Card / Internet Banking via VNPay.");
            //     proceedButton.setText("Proceed with VNPay (Domestic)");
            //     proceedButton.setDisable(false);
            //     break;
            default:
                selectedMethodDescriptionLabel.setText("Please select a payment method.");
                proceedButton.setText("Proceed");
                proceedButton.setDisable(true);
                break;
        }
    }

    @FXML
    void handleBackToOrderSummaryAction(ActionEvent event) {
        System.out.println("PaymentMethodScreenController: Back to Order Summary action triggered");
        
        if (mainLayoutController != null && currentOrder != null) {
            try {
                Object controller = mainLayoutController.loadContent("/com/aims/presentation/views/order_summary_screen.fxml");
                mainLayoutController.setHeaderTitle("Order Summary & Confirmation");
                
                // Verify controller loading and injection
                if (controller instanceof OrderSummaryController) {
                    ((OrderSummaryController) controller).setOrderData(currentOrder);
                    System.out.println("PaymentMethodScreenController: Successfully navigated back to order summary");
                } else {
                    System.err.println("PaymentMethodScreenController: Controller injection failed - invalid controller type");
                    setErrorMessage("Failed to initialize order summary screen properly", true);
                }
                
            } catch (Exception e) {
                System.err.println("PaymentMethodScreenController: Navigation error: " + e.getMessage());
                e.printStackTrace();
                setErrorMessage("Failed to navigate back to order summary: " + e.getMessage(), true);
            }
        } else {
            System.err.println("PaymentMethodScreenController: MainLayoutController or order data not available for back navigation");
            setErrorMessage("Navigation error. Cannot go back to order summary.", true);
        }
    }

    @FXML
    void handleProceedAction(ActionEvent event) {
        System.out.println("PaymentMethodScreenController.handleProceedAction: Payment proceed action initiated");
        
        try {
            // Enhanced input validation with specific error messages
            if (currentOrder == null) {
                System.err.println("PaymentMethodScreenController.handleProceedAction: No order data available");
                setErrorMessage("No order information available to proceed. Please restart the order process.", true);
                proceedButton.setDisable(true);
                return;
            }
            
            // Clear previous error messages
            setErrorMessage("", false);
            
            // Validate payment method selection
            if (paymentMethodToggleGroup == null) {
                System.err.println("PaymentMethodScreenController.handleProceedAction: Payment method group is null");
                setErrorMessage("Payment method selection is not available. Please reload the page.", true);
                return;
            }

            RadioButton selectedRadio = (RadioButton) paymentMethodToggleGroup.getSelectedToggle();
            if (selectedRadio == null) {
                System.err.println("PaymentMethodScreenController.handleProceedAction: No payment method selected");
                setErrorMessage("Please select a payment method to continue.", true);
                return;
            }

            String selectedMethodUserData = (String) selectedRadio.getUserData();
            if (selectedMethodUserData == null || selectedMethodUserData.trim().isEmpty()) {
                System.err.println("PaymentMethodScreenController.handleProceedAction: Invalid payment method data");
                setErrorMessage("Selected payment method is invalid. Please select a different method.", true);
                return;
            }
            
            System.out.println("PaymentMethodScreenController.handleProceedAction: Proceeding with payment method: " + selectedMethodUserData + " for Order ID: " + currentOrder.getOrderId());

            // Validate navigation dependencies before proceeding
            if (mainLayoutController == null) {
                System.err.println("PaymentMethodScreenController.handleProceedAction: CRITICAL - MainLayoutController not available");
                setErrorMessage("System error: Cannot proceed with payment. Please try refreshing the application.", true);
                return;
            }

            // Enhanced payment service handling
            if (paymentService != null) {
                System.out.println("PaymentMethodScreenController.handleProceedAction: PaymentService available - initiating actual payment");
                
                try {
                    // Create a temporary PaymentMethod for VNPAY processing
                    PaymentMethod vnpayPaymentMethod = createVNPayPaymentMethod(selectedMethodUserData);
                    
                    // Call PaymentService to process payment
                    System.out.println("PaymentMethodScreenController.handleProceedAction: Calling PaymentService.processPayment()");
                    PaymentTransaction transaction = paymentService.processPayment(currentOrder, vnpayPaymentMethod.getPaymentMethodId());
                    
                    // Extract payment URL from gateway response data
                    String paymentUrl = extractPaymentUrlFromTransaction(transaction);
                    
                    if (paymentUrl != null && !paymentUrl.trim().isEmpty()) {
                        System.out.println("PaymentMethodScreenController.handleProceedAction: Payment URL extracted successfully: " + paymentUrl);
                        navigateToPaymentProcessingScreen(transaction.getTransactionId(), paymentUrl);
                    } else {
                        System.err.println("PaymentMethodScreenController.handleProceedAction: No payment URL generated");
                        setErrorMessage("Failed to generate payment URL. Please try again.", true);
                    }
                    
                } catch (PaymentException | ValidationException | ResourceNotFoundException | SQLException e) {
                    System.err.println("PaymentMethodScreenController.handleProceedAction: Payment processing failed: " + e.getMessage());
                    e.printStackTrace();
                    
                    // Use centralized error handling
                    PaymentErrorHandler.handlePaymentInitiationError(e, this, currentOrder);
                } catch (Exception e) {
                    System.err.println("PaymentMethodScreenController.handleProceedAction: Unexpected error during payment: " + e.getMessage());
                    e.printStackTrace();
                    
                    // Use centralized error handling for unexpected errors
                    PaymentErrorHandler.handlePaymentInitiationError(e, this, currentOrder);
                }
            } else {
                System.err.println("PaymentMethodScreenController.handleProceedAction: PaymentService not available - cannot proceed");
                
                // Handle service unavailable error
                ValidationException serviceException = new ValidationException("Payment service is not available. Please refresh the application and try again.");
                PaymentErrorHandler.handlePaymentInitiationError(serviceException, this, currentOrder);
            }
            
        } catch (Exception e) {
            System.err.println("PaymentMethodScreenController.handleProceedAction: Unexpected error during payment proceed: " + e.getMessage());
            e.printStackTrace();
            
            // Use centralized error handling for top-level exceptions
            PaymentErrorHandler.handlePaymentInitiationError(e, this, currentOrder);
        }
    }

    /**
     * Creates a temporary PaymentMethod for VNPAY processing
     */
    private PaymentMethod createVNPayPaymentMethod(String selectedMethodUserData) {
        PaymentMethod paymentMethod = new PaymentMethod();
        paymentMethod.setPaymentMethodId("VNPAY_TEMP_" + UUID.randomUUID().toString());
        
        // Set payment method type based on selection
        switch (selectedMethodUserData) {
            case "VNPAY_CREDIT_CARD":
                paymentMethod.setMethodType(PaymentMethodType.CREDIT_CARD);
                break;
            case "VNPAY_DOMESTIC_CARD":
                paymentMethod.setMethodType(PaymentMethodType.DOMESTIC_DEBIT_CARD);
                break;
            default:
                paymentMethod.setMethodType(PaymentMethodType.CREDIT_CARD); // Default
                break;
        }
        
        // No user account association for temporary payment method
        paymentMethod.setUserAccount(null);
        paymentMethod.setDefault(false);
        
        System.out.println("PaymentMethodScreenController: Created temporary PaymentMethod: " + paymentMethod.getPaymentMethodId() + " of type: " + paymentMethod.getMethodType());
        return paymentMethod;
    }

    /**
     * Extracts payment URL from PaymentTransaction's gateway response data
     */
    private String extractPaymentUrlFromTransaction(PaymentTransaction transaction) {
        try {
            String gatewayResponseData = transaction.getGatewayResponseData();
            if (gatewayResponseData != null && !gatewayResponseData.trim().isEmpty()) {
                JsonObject responseJson = gson.fromJson(gatewayResponseData, JsonObject.class);
                if (responseJson.has("paymentUrl")) {
                    String paymentUrl = responseJson.get("paymentUrl").getAsString();
                    System.out.println("PaymentMethodScreenController: Extracted payment URL: " + paymentUrl);
                    return paymentUrl;
                } else {
                    System.err.println("PaymentMethodScreenController: No paymentUrl found in gateway response data");
                }
            } else {
                System.err.println("PaymentMethodScreenController: Gateway response data is null or empty");
            }
        } catch (Exception e) {
            System.err.println("PaymentMethodScreenController: Error parsing gateway response data: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    private void navigateToPaymentProcessingScreen(String aimsTransactionId) {
        navigateToPaymentProcessingScreen(aimsTransactionId, null);
    }

    private void navigateToPaymentProcessingScreen(String aimsTransactionId, String paymentUrl) {
        System.out.println("PaymentMethodScreenController: Navigating to Payment Processing for transaction: " + aimsTransactionId +
                          (paymentUrl != null ? " with payment URL" : " without payment URL"));
        
        // Enhanced defensive programming
        if (mainLayoutController != null) {
            try {
                Object controller = mainLayoutController.loadContent("/com/aims/presentation/views/payment_processing_screen.fxml");
                mainLayoutController.setHeaderTitle("Processing Payment...");
                
                // Verify controller loading and injection
                if (controller instanceof PaymentProcessingScreenController) {
                    PaymentProcessingScreenController processingController = (PaymentProcessingScreenController) controller;
                    processingController.setMainLayoutController(mainLayoutController);
                    
                    // Inject HostServices for browser launching
                    if (hostServices != null) {
                        processingController.setHostServices(hostServices);
                        System.out.println("PaymentMethodScreenController: HostServices injected to PaymentProcessingScreenController");
                    } else {
                        System.out.println("PaymentMethodScreenController: Warning - HostServices not available for browser launching");
                    }
                    
                    // Set transaction data with or without payment URL
                    if (paymentUrl != null && !paymentUrl.trim().isEmpty()) {
                        processingController.setPaymentData(currentOrder, aimsTransactionId, paymentUrl);
                    } else {
                        processingController.setTransactionData(currentOrder, aimsTransactionId);
                    }
                    
                    System.out.println("PaymentMethodScreenController: Transaction data passed to processing controller successfully");
                } else {
                    System.err.println("PaymentMethodScreenController: Controller injection failed - invalid controller type");
                    setErrorMessage("Failed to initialize payment processing screen properly", true);
                    return;
                }
                
                System.out.println("PaymentMethodScreenController: Successfully navigated to payment processing screen");
            } catch (Exception e) {
                System.err.println("PaymentMethodScreenController: Navigation error: " + e.getMessage());
                e.printStackTrace();
                setErrorMessage("Failed to navigate to payment processing: " + e.getMessage(), true);
            }
        } else {
            System.err.println("PaymentMethodScreenController: CRITICAL - MainLayoutController not available for navigation");
            setErrorMessage("Cannot navigate to payment processing due to a system error. Please try refreshing the page or restart the application.", true);
        }
    }

     private void navigateToPaymentResultScreen(com.aims.core.application.dtos.PaymentResultDTO paymentResult) {
        // Code để điều hướng đến màn hình kết quả thanh toán
        System.out.println("Navigating to Payment Result: " + paymentResult.status());
    }

    private void setErrorMessage(String message, boolean visible) {
        errorMessageLabel.setText(message);
        errorMessageLabel.setVisible(visible);
        errorMessageLabel.setManaged(visible);
    }
}