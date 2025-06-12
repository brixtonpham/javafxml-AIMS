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

public class OrderSummaryController implements MainLayoutController.IChildController {

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

    @Override
    public void setMainLayoutController(MainLayoutController mainLayoutController) {
        this.mainLayoutController = mainLayoutController;
        System.out.println("OrderSummaryController: MainLayoutController injected successfully");
    }
    
    // public void setSceneManager(FXMLSceneManager sceneManager) { this.sceneManager = sceneManager; }

    public void initialize() {
        System.out.println("OrderSummaryController.initialize: Starting initialization");
        
        try {
            errorMessageLabel.setText("");
            errorMessageLabel.setVisible(false);
            
            // Validate critical UI components
            if (orderIdLabel == null || orderDateLabel == null) {
                System.err.println("OrderSummaryController.initialize: CRITICAL - Essential UI components are null");
                showNavigationError("Screen initialization failed. Please reload the page.");
                return;
            }
            
            if (proceedToPaymentMethodButton == null) {
                System.err.println("OrderSummaryController.initialize: CRITICAL - Payment button is null");
                showNavigationError("Payment functionality is not available. Please reload the page.");
                return;
            }
            
            System.out.println("OrderSummaryController.initialize: Initialization completed successfully");
            
        } catch (Exception e) {
            System.err.println("OrderSummaryController.initialize: CRITICAL initialization error: " + e.getMessage());
            e.printStackTrace();
            showNavigationError("Failed to initialize order summary screen. Please try refreshing the page.");
        }
    }

    /**
     * Được gọi từ controller trước (DeliveryInfoScreenController) để truyền dữ liệu đơn hàng.
     * Enhanced with comprehensive validation and error handling.
     */
    public void setOrderData(OrderEntity order) {
        System.out.println("OrderSummaryController.setOrderData: Setting order data");
        
        try {
            this.currentOrder = order;
            
            if (currentOrder != null) {
                // Validate order data integrity
                if (validateOrderData(currentOrder)) {
                    populateOrderSummary();
                    proceedToPaymentMethodButton.setDisable(false);
                    System.out.println("OrderSummaryController.setOrderData: Order data set successfully for Order ID: " + currentOrder.getOrderId());
                } else {
                    System.err.println("OrderSummaryController.setOrderData: Order data validation failed");
                    showNavigationError("Order data is incomplete or invalid. Please return to previous step and verify your information.");
                    proceedToPaymentMethodButton.setDisable(true);
                }
            } else {
                System.err.println("OrderSummaryController.setOrderData: No order data received");
                showNavigationError("Order information is not available. Please restart the order process.");
                
                // Graceful degradation
                if (orderIdLabel != null) {
                    orderIdLabel.setText("Error: Order data missing");
                }
                if (proceedToPaymentMethodButton != null) {
                    proceedToPaymentMethodButton.setDisable(true);
                }
            }
            
        } catch (Exception e) {
            System.err.println("OrderSummaryController.setOrderData: Error setting order data: " + e.getMessage());
            e.printStackTrace();
            showNavigationError("Failed to process order information: " + e.getMessage());
            if (proceedToPaymentMethodButton != null) {
                proceedToPaymentMethodButton.setDisable(true);
            }
        }
    }
    
    /**
     * Validates order data integrity before display.
     *
     * @param order The order to validate
     * @return true if order data is valid, false otherwise
     */
    private boolean validateOrderData(OrderEntity order) {
        if (order == null) {
            System.err.println("OrderSummaryController.validateOrderData: Order is null");
            return false;
        }
        
        if (order.getOrderId() == null || order.getOrderId().trim().isEmpty()) {
            System.err.println("OrderSummaryController.validateOrderData: Order ID is missing");
            return false;
        }
        
        if (order.getOrderDate() == null) {
            System.err.println("OrderSummaryController.validateOrderData: Order date is missing");
            return false;
        }
        
        if (order.getDeliveryInfo() == null) {
            System.err.println("OrderSummaryController.validateOrderData: Delivery information is missing");
            return false;
        }
        
        if (order.getOrderItems() == null || order.getOrderItems().isEmpty()) {
            System.err.println("OrderSummaryController.validateOrderData: Order items are missing");
            return false;
        }
        
        if (order.getTotalAmountPaid() <= 0) {
            System.err.println("OrderSummaryController.validateOrderData: Total amount is invalid");
            return false;
        }
        
        System.out.println("OrderSummaryController.validateOrderData: Order data validation passed");
        return true;
    }

    private void populateOrderSummary() {
        System.out.println("OrderSummaryController.populateOrderSummary: Starting to populate order summary");
        
        try {
            // Populate order basic information with null checks
            if (orderIdLabel != null) {
                orderIdLabel.setText(currentOrder.getOrderId() != null ? currentOrder.getOrderId() : "N/A");
            }
            
            if (orderDateLabel != null) {
                if (currentOrder.getOrderDate() != null) {
                    orderDateLabel.setText(currentOrder.getOrderDate().format(DATE_TIME_FORMATTER));
                } else {
                    orderDateLabel.setText("N/A");
                }
            }

            // Populate delivery information with enhanced error handling
            populateDeliveryInformation();
            
            // Populate order items with enhanced error handling
            populateOrderItems();
            
            // Populate pricing information with validation
            populatePricingInformation();
            
            System.out.println("OrderSummaryController.populateOrderSummary: Order summary populated successfully");
            
        } catch (Exception e) {
            System.err.println("OrderSummaryController.populateOrderSummary: Error populating order summary: " + e.getMessage());
            e.printStackTrace();
            showNavigationError("Failed to display order information properly: " + e.getMessage());
        }
    }
    
    /**
     * Populates delivery information section with error handling.
     */
    private void populateDeliveryInformation() {
        try {
            DeliveryInfo deliveryInfo = currentOrder.getDeliveryInfo();
            if (deliveryInfo != null) {
                if (recipientNameText != null) {
                    recipientNameText.setText(deliveryInfo.getRecipientName() != null ? deliveryInfo.getRecipientName() : "N/A");
                }
                if (phoneText != null) {
                    phoneText.setText(deliveryInfo.getPhoneNumber() != null ? deliveryInfo.getPhoneNumber() : "N/A");
                }
                if (emailText != null) {
                    emailText.setText(deliveryInfo.getEmail() != null ? deliveryInfo.getEmail() : "N/A");
                }
                if (addressText != null) {
                    addressText.setText(deliveryInfo.getDeliveryAddress() != null ? deliveryInfo.getDeliveryAddress() : "N/A");
                }
                if (provinceCityText != null) {
                    provinceCityText.setText(deliveryInfo.getDeliveryProvinceCity() != null ? deliveryInfo.getDeliveryProvinceCity() : "N/A");
                }
                if (instructionsText != null) {
                    instructionsText.setText(deliveryInfo.getDeliveryInstructions() != null ? deliveryInfo.getDeliveryInstructions() : "N/A");
                }
                if (deliveryMethodText != null) {
                    deliveryMethodText.setText(deliveryInfo.getDeliveryMethodChosen() != null ? deliveryInfo.getDeliveryMethodChosen() : "N/A");
                }

                // Handle rush delivery display
                boolean isRushDelivery = "RUSH_DELIVERY".equalsIgnoreCase(deliveryInfo.getDeliveryMethodChosen()) &&
                                       deliveryInfo.getRequestedRushDeliveryTime() != null;
                
                if (rushTimeLabel != null) {
                    rushTimeLabel.setVisible(isRushDelivery);
                    rushTimeLabel.setManaged(isRushDelivery);
                }
                
                if (rushTimeText != null) {
                    if (isRushDelivery) {
                        rushTimeText.setText(deliveryInfo.getRequestedRushDeliveryTime().format(DATE_TIME_FORMATTER));
                    }
                    rushTimeText.setVisible(isRushDelivery);
                    rushTimeText.setManaged(isRushDelivery);
                }
            } else {
                System.err.println("OrderSummaryController.populateDeliveryInformation: Delivery info is null");
                showNavigationError("Delivery information is missing from the order");
            }
        } catch (Exception e) {
            System.err.println("OrderSummaryController.populateDeliveryInformation: Error populating delivery info: " + e.getMessage());
            throw e; // Re-throw to be handled by parent method
        }
    }
    
    /**
     * Populates order items section with enhanced error handling.
     */
    private void populateOrderItems() {
        try {
            if (orderItemsVBox != null) {
                orderItemsVBox.getChildren().clear();
            } else {
                System.err.println("OrderSummaryController.populateOrderItems: orderItemsVBox is null");
                return;
            }
            
            if (currentOrder.getOrderItems() != null && !currentOrder.getOrderItems().isEmpty()) {
                for (OrderItem item : currentOrder.getOrderItems()) {
                    try {
                        // Attempt to load FXML partial
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/aims/presentation/views/partials/order_item_row.fxml"));
                        Parent itemNode = loader.load();
                        OrderItemRowController itemController = loader.getController();
                        
                        if (itemController != null) {
                            itemController.setData(item);
                            orderItemsVBox.getChildren().add(itemNode);
                        } else {
                            System.err.println("OrderSummaryController.populateOrderItems: ItemController is null, using fallback");
                            createFallbackItemDisplay(item);
                        }

                    } catch (IOException e) {
                        System.err.println("OrderSummaryController.populateOrderItems: Error loading order_item_row.fxml: " + e.getMessage());
                        // Fallback to manual creation
                        createFallbackItemDisplay(item);
                    }
                }
            } else {
                System.err.println("OrderSummaryController.populateOrderItems: No order items found");
                showNavigationError("No items found in the order");
            }
        } catch (Exception e) {
            System.err.println("OrderSummaryController.populateOrderItems: Error populating order items: " + e.getMessage());
            throw e; // Re-throw to be handled by parent method
        }
    }
    
    /**
     * Creates a fallback display for order items when FXML loading fails.
     */
    private void createFallbackItemDisplay(OrderItem item) {
        try {
            VBox itemBox = new VBox(2);
            itemBox.setStyle("-fx-padding: 10; -fx-border-color: #e0e0e0; -fx-border-radius: 5;");
            
            String itemTitle = (item.getProduct() != null && item.getProduct().getTitle() != null) ?
                              item.getProduct().getTitle() : "Unknown Product";
            
            Label title = new Label(itemTitle + " (x" + item.getQuantity() + ")");
            title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
            
            Label price = new Label(String.format("Price/unit: %,.0f VND, Total: %,.0f VND",
                                    item.getPriceAtTimeOfOrder(),
                                    item.getPriceAtTimeOfOrder() * item.getQuantity()));
            price.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");
            
            itemBox.getChildren().addAll(title, price);
            orderItemsVBox.getChildren().add(itemBox);
        } catch (Exception e) {
            System.err.println("OrderSummaryController.createFallbackItemDisplay: Error creating fallback display: " + e.getMessage());
        }
    }
    
    /**
     * Populates pricing information with validation.
     */
    private void populatePricingInformation() {
        try {
            if (subtotalExclVATLabel != null) {
                subtotalExclVATLabel.setText(String.format("%,.0f VND", currentOrder.getTotalProductPriceExclVAT()));
            }
            
            if (vatLabel != null) {
                float vatAmount = currentOrder.getTotalProductPriceInclVAT() - currentOrder.getTotalProductPriceExclVAT();
                vatLabel.setText(String.format("%,.0f VND", vatAmount));
            }
            
            if (subtotalInclVATLabel != null) {
                subtotalInclVATLabel.setText(String.format("%,.0f VND", currentOrder.getTotalProductPriceInclVAT()));
            }
            
            if (shippingFeeLabel != null) {
                shippingFeeLabel.setText(String.format("%,.0f VND", currentOrder.getCalculatedDeliveryFee()));
            }
            
            if (totalAmountPaidLabel != null) {
                totalAmountPaidLabel.setText(String.format("%,.0f VND", currentOrder.getTotalAmountPaid()));
            }
        } catch (Exception e) {
            System.err.println("OrderSummaryController.populatePricingInformation: Error populating pricing: " + e.getMessage());
            throw e; // Re-throw to be handled by parent method
        }
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
        // Input validation
        if (currentOrder == null) {
            System.err.println("OrderSummaryController: No order data to proceed with payment");
            showNavigationError("Order information is not available. Please restart the order process.");
            return;
        }
        
        System.out.println("OrderSummaryController: Proceeding to payment method for Order ID: " + currentOrder.getOrderId());
        
        // Enhanced defensive programming with fallback mechanisms
        if (mainLayoutController != null) {
            try {
                Object controller = mainLayoutController.loadContent("/com/aims/presentation/views/payment_method_screen.fxml");
                mainLayoutController.setHeaderTitle("Select Payment Method");
                
                // Verify controller loading and injection
                if (controller instanceof PaymentMethodScreenController) {
                    PaymentMethodScreenController paymentController = (PaymentMethodScreenController) controller;
                    paymentController.setOrderData(currentOrder);
                    System.out.println("OrderSummaryController: Successfully navigated to payment method selection");
                } else {
                    System.err.println("OrderSummaryController: Controller injection failed - invalid controller type");
                    showNavigationError("Failed to initialize payment screen properly");
                }
                
            } catch (Exception e) {
                System.err.println("OrderSummaryController: Navigation error: " + e.getMessage());
                e.printStackTrace();
                showNavigationError("Failed to load payment method screen: " + e.getMessage());
            }
        } else {
            System.err.println("OrderSummaryController: CRITICAL - MainLayoutController not available for navigation");
            
            // Recovery mechanism: Show error and suggest action
            showNavigationError("Cannot navigate to payment screen due to a system error. Please try refreshing the page or restart the application.");
        }
    }
    
    private void showNavigationError(String details) {
        errorMessageLabel.setText("Navigation Error: " + details);
        errorMessageLabel.setVisible(true);
        errorMessageLabel.setManaged(true);
        proceedToPaymentMethodButton.setDisable(true);
        
        System.err.println("OrderSummaryController: " + details);
    }
}