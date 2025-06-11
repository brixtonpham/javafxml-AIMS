package com.aims.core.presentation.controllers;

import com.aims.core.application.services.IOrderService;
import com.aims.core.entities.OrderEntity;
import com.aims.core.entities.OrderItem;
import com.aims.core.entities.DeliveryInfo;
import com.aims.core.entities.Invoice;
import com.aims.core.entities.PaymentTransaction;
import com.aims.core.enums.OrderStatus;
import com.aims.core.shared.exceptions.ResourceNotFoundException;
import com.aims.core.presentation.utils.AlertHelper;
import com.aims.core.presentation.utils.FXMLSceneManager;
import com.aims.core.shared.constants.FXMLPaths; // For FXML paths
import java.util.Optional; // For AlertHelper confirmation
import javafx.scene.control.ButtonType; // For AlertHelper confirmation

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text; // For better text wrapping

import java.io.IOException;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class CustomerOrderDetailController {

    @FXML
    private Label screenTitleLabel;
    @FXML
    private Label orderIdLabel;
    @FXML
    private Label orderDateLabel;
    @FXML
    private Label orderStatusLabel;

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
    private Label rushTimeLabelHeader;
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
    private VBox paymentTransactionsVBox;

    @FXML
    private Label errorMessageLabel;
    @FXML
    private Button cancelOrderButton;

    // @Inject
    private IOrderService orderService;
    private MainLayoutController mainLayoutController;
    private com.aims.core.presentation.utils.FXMLSceneManager sceneManager;

    private OrderEntity currentOrder;
    private String orderIdToLoad;
    // Assuming customer ID is available from a logged-in session or passed if needed for cancel authorization
    private String currentCustomerId; // TODO: Get this from session/auth context

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public CustomerOrderDetailController() {
        // orderService = new OrderServiceImpl(...); // DI
    }

    public void setMainLayoutController(MainLayoutController mainLayoutController) {
        this.mainLayoutController = mainLayoutController;
        System.out.println("CustomerOrderDetailController.setMainLayoutController: MainLayoutController injected successfully");
    }
    
    public void setSceneManager(com.aims.core.presentation.utils.FXMLSceneManager sceneManager) {
        this.sceneManager = sceneManager;
        System.out.println("CustomerOrderDetailController.setSceneManager: SceneManager injected successfully");
    }
    
    public void setOrderService(IOrderService orderService) {
        this.orderService = orderService;
        System.out.println("CustomerOrderDetailController.setOrderService: OrderService injected successfully - Available: " + (orderService != null));
    }
    
    public void setCurrentCustomerId(String customerId) {
        this.currentCustomerId = customerId;
        System.out.println("CustomerOrderDetailController.setCurrentCustomerId: Customer ID set to: " + customerId);
    }


    public void initialize() {
        errorMessageLabel.setText("");
        errorMessageLabel.setVisible(false);
    }

    /**
     * Called to set the Order ID to be displayed.
     * This would typically be called when navigating to this screen.
     */
    public void setOrderIdToLoad(String orderId) {
        this.orderIdToLoad = orderId;
        screenTitleLabel.setText("Order Details - #" + orderId);
        loadOrderDetails();
    }

    private void loadOrderDetails() {
        if (orderIdToLoad == null || orderService == null) {
            AlertHelper.showErrorDialog("Error", "Service Unavailable", "Order ID is missing or order service is unavailable.");
            errorMessageLabel.setText("Cannot load order details.");
            errorMessageLabel.setVisible(true);
            return;
        }

        try {
            this.currentOrder = orderService.getOrderDetails(orderIdToLoad); // Service should load all related entities
            if (currentOrder == null) {
                AlertHelper.showErrorDialog("Not Found", "Order Not Found", "Order with ID " + orderIdToLoad + " could not be found.");
                errorMessageLabel.setText("Order not found.");
                errorMessageLabel.setVisible(true);
                return;
            }
            populateOrderData();

        } catch (SQLException | ResourceNotFoundException e) {
            e.printStackTrace();
            AlertHelper.showErrorDialog("Error Loading Order", "Database/Service Error", "Could not retrieve order details: " + e.getMessage());
             errorMessageLabel.setText("Error loading order: " + e.getMessage());
             errorMessageLabel.setVisible(true);
        }
    }

    private void populateOrderData() {
        orderIdLabel.setText(currentOrder.getOrderId());
        orderDateLabel.setText(currentOrder.getOrderDate().format(DATE_TIME_FORMATTER));
        orderStatusLabel.setText(currentOrder.getOrderStatus().toString().replace("_", " "));
        orderStatusLabel.setStyle("-fx-text-fill: " + getStatusColor(currentOrder.getOrderStatus()) + "; -fx-font-weight: bold;");

        // Delivery Info
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
                rushTimeLabelHeader.setVisible(true); rushTimeLabelHeader.setManaged(true);
                rushTimeText.setText(deliveryInfo.getRequestedRushDeliveryTime().format(DATE_TIME_FORMATTER));
                rushTimeText.setVisible(true); rushTimeText.setManaged(true);
            }
        }

        // Order Items
        orderItemsVBox.getChildren().clear();
        if (currentOrder.getOrderItems() != null) {
            for (OrderItem item : currentOrder.getOrderItems()) {
                 try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/aims/presentation/views/partials/order_item_row.fxml"));
                    Parent itemNode = loader.load();
                    // OrderItemRowController itemCtrl = loader.getController();
                    // itemCtrl.setData(item, false); // false indicates not editable (view only)
                    // For now, simple labels
                    VBox itemBox = new VBox(2);
                    Label title = new Label(item.getProduct().getTitle() + " (x" + item.getQuantity() + ")");
                    title.setStyle("-fx-font-weight: bold;");
                    Label price = new Label(String.format("Price/unit: %,.0f VND, Total: %,.0f VND",
                                            item.getPriceAtTimeOfOrder(), // Giá đã lưu lúc đặt hàng
                                            item.getPriceAtTimeOfOrder() * item.getQuantity()));
                    itemBox.getChildren().addAll(title, price);
                    orderItemsVBox.getChildren().add(itemBox);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        // Pricing
        subtotalExclVATLabel.setText(String.format("%,.0f VND", currentOrder.getTotalProductPriceExclVAT()));
        float vatAmount = currentOrder.getTotalProductPriceInclVAT() - currentOrder.getTotalProductPriceExclVAT();
        vatLabel.setText(String.format("%,.0f VND", vatAmount));
        subtotalInclVATLabel.setText(String.format("%,.0f VND", currentOrder.getTotalProductPriceInclVAT()));
        shippingFeeLabel.setText(String.format("%,.0f VND", currentOrder.getCalculatedDeliveryFee()));
        totalAmountPaidLabel.setText(String.format("%,.0f VND", currentOrder.getTotalAmountPaid()));

        // Payment Transactions
        paymentTransactionsVBox.getChildren().clear();
        List<PaymentTransaction> transactions = currentOrder.getPaymentTransactions();
        if (transactions != null && !transactions.isEmpty()) {
            for (PaymentTransaction pt : transactions) {
                VBox ptBox = new VBox(3);
                ptBox.setStyle("-fx-border-color: #cccccc; -fx-padding: 5;");
                Label type = new Label("Type: " + pt.getTransactionType() + (pt.getExternalTransactionId()!=null ? " (Gateway ID: "+pt.getExternalTransactionId()+")" : " (AIMS ID: "+pt.getTransactionId()+")"));
                Label status = new Label("Status: " + pt.getTransactionStatus());
                Label amountTime = new Label(String.format("Amount: %,.0f VND on %s", pt.getAmount(), pt.getTransactionDateTime().format(DATE_TIME_FORMATTER)));
                Text content = new Text("Details: " + (pt.getTransactionContent() != null ? pt.getTransactionContent() : "N/A"));
                content.setWrappingWidth(400);
                ptBox.getChildren().addAll(type, status, amountTime, content);
                paymentTransactionsVBox.getChildren().add(ptBox);
            }
        } else {
            paymentTransactionsVBox.getChildren().add(new Label("No payment transaction details available."));
        }


        // Cancel Button Visibility
        // "Customers can choose to cancel the order when viewing the order information before the order is approved."
        if (currentOrder.getOrderStatus() == OrderStatus.PENDING_PROCESSING ||
            currentOrder.getOrderStatus() == OrderStatus.PENDING_PAYMENT || // Might allow cancel if stuck before payment
            currentOrder.getOrderStatus() == OrderStatus.PENDING_DELIVERY_INFO) { // If payment was somehow bypassed or failed and reset
            cancelOrderButton.setVisible(true);
            cancelOrderButton.setManaged(true);
        } else {
            cancelOrderButton.setVisible(false);
            cancelOrderButton.setManaged(false);
        }
    }

    private String getStatusColor(OrderStatus status) {
        if (status == null) return "black";
        return switch (status) {
            case PENDING_PROCESSING, PENDING_PAYMENT, PENDING_DELIVERY_INFO -> "orange";
            case APPROVED, SHIPPING, DELIVERED -> "green";
            case CANCELLED, REJECTED, PAYMENT_FAILED, ERROR_STOCK_UPDATE_FAILED -> "red";
            default -> "black";
        };
    }

    @FXML
    void handleCancelOrderAction(ActionEvent event) {
        if (currentOrder == null || orderService == null) {
            AlertHelper.showErrorDialog("Error", "Unavailable Data", "Order data or service is unavailable for cancellation.");
            return;
        }

        boolean confirmed = AlertHelper.showConfirmationDialog("Cancel Order",
                "Are you sure you want to cancel order #" + currentOrder.getOrderId() + "? This action cannot be undone.");
        if (!confirmed) {
            return;
        }
        // System.out.println("Attempting to cancel order: " + currentOrder.getOrderId());

        // TODO: Get actual currentCustomerId (e.g., from a session manager or if this screen requires login)
        // For now, using the one set via setCurrentCustomerId or a placeholder if not set
        String customerIdForAuth = this.currentCustomerId != null ? this.currentCustomerId : "customer_placeholder_id";
        if ("customer_placeholder_id".equals(customerIdForAuth) && currentOrder.getUserAccount() != null) {
            customerIdForAuth = currentOrder.getUserAccount().getUserId(); // Fallback if not set directly
        }


        try {
            orderService.cancelOrder(currentOrder.getOrderId(), customerIdForAuth);
            AlertHelper.showInformationDialog("Order Cancelled", "Order #" + currentOrder.getOrderId() + " has been successfully cancelled.");
            loadOrderDetails(); // Refresh to show updated status
        } catch (Exception e) { // Catch general exception for now
            e.printStackTrace();
            AlertHelper.showErrorDialog("Cancellation Failed", "Error During Cancellation", "Could not cancel the order: " + e.getMessage());
            errorMessageLabel.setText("Cancellation failed: " + e.getMessage());
            errorMessageLabel.setVisible(true);
        }
    }

    @FXML
    void handleBackToHomeAction(ActionEvent event) {
        System.out.println("Back to Home action triggered");
        if (sceneManager != null && mainLayoutController != null) {
            mainLayoutController.loadContent(FXMLPaths.HOME_SCREEN); // Using FXMLPaths constant
            mainLayoutController.setHeaderTitle("AIMS Home");
        }
    }
}