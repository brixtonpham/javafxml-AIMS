package com.aims.core.presentation.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.control.ListView;

import com.aims.core.application.services.IOrderService;
import com.aims.core.application.services.IDeliveryCalculationService;
import com.aims.core.entities.OrderEntity;
import com.aims.core.entities.OrderItem;
import com.aims.core.entities.DeliveryInfo;
import com.aims.core.presentation.controllers.base.MainLayoutController;
import com.aims.core.shared.NavigationService;
import com.aims.core.shared.ServiceFactory;
import com.aims.core.shared.exceptions.ResourceNotFoundException;

import java.util.List;

/**
 * @deprecated Use OrderSummaryController instead. This controller is deprecated due to navigation compatibility issues.
 * The newer OrderSummaryController provides enhanced functionality and better integration with the navigation system.
 */
@Deprecated
public class OrderSummaryScreenController {

    @FXML private Label orderIdLabel;
    @FXML private Label totalItemsLabel;
    @FXML private Label subtotalLabel;
    @FXML private Label deliveryFeeLabel;
    @FXML private Label totalAmountLabel;
    @FXML private ListView<OrderItem> itemsListView;
    @FXML private VBox deliveryInfoBox;
    @FXML private Label deliveryAddressLabel;
    @FXML private Label deliveryMethodLabel;
    @FXML private Label deliveryInstructionsLabel;
    @FXML private Button editDeliveryButton;
    @FXML private Button proceedToPaymentButton;
    @FXML private Button backToCartButton;

    private IOrderService orderService;
    private IDeliveryCalculationService deliveryService;
    private MainLayoutController mainLayoutController;
    private OrderEntity currentOrder;
    
    public OrderSummaryScreenController() {
        this.orderService = ServiceFactory.getOrderService();
        this.deliveryService = ServiceFactory.getDeliveryCalculationService();
    }
    
    public void setMainLayoutController(MainLayoutController controller) {
        this.mainLayoutController = controller;
    }

    public void initData(String orderId) {
        try {
            this.currentOrder = orderService.getOrderById(orderId);
            updateUI();
        } catch (ResourceNotFoundException e) {
            showError("Could not find order details: " + e.getMessage());
        }
    }

    private void updateUI() {
        if (currentOrder == null) {
            showError("No order data available");
            return;
        }

        orderIdLabel.setText("Order #" + currentOrder.getOrderId());
        updateOrderItems();
        updateDeliveryInfo();
        updateTotals();
        
        // Enable/disable buttons based on order state
        boolean hasDeliveryInfo = currentOrder.getDeliveryInfo() != null;
        proceedToPaymentButton.setDisable(!hasDeliveryInfo);
        editDeliveryButton.setVisible(hasDeliveryInfo);
    }

    private void updateOrderItems() {
        List<OrderItem> items = currentOrder.getOrderItems();
        itemsListView.getItems().clear();
        itemsListView.getItems().addAll(items);
        totalItemsLabel.setText(String.format("%d items", items.size()));
    }

    private void updateDeliveryInfo() {
        DeliveryInfo info = currentOrder.getDeliveryInfo();
        if (info != null) {
            deliveryInfoBox.setVisible(true);
            deliveryAddressLabel.setText(formatAddress(info));
            deliveryMethodLabel.setText("Delivery Method: " + 
                (info.isRushDelivery() ? "Rush Delivery" : "Standard Delivery"));
            deliveryInstructionsLabel.setText(info.getDeliveryInstructions());
        } else {
            deliveryInfoBox.setVisible(false);
        }
    }

    private void updateTotals() {
        float subtotal = calculateSubtotal();
        float deliveryFee = currentOrder.getCalculatedDeliveryFee();
        float total = subtotal + deliveryFee;

        subtotalLabel.setText(String.format("%.2f VND", subtotal));
        deliveryFeeLabel.setText(String.format("%.2f VND", deliveryFee));
        totalAmountLabel.setText(String.format("%.2f VND", total));
    }

    private float calculateSubtotal() {
        return currentOrder.getOrderItems().stream()
            .map(item -> item.getPriceAtTimeOfOrder() * item.getQuantity())
            .reduce(0f, Float::sum);
    }

    private String formatAddress(DeliveryInfo info) {
        return String.format("%s\n%s, %s", 
            info.getStreetAddress(),
            info.getDistrict(),
            info.getCity()
        );
    }

    @FXML
    private void handleProceedToPayment() {
        NavigationService.navigateToPaymentMethod(currentOrder.getOrderId());
    }

    @FXML
    private void handleEditDelivery() {
        NavigationService.navigateTo("delivery_info_screen.fxml", mainLayoutController, controller -> {
            DeliveryInfoScreenController deliveryController = (DeliveryInfoScreenController) controller;
            deliveryController.initData(currentOrder);
        });
    }

    @FXML
    private void handleBackToCart() {
        NavigationService.navigateTo("cart_screen.fxml", mainLayoutController, null);
    }

    private void showError(String message) {
        // Show error dialog or message
        NavigationService.showDialog("error_dialog.fxml", controller -> {
            ((com.aims.core.presentation.controllers.dialogs.ErrorDialogController) controller).setErrorMessage(message);
        });
    }
}