package com.aims.core.presentation.controllers;

import com.aims.core.entities.OrderEntity;
import com.aims.core.entities.OrderItem;
import com.aims.core.entities.DeliveryInfo;
// import com.aims.presentation.utils.AlertHelper;
// import com.aims.presentation.utils.FXMLSceneManager;
// import com.aims.core.application.dtos.OrderItemDTO; // Nếu bạn dùng DTO cho item

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text; // Dùng Text để wrap text tốt hơn Label trong một số trường hợp

import java.io.IOException;
import java.time.format.DateTimeFormatter;

public class OrderSummaryController {

    @FXML
    private Label orderIdLabel;
    @FXML
    private Label orderDateLabel;

    @FXML
    private Text recipientNameText;
    @FXML
    private Text phoneText;
    @FXML
    private Text emailText;
    @FXML
    private Text addressText;
    @FXML
    private Text provinceCityText;
    @FXML
    private Text instructionsText;
    @FXML
    private Text deliveryMethodText;
    @FXML
    private Label rushTimeLabel;
    @FXML
    private Text rushTimeText;


    @FXML
    private VBox orderItemsVBox;

    @FXML
    private Label subtotalExclVATLabel;
    @FXML
    private Label vatLabel;
    @FXML
    private Label subtotalInclVATLabel;
    @FXML
    private Label shippingFeeLabel;
    @FXML
    private Label totalAmountPaidLabel;

    @FXML
    private Label errorMessageLabel;
    @FXML
    private Button proceedToPaymentMethodButton;

    private MainLayoutController mainLayoutController;
    // private FXMLSceneManager sceneManager;
    private OrderEntity currentOrder;
    private static final float VAT_RATE = 0.10f;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");


    public OrderSummaryController() {
        // Constructor
    }

    public void setMainLayoutController(MainLayoutController mainLayoutController) {
        this.mainLayoutController = mainLayoutController;
    }
    
    // public void setSceneManager(FXMLSceneManager sceneManager) { this.sceneManager = sceneManager; }

    public void initialize() {
        errorMessageLabel.setText("");
        errorMessageLabel.setVisible(false);
    }

    /**
     * Được gọi từ controller trước (DeliveryInfoScreenController) để truyền dữ liệu đơn hàng.
     */
    public void setOrderData(OrderEntity order) {
        this.currentOrder = order;
        if (currentOrder != null) {
            populateOrderSummary();
        } else {
            // AlertHelper.showErrorAlert("Error", "No order data received for summary.");
             System.err.println("OrderSummaryScreen: No order data received.");
            // Có thể điều hướng về trang trước hoặc hiển thị lỗi rõ ràng
            orderIdLabel.setText("Error: Order data missing.");
            proceedToPaymentMethodButton.setDisable(true);
        }
    }

    private void populateOrderSummary() {
        orderIdLabel.setText(currentOrder.getOrderId());
        orderDateLabel.setText(currentOrder.getOrderDate().format(DATE_TIME_FORMATTER));

        DeliveryInfo deliveryInfo = currentOrder.getDeliveryInfo();
        if (deliveryInfo != null) {
            recipientNameText.setText(deliveryInfo.getRecipientName());
            phoneText.setText(deliveryInfo.getPhoneNumber());
            emailText.setText(deliveryInfo.getEmail());
            addressText.setText(deliveryInfo.getDeliveryAddress());
            provinceCityText.setText(deliveryInfo.getDeliveryProvinceCity());
            instructionsText.setText(deliveryInfo.getDeliveryInstructions() != null ? deliveryInfo.getDeliveryInstructions() : "N/A");
            deliveryMethodText.setText(deliveryInfo.getDeliveryMethodChosen());

            if ("RUSH_DELIVERY".equalsIgnoreCase(deliveryInfo.getDeliveryMethodChosen()) && deliveryInfo.getRequestedRushDeliveryTime() != null) {
                rushTimeLabel.setVisible(true);
                rushTimeLabel.setManaged(true);
                rushTimeText.setText(deliveryInfo.getRequestedRushDeliveryTime().format(DATE_TIME_FORMATTER));
                rushTimeText.setVisible(true);
                rushTimeText.setManaged(true);
            } else {
                rushTimeLabel.setVisible(false);
                rushTimeLabel.setManaged(false);
                rushTimeText.setVisible(false);
                rushTimeText.setManaged(false);
            }
        }

        orderItemsVBox.getChildren().clear();
        if (currentOrder.getOrderItems() != null) {
            for (OrderItem item : currentOrder.getOrderItems()) {
                try {
                    // Sử dụng FXML partial 'order_item_row.fxml' để hiển thị mỗi item
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/aims/presentation/views/partials/order_item_row.fxml"));
                    Parent itemNode = loader.load();
                    // Get the controller and set the data
                    OrderItemRowController itemController = loader.getController();
                    itemController.setData(item);
                    orderItemsVBox.getChildren().add(itemNode);

                } catch (IOException e) {
                    e.printStackTrace();
                    System.err.println("Error loading order_item_row.fxml: " + e.getMessage());
                    // Fallback to manual creation if FXML loading fails
                    VBox itemBox = new VBox(2);
                    Label title = new Label(item.getProduct().getTitle() + " (x" + item.getQuantity() + ")");
                    title.setStyle("-fx-font-weight: bold;");
                    Label price = new Label(String.format("Price/unit: %,.0f VND, Total: %,.0f VND",
                                            item.getPriceAtTimeOfOrder(), // Giá chưa VAT
                                            item.getPriceAtTimeOfOrder() * item.getQuantity()));
                    itemBox.getChildren().addAll(title, price);
                    orderItemsVBox.getChildren().add(itemBox);
                }
            }
        }

        subtotalExclVATLabel.setText(String.format("%,.0f VND", currentOrder.getTotalProductPriceExclVAT()));
        float vatAmount = currentOrder.getTotalProductPriceInclVAT() - currentOrder.getTotalProductPriceExclVAT();
        vatLabel.setText(String.format("%,.0f VND", vatAmount));
        subtotalInclVATLabel.setText(String.format("%,.0f VND", currentOrder.getTotalProductPriceInclVAT()));
        shippingFeeLabel.setText(String.format("%,.0f VND", currentOrder.getCalculatedDeliveryFee()));
        totalAmountPaidLabel.setText(String.format("%,.0f VND", currentOrder.getTotalAmountPaid()));
    }


    @FXML
    void handleBackToDeliveryInfoAction(ActionEvent event) {
        System.out.println("Back to Delivery Info action triggered");
        
        if (mainLayoutController != null && currentOrder != null) {
            try {
                Object controller = mainLayoutController.loadContent("/com/aims/presentation/views/delivery_info_screen.fxml");
                mainLayoutController.setHeaderTitle("Delivery Information");
                
                // Pass order data to delivery controller for editing and recalculation
                if (controller instanceof DeliveryInfoScreenController) {
                    ((DeliveryInfoScreenController) controller).setOrderData(currentOrder);
                }
                
                System.out.println("Successfully navigated back to delivery info");
            } catch (Exception e) {
                System.err.println("Error navigating back to delivery info: " + e.getMessage());
            }
        } else {
            System.err.println("MainLayoutController or order data not available for back navigation");
        }
    }

    @FXML
    void handleProceedToPaymentMethodAction(ActionEvent event) {
        if (currentOrder == null) {
            System.err.println("No order data to proceed with payment");
            return;
        }
        
        System.out.println("Proceed to Select Payment Method action triggered for Order ID: " + currentOrder.getOrderId());
        
        if (mainLayoutController != null) {
            try {
                Object controller = mainLayoutController.loadContent("/com/aims/presentation/views/payment_method_screen.fxml");
                mainLayoutController.setHeaderTitle("Select Payment Method");
                
                // Pass order data to payment method controller
                if (controller instanceof PaymentMethodScreenController) {
                    ((PaymentMethodScreenController) controller).setOrderData(currentOrder);
                }
                
                System.out.println("Successfully navigated to payment method selection");
            } catch (Exception e) {
                System.err.println("Error navigating to payment method: " + e.getMessage());
            }
        } else {
            System.err.println("MainLayoutController not available for navigation");
        }
    }
}