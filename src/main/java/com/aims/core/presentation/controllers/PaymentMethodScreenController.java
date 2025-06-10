package com.aims.core.presentation.controllers;

import com.aims.core.entities.OrderEntity;
// import com.aims.presentation.utils.AlertHelper;
// import com.aims.presentation.utils.FXMLSceneManager;
// import com.aims.core.application.services.IPaymentService; // Sẽ cần khi thực sự xử lý thanh toán

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.application.HostServices; // Để mở trình duyệt

public class PaymentMethodScreenController {

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
    // private IPaymentService paymentService; // Sẽ được inject
    private OrderEntity currentOrder;
    private HostServices hostServices; // Để mở URL trong trình duyệt mặc định

    public PaymentMethodScreenController() {
        // paymentService = new PaymentServiceImpl(...); // DI
    }

    public void setMainLayoutController(MainLayoutController mainLayoutController) {
        this.mainLayoutController = mainLayoutController;
    }
    
    // public void setSceneManager(FXMLSceneManager sceneManager) { this.sceneManager = sceneManager; }
    // public void setPaymentService(IPaymentService paymentService) { this.paymentService = paymentService; }
    public void setHostServices(HostServices hostServices) { this.hostServices = hostServices; }


    public void initialize() {
        errorMessageLabel.setText("");
        errorMessageLabel.setVisible(false);

        paymentMethodToggleGroup.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                String selectedMethod = (String) newValue.getUserData();
                updateDescriptionAndButton(selectedMethod);
            }
        });
        // Initialize description for the default selected radio button
        if(vnpayCreditCardRadio.isSelected()){
            updateDescriptionAndButton((String) vnpayCreditCardRadio.getUserData());
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
        System.out.println("Back to Order Summary action triggered");
        
        if (mainLayoutController != null && currentOrder != null) {
            try {
                Object controller = mainLayoutController.loadContent("/com/aims/presentation/views/order_summary_screen.fxml");
                mainLayoutController.setHeaderTitle("Order Summary & Confirmation");
                
                // TODO: Pass order data to summary controller when setOrderData method is available
                // if (controller instanceof OrderSummaryController) {
                //     ((OrderSummaryController) controller).setOrderData(currentOrder);
                // }
                
                System.out.println("Successfully navigated back to order summary");
            } catch (Exception e) {
                System.err.println("Error navigating back to order summary: " + e.getMessage());
            }
        } else {
            System.err.println("MainLayoutController or order data not available for back navigation");
        }
    }

    @FXML
    void handleProceedAction(ActionEvent event) {
        if (currentOrder == null) {
            // AlertHelper.showErrorAlert("Error", "No order to process payment for.");
            setErrorMessage("No order information available to proceed.", true);
            return;
        }
        setErrorMessage("", false);

        RadioButton selectedRadio = (RadioButton) paymentMethodToggleGroup.getSelectedToggle();
        if (selectedRadio == null) {
            // AlertHelper.showWarningAlert("No Selection", "Please select a payment method.");
             setErrorMessage("Please select a payment method.", true);
            return;
        }

        String selectedMethodUserData = (String) selectedRadio.getUserData();
        System.out.println("Proceeding with payment method: " + selectedMethodUserData + " for Order ID: " + currentOrder.getOrderId());

        // Đây là nơi bạn sẽ gọi IPaymentService để bắt đầu quá trình thanh toán.
        // IPaymentService sẽ trả về một URL redirect của VNPay (trong PaymentResultDTO hoặc Map).
        // Sau đó, bạn sẽ mở URL này trong trình duyệt.
        // Việc xử lý callback từ VNPay (vnp_ReturnUrl) sẽ phức tạp hơn trong desktop app.
        // Bạn có thể cần một server nhỏ lắng nghe hoặc một cơ chế khác.
        // Hoặc, cho desktop, VNPay có thể cung cấp SDK/API cho phép thanh toán không qua redirect hoàn toàn (cần kiểm tra tài liệu VNPay).

        // --- Bắt đầu ví dụ luồng gọi PaymentService ---
        // if (paymentService == null) {
        //     AlertHelper.showErrorAlert("Service Error", "Payment service is not available.");
        //     return;
        // }
        //
        // try {
        //     Map<String, Object> paymentParamsForStrategy = new HashMap<>();
        //     // Lấy IP của client nếu có thể, ví dụ:
        //     // paymentParamsForStrategy.put("ipAddress", "192.168.1.10"); // Lấy IP thực tế
        //
        //     if ("VNPAY_DOMESTIC_CARD".equals(selectedMethodUserData)) {
        //         // Cần lấy bank code, ví dụ từ một ComboBox khác trên màn hình này (chưa thêm vào FXML)
        //         // String bankCode = selectedBankComboBox.getValue();
        //         // if (bankCode == null || bankCode.isEmpty()) {
        //         //     AlertHelper.showErrorAlert("Missing Information", "Please select your bank for domestic card payment.");
        //         //     return;
        //         // }
        //         // paymentParamsForStrategy.put("vnp_BankCode", bankCode);
        //         System.out.println("Domestic card selected - bank code selection needed.");
        //     }
        //
        //     // PaymentServiceImpl sẽ chọn strategy phù hợp dựa trên paymentMethodId hoặc thông tin từ OrderEntity
        //     // Hoặc bạn có thể truyền PaymentMethodType vào PaymentService
        //     PaymentResultDTO paymentResult = paymentService.initiatePayment(currentOrder, selectedMethodUserData, paymentParamsForStrategy); // initiatePayment là một ví dụ tên method
        //
        //     if (paymentResult.getPaymentUrl() != null && !paymentResult.getPaymentUrl().isEmpty()) {
        //         if (hostServices != null) {
        //             hostServices.showDocument(paymentResult.getPaymentUrl());
        //             // Sau khi redirect, ứng dụng cần một cách để biết kết quả thanh toán.
        //             // Có thể là một màn hình chờ (PaymentProcessingScreen) và sau đó điều hướng tới PaymentResultScreen
        //             // Hoặc nếu không có redirect, trực tiếp hiển thị PaymentResultScreen.
        //             navigateToPaymentProcessingScreen(paymentResult.getAimsTransactionId());
        //         } else {
        //             AlertHelper.showErrorAlert("Browser Error", "Cannot open payment URL. HostServices not available.");
        //         }
        //     } else {
        //         // Xử lý trường hợp thanh toán trực tiếp không cần redirect hoặc lỗi ngay khi tạo yêu cầu
        //         navigateToPaymentResultScreen(paymentResult);
        //     }
        //
        // } catch (PaymentException | ValidationException | ResourceNotFoundException | SQLException e) {
        //     e.printStackTrace();
        //     AlertHelper.showErrorAlert("Payment Initiation Failed", e.getMessage());
        // }
        System.out.println("Initiate payment process - implement with actual service call and navigation/redirect.");
        // For now, simulate navigation to processing/result screen
        navigateToPaymentProcessingScreen("TEMP_TRANS_ID_" + currentOrder.getOrderId());
    }

    private void navigateToPaymentProcessingScreen(String aimsTransactionId) {
        System.out.println("Navigating to Payment Processing for transaction: " + aimsTransactionId);
        
        if (mainLayoutController != null) {
            try {
                Object controller = mainLayoutController.loadContent("/com/aims/presentation/views/payment_processing_screen.fxml");
                mainLayoutController.setHeaderTitle("Processing Payment...");
                
                // TODO: Pass transaction data to processing controller when setTransactionData method is available
                // if (controller instanceof PaymentProcessingScreenController) {
                //     ((PaymentProcessingScreenController) controller).setTransactionData(currentOrder, aimsTransactionId);
                // }
                
                System.out.println("Successfully navigated to payment processing screen");
            } catch (Exception e) {
                System.err.println("Error navigating to payment processing: " + e.getMessage());
                setErrorMessage("Navigation error. Please try again.", true);
            }
        } else {
            System.err.println("MainLayoutController not available for navigation");
            setErrorMessage("Navigation error. Please refresh the page.", true);
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