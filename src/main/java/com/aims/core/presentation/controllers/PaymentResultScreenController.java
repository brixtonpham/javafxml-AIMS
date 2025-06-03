package com.aims.core.presentation.controllers;

import com.aims.core.entities.OrderEntity;
// import com.aims.presentation.utils.FXMLSceneManager;
// import com.aims.core.application.dtos.PaymentResultDTO; // Nếu dùng DTO để truyền kết quả

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map; // If gatewayData is a Map

public class PaymentResultScreenController {

    @FXML
    private ImageView statusIconImageView;
    @FXML
    private Label paymentStatusTitleLabel;
    @FXML
    private Label paymentMessageLabel;
    @FXML
    private Label orderIdLabel;
    @FXML
    private Label customerNameLabel;
    @FXML
    private Label phoneLabel;
    @FXML
    private Text shippingAddressText;
    @FXML
    private Label totalAmountLabel;
    @FXML
    private Label aimsTransactionIdLabel;
    @FXML
    private Label gatewayTransactionIdLabel;
    @FXML
    private Label transactionTimeLabel;
    @FXML
    private Text gatewayMessageText;
    @FXML
    private Button viewOrderButton;
    @FXML
    private GridPane detailsGrid;


    // private MainLayoutController mainLayoutController;
    // private FXMLSceneManager sceneManager;
    private OrderEntity currentOrder; // Đơn hàng đã được xử lý (có thể đã được cập nhật status)
    private String localAimsTransactionId; // Mã giao dịch của AIMS

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    // private Image successIcon;
    // private Image failureIcon;

    public PaymentResultScreenController() {
        // Constructor
    }

    // public void setMainLayoutController(MainLayoutController mainLayoutController) { this.mainLayoutController = mainLayoutController; }
    // public void setSceneManager(FXMLSceneManager sceneManager) { this.sceneManager = sceneManager; }

    public void initialize() {
        // try {
        //     successIcon = new Image(getClass().getResourceAsStream("/assets/images/icons/success_icon.png")); // Thay bằng icon thật
        //     failureIcon = new Image(getClass().getResourceAsStream("/assets/images/icons/failure_icon.png")); // Thay bằng icon thật
        // } catch (Exception e) {
        //     System.err.println("Error loading status icons: " + e.getMessage());
        // }
        detailsGrid.setVisible(false); // Ẩn chi tiết ban đầu
    }

    /**
     * Được gọi từ controller trước (PaymentProcessingScreenController hoặc khi xử lý callback)
     * để hiển thị kết quả.
     * @param order The order entity (có thể đã được cập nhật status)
     * @param isSuccess True nếu thanh toán thành công.
     * @param message Thông điệp chung về kết quả.
     * @param gatewayData Dữ liệu trả về từ cổng thanh toán (ví dụ Map các tham số VNPay trả về).
     * @param aimsTxnId Mã giao dịch của AIMS.
     */
    public void setPaymentResult(OrderEntity order, boolean isSuccess, String message, Map<String, String> gatewayData, String aimsTxnId) {
        this.currentOrder = order;
        this.localAimsTransactionId = aimsTxnId;

        paymentMessageLabel.setText(message);

        if (isSuccess) {
            paymentStatusTitleLabel.setText("Payment Successful!");
            paymentStatusTitleLabel.setTextFill(Color.GREEN);
            // if (successIcon != null) statusIconImageView.setImage(successIcon);
            viewOrderButton.setVisible(true);
            viewOrderButton.setManaged(true);
            populateSuccessDetails(gatewayData);
        } else {
            paymentStatusTitleLabel.setText("Payment Failed / Pending");
            paymentStatusTitleLabel.setTextFill(Color.RED);
            // if (failureIcon != null) statusIconImageView.setImage(failureIcon);
            viewOrderButton.setVisible(false);
            viewOrderButton.setManaged(false);
            populateFailureDetails(gatewayData);
        }
    }

    private void populateSuccessDetails(Map<String, String> gatewayData) {
        detailsGrid.setVisible(true);
        if (currentOrder != null) {
            orderIdLabel.setText(currentOrder.getOrderId());
            if (currentOrder.getDeliveryInfo() != null) {
                customerNameLabel.setText(currentOrder.getDeliveryInfo().getRecipientName());
                phoneLabel.setText(currentOrder.getDeliveryInfo().getPhoneNumber());
                shippingAddressText.setText(currentOrder.getDeliveryInfo().getDeliveryAddress() + ", " + currentOrder.getDeliveryInfo().getDeliveryProvinceCity());
            }
            totalAmountLabel.setText(String.format("%,.0f VND", currentOrder.getTotalAmountPaid()));
        }
        aimsTransactionIdLabel.setText(localAimsTransactionId != null ? localAimsTransactionId : "N/A");

        if (gatewayData != null) {
            gatewayTransactionIdLabel.setText(gatewayData.getOrDefault("vnp_TransactionNo", "N/A"));
            String payDateStr = gatewayData.get("vnp_PayDate"); // Format YYYYMMDDHHMMSS
            if (payDateStr != null) {
                try {
                    LocalDateTime payDateTime = LocalDateTime.parse(payDateStr, DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
                    transactionTimeLabel.setText(payDateTime.format(DATE_TIME_FORMATTER));
                } catch (Exception e) {
                    transactionTimeLabel.setText(payDateStr); // Show raw if parse fails
                }
            } else {
                 transactionTimeLabel.setText(LocalDateTime.now().format(DATE_TIME_FORMATTER)); // Fallback
            }
            gatewayMessageText.setText("VNPay Code: " + gatewayData.getOrDefault("vnp_ResponseCode", "N/A") +
                                     " - " + gatewayData.getOrDefault("vnp_Message", "Payment processed."));
        }
    }
    
    private void populateFailureDetails(Map<String, String> gatewayData) {
        detailsGrid.setVisible(true); // Vẫn hiển thị một số thông tin nếu có thể
         if (currentOrder != null) {
            orderIdLabel.setText(currentOrder.getOrderId());
            totalAmountLabel.setText(String.format("%,.0f VND (Attempted)", currentOrder.getTotalAmountPaid()));
             if (currentOrder.getDeliveryInfo() != null) {
                customerNameLabel.setText(currentOrder.getDeliveryInfo().getRecipientName());
            }
        }
        aimsTransactionIdLabel.setText(localAimsTransactionId != null ? localAimsTransactionId : "N/A");
        gatewayTransactionIdLabel.setText(gatewayData != null ? gatewayData.getOrDefault("vnp_TransactionNo", "N/A") : "N/A");
        transactionTimeLabel.setText(LocalDateTime.now().format(DATE_TIME_FORMATTER));
        gatewayMessageText.setText(gatewayData != null ? "VNPay Code: " + gatewayData.getOrDefault("vnp_ResponseCode", "N/A") +
                                     " - " + gatewayData.getOrDefault("vnp_Message", "Details not available.") : "Details not available.");
    }


    @FXML
    void handleViewOrderDetailsAction(ActionEvent event) {
        if (currentOrder == null) return;
        System.out.println("View Order Details action triggered for order: " + currentOrder.getOrderId());
        // if (sceneManager != null && mainLayoutController != null) {
        //     CustomerOrderDetailController detailCtrl = (CustomerOrderDetailController) sceneManager.loadFXMLIntoPane(
        //         mainLayoutController.getContentPane(), FXMLSceneManager.CUSTOMER_ORDER_DETAIL_SCREEN
        //     );
        //     detailCtrl.setOrderData(currentOrder); // Hoặc chỉ OrderID rồi controller tự load
        //     detailCtrl.setMainLayoutController(mainLayoutController);
        //     mainLayoutController.setHeaderTitle("Order Details - #" + currentOrder.getOrderId());
        // }
    }

    @FXML
    void handleContinueShoppingAction(ActionEvent event) {
        System.out.println("Continue Shopping action triggered");
        // if (sceneManager != null && mainLayoutController != null) {
        //     mainLayoutController.loadContent(FXMLSceneManager.HOME_SCREEN);
        //     mainLayoutController.setHeaderTitle("AIMS Home");
        // }
    }
}