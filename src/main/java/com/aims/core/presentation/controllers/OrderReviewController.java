package com.aims.core.presentation.controllers;

import com.aims.core.application.services.IOrderService;
import com.aims.core.entities.OrderEntity;
import com.aims.core.entities.OrderItem;
import com.aims.core.entities.DeliveryInfo;
import com.aims.core.entities.PaymentTransaction;
import com.aims.core.enums.OrderStatus;
// import com.aims.presentation.utils.AlertHelper;
// import com.aims.presentation.utils.FXMLSceneManager;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

import java.io.IOException;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class OrderReviewController {

    @FXML private Label screenTitleLabel;
    @FXML private Label orderIdLabel;
    @FXML private Label orderDateLabel;
    @FXML private Label orderStatusLabel;
    @FXML private Text customerNameText;
    @FXML private Text phoneText;
    @FXML private Text emailText;
    @FXML private Text addressText;
    @FXML private Text deliveryMethodText;
    @FXML private VBox orderItemsVBox;
    @FXML private Label subtotalExclVATLabel;
    @FXML private Label vatLabel;
    @FXML private Label subtotalInclVATLabel;
    @FXML private Label shippingFeeLabel;
    @FXML private Label totalAmountPaidLabel;
    @FXML private VBox paymentTransactionsVBox;
    @FXML private TextArea rejectionReasonArea;
    @FXML private Button approveOrderButton;
    @FXML private Button rejectOrderButton;
    @FXML private Label errorMessageLabel;
    @FXML private VBox managerActionBox;


    // @Inject
    private IOrderService orderService;
    private MainLayoutController mainLayoutController;
    private com.aims.core.presentation.utils.FXMLSceneManager sceneManager;

    private OrderEntity currentOrderToReview;
    private String orderIdToLoad;
    private String currentManagerId; // Important for auditing and service calls

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public OrderReviewController() {
        // orderService = new OrderServiceImpl(...); // DI
    }

    public void setMainLayoutController(MainLayoutController mainLayoutController) {
        this.mainLayoutController = mainLayoutController;
        System.out.println("OrderReviewController.setMainLayoutController: MainLayoutController injected successfully");
    }
    
    public void setSceneManager(com.aims.core.presentation.utils.FXMLSceneManager sceneManager) {
        this.sceneManager = sceneManager;
        System.out.println("OrderReviewController.setSceneManager: SceneManager injected successfully");
    }
    
    public void setOrderService(IOrderService orderService) {
        this.orderService = orderService;
        System.out.println("OrderReviewController.setOrderService: OrderService injected successfully - Available: " + (orderService != null));
    }

    public void setCurrentManagerId(String managerId) {
        this.currentManagerId = managerId;
    }

    public void initialize() {
        setErrorMessage("", false);
    }

    /**
     * Called to set the Order ID to be reviewed.
     */
    public void setOrderToReview(String orderId) {
        this.orderIdToLoad = orderId;
        screenTitleLabel.setText("Review Order - #" + orderId);
        loadOrderDetailsForReview();
    }

    private void loadOrderDetailsForReview() {
        if (orderIdToLoad == null || orderService == null) {
            setErrorMessage("Order ID missing or service unavailable.", true);
            disableActions();
            return;
        }

        try {
            this.currentOrderToReview = orderService.getOrderDetails(orderIdToLoad); // Service loads all related entities
            if (currentOrderToReview == null) {
                setErrorMessage("Order #" + orderIdToLoad + " not found.", true);
                disableActions();
                return;
            }
            populateOrderUIData();
            // Enable/disable action buttons based on current status
            if (currentOrderToReview.getOrderStatus() == OrderStatus.PENDING_PROCESSING) {
                managerActionBox.setDisable(false);
            } else {
                managerActionBox.setDisable(true);
                rejectionReasonArea.setText("This order is not pending processing and cannot be approved/rejected here. Current status: " + currentOrderToReview.getOrderStatus());
                rejectionReasonArea.setEditable(false);
            }

        } catch (Exception e) { // Catch SQLException, ResourceNotFoundException
            e.printStackTrace();
            setErrorMessage("Error loading order details: " + e.getMessage(), true);
            disableActions();
        }
    }

    private void disableActions() {
        managerActionBox.setDisable(true);
    }

    private void populateOrderUIData() {
        orderIdLabel.setText(currentOrderToReview.getOrderId());
        orderDateLabel.setText(currentOrderToReview.getOrderDate().format(DATE_TIME_FORMATTER));
        orderStatusLabel.setText(currentOrderToReview.getOrderStatus().toString().replace("_", " "));
        orderStatusLabel.setStyle("-fx-text-fill: " + getStatusColor(currentOrderToReview.getOrderStatus()) + "; -fx-font-weight: bold;");

        DeliveryInfo deliveryInfo = currentOrderToReview.getDeliveryInfo();
        if (deliveryInfo != null) {
            customerNameText.setText(deliveryInfo.getRecipientName() + (currentOrderToReview.getUserAccount() != null ? " (User ID: " + currentOrderToReview.getUserAccount().getUserId() + ")" : " (Guest)"));
            phoneText.setText(deliveryInfo.getPhoneNumber());
            emailText.setText(deliveryInfo.getEmail());
            addressText.setText(deliveryInfo.getDeliveryAddress() + ", " + deliveryInfo.getDeliveryProvinceCity());
            deliveryMethodText.setText(deliveryInfo.getDeliveryMethodChosen());
        } else {
             customerNameText.setText("N/A");
        }

        orderItemsVBox.getChildren().clear();
        if (currentOrderToReview.getOrderItems() != null) {
            for (OrderItem item : currentOrderToReview.getOrderItems()) {
                 try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/aims/presentation/views/partials/order_item_row.fxml"));
                    Parent itemNode = loader.load();
                    // OrderItemRowController itemCtrl = loader.getController();
                    // itemCtrl.setData(item, false); // false for view only
                    // Simple display:
                    VBox itemBox = new VBox(2);
                    Label title = new Label(item.getProduct().getTitle() + " (x" + item.getQuantity() + ")");
                    title.setStyle("-fx-font-weight: bold;");
                    Label price = new Label(String.format("Price/unit: %,.0f VND, Total: %,.0f VND",
                                            item.getPriceAtTimeOfOrder(),
                                            item.getPriceAtTimeOfOrder() * item.getQuantity()));
                    itemBox.getChildren().addAll(title, price);
                    orderItemsVBox.getChildren().add(itemBox);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        subtotalExclVATLabel.setText(String.format("%,.0f VND", currentOrderToReview.getTotalProductPriceExclVAT()));
        float vatAmount = currentOrderToReview.getTotalProductPriceInclVAT() - currentOrderToReview.getTotalProductPriceExclVAT();
        vatLabel.setText(String.format("%,.0f VND", vatAmount));
        subtotalInclVATLabel.setText(String.format("%,.0f VND", currentOrderToReview.getTotalProductPriceInclVAT()));
        shippingFeeLabel.setText(String.format("%,.0f VND", currentOrderToReview.getCalculatedDeliveryFee()));
        totalAmountPaidLabel.setText(String.format("%,.0f VND", currentOrderToReview.getTotalAmountPaid()));

        paymentTransactionsVBox.getChildren().clear();
        List<PaymentTransaction> transactions = currentOrderToReview.getPaymentTransactions();
        if (transactions != null && !transactions.isEmpty()) {
            for (PaymentTransaction pt : transactions) {
                VBox ptBox = new VBox(3);
                ptBox.setStyle("-fx-border-color: #cccccc; -fx-padding: 5;");
                Label type = new Label("Type: " + pt.getTransactionType() + (pt.getExternalTransactionId()!=null ? " (Gateway ID: "+pt.getExternalTransactionId()+")" : " (AIMS ID: "+pt.getTransactionId()+")"));
                Label status = new Label("Status: " + pt.getTransactionStatus());
                Label amountTime = new Label(String.format("Amount: %,.0f VND on %s", pt.getAmount(), pt.getTransactionDateTime().format(DATE_TIME_FORMATTER)));
                ptBox.getChildren().addAll(type, status, amountTime);
                paymentTransactionsVBox.getChildren().add(ptBox);
            }
        } else {
            paymentTransactionsVBox.getChildren().add(new Label("No payment transaction details found."));
        }
    }
    
    private String getStatusColor(OrderStatus status) {
        if (status == null) return "black";
        return switch (status) {
            case PENDING_PROCESSING -> "orange";
            case APPROVED, SHIPPING, DELIVERED -> "green";
            case CANCELLED, REJECTED, PAYMENT_FAILED, ERROR_STOCK_UPDATE_FAILED -> "red";
            default -> "black";
        };
    }

    @FXML
    void handleApproveOrderAction(ActionEvent event) {
        if (currentOrderToReview == null || orderService == null || currentManagerId == null) {
            setErrorMessage("Cannot approve order. Data or service missing.", true);
            return;
        }
        setErrorMessage("", false);
        // boolean confirmed = AlertHelper.showConfirmationDialog("Approve Order", "Are you sure you want to approve order #" + currentOrderToReview.getOrderId() + "?");
        // if (!confirmed) return;
        System.out.println("Attempting to approve order: " + currentOrderToReview.getOrderId()); // Replace with actual confirmation

        try {
            orderService.approveOrder(currentOrderToReview.getOrderId(), currentManagerId);
            // AlertHelper.showInfoAlert("Order Approved", "Order #" + currentOrderToReview.getOrderId() + " has been approved.");
            loadOrderDetailsForReview(); // Refresh to show new status and disable buttons
        } catch (Exception e) { // Catch SQLException, ResourceNotFoundException, ValidationException, InventoryException
            e.printStackTrace();
            // AlertHelper.showErrorAlert("Approval Failed", "Could not approve order: " + e.getMessage());
            setErrorMessage("Approval failed: " + e.getMessage(), true);
        }
    }

    @FXML
    void handleRejectOrderAction(ActionEvent event) {
        if (currentOrderToReview == null || orderService == null || currentManagerId == null) {
            setErrorMessage("Cannot reject order. Data or service missing.", true);
            return;
        }
        String reason = rejectionReasonArea.getText();
        if (reason == null || reason.trim().isEmpty()) {
            setErrorMessage("Rejection reason is required.", true);
            // AlertHelper.showWarningAlert("Reason Required", "Please provide a reason for rejecting the order.");
            return;
        }
        setErrorMessage("", false);

        // boolean confirmed = AlertHelper.showConfirmationDialog("Reject Order",
        //         "Are you sure you want to reject order #" + currentOrderToReview.getOrderId() + "?\nReason: " + reason);
        // if (!confirmed) return;
         System.out.println("Attempting to reject order: " + currentOrderToReview.getOrderId()); // Replace with actual confirmation


        try {
            orderService.rejectOrder(currentOrderToReview.getOrderId(), currentManagerId, reason);
            // AlertHelper.showInfoAlert("Order Rejected", "Order #" + currentOrderToReview.getOrderId() + " has been rejected.");
            loadOrderDetailsForReview(); // Refresh
        } catch (Exception e) { // Catch SQLException, ResourceNotFoundException, ValidationException, PaymentException
            e.printStackTrace();
            // AlertHelper.showErrorAlert("Rejection Failed", "Could not reject the order: " + e.getMessage());
            setErrorMessage("Rejection failed: " + e.getMessage(), true);
        }
    }

    @FXML
    void handleBackToListAction(ActionEvent event) {
        System.out.println("Back to Pending Orders List action triggered");
        // if (sceneManager != null && mainLayoutController != null) {
        //     mainLayoutController.loadContent(FXMLSceneManager.PM_PENDING_ORDERS_LIST_SCREEN);
        //     mainLayoutController.setHeaderTitle("Pending Orders Review");
        // }
    }

    private void setErrorMessage(String message, boolean isError) {
        errorMessageLabel.setText(message);
        errorMessageLabel.setVisible(message != null && !message.isEmpty());
        errorMessageLabel.setManaged(message != null && !message.isEmpty());
        errorMessageLabel.setStyle(isError ? "-fx-text-fill: red;" : "-fx-text-fill: green;");
    }
}