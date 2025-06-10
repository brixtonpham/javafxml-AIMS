package com.aims.core.presentation.controllers;

import com.aims.core.application.dtos.CartItemDTO; // Giả sử có để hiển thị tóm tắt
import com.aims.core.application.dtos.DeliveryInfoDTO;
import com.aims.core.application.services.IOrderService;
import com.aims.core.application.services.IDeliveryCalculationService; // Để kiểm tra địa chỉ giao nhanh
import com.aims.core.entities.OrderEntity; // Service có thể trả về OrderEntity
import com.aims.core.entities.DeliveryInfo; // Để tạo đối tượng gửi đi
import com.aims.core.entities.OrderItem;
import com.aims.core.entities.Product;
// import com.aims.presentation.utils.AlertHelper;
// import com.aims.presentation.utils.FXMLSceneManager;
import com.aims.core.shared.exceptions.ValidationException;
import com.aims.core.shared.exceptions.ResourceNotFoundException;


import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class DeliveryInfoScreenController {

    @FXML
    private TextField nameField;
    @FXML
    private TextField emailField;
    @FXML
    private TextField phoneField;
    @FXML
    private ComboBox<String> provinceCityComboBox;
    @FXML
    private TextArea addressArea;
    @FXML
    private TextArea instructionsArea;
    @FXML
    private CheckBox rushOrderCheckBox;
    @FXML
    private VBox rushOrderDetailsBox;
    @FXML
    private DatePicker rushDeliveryDatePicker;
    @FXML
    private ComboBox<String> rushDeliveryTimeComboBox;
    @FXML
    private Label rushOrderEligibilityLabel;

    @FXML
    private VBox orderItemsVBox; // Để hiển thị danh sách sản phẩm tóm tắt
    @FXML
    private Label subtotalLabel;
    @FXML
    private Label shippingFeeLabel;
    @FXML
    private Label vatLabel;
    @FXML
    private Label totalAmountLabel;
    @FXML
    private Label errorMessageLabel;
    @FXML
    private Button proceedToPaymentButton;


    // --- Service Dependencies (cần được inject) ---
    // @Inject
    private IOrderService orderService;
    // @Inject
    private IDeliveryCalculationService deliveryService; // Để kiểm tra điều kiện giao hàng nhanh
    private MainLayoutController mainLayoutController;
    // private FXMLSceneManager sceneManager;

    private OrderEntity currentOrder; // Đơn hàng đang được xử lý, nhận từ bước trước
    private static final float VAT_RATE = 0.10f;

    public DeliveryInfoScreenController() {
        // Khởi tạo service (trong thực tế dùng DI)
        // orderService = new OrderServiceImpl(...);
        // deliveryService = new DeliveryCalculationServiceImpl(...);
    }

    public void setMainLayoutController(MainLayoutController mainLayoutController) {
        this.mainLayoutController = mainLayoutController;
    }
    
    public void setOrderService(IOrderService orderService) {
        this.orderService = orderService;
    }
    
    public void setDeliveryService(IDeliveryCalculationService deliveryService) {
        this.deliveryService = deliveryService;
    }

    public void initialize() {
        // TODO: Load danh sách tỉnh/thành vào provinceCityComboBox (từ DB hoặc config)
        provinceCityComboBox.setItems(FXCollections.observableArrayList("Hanoi", "Ho Chi Minh City", "Da Nang", "Other"));
        provinceCityComboBox.setOnAction(event -> calculateAndUpdateShippingFee());

        addressArea.textProperty().addListener((obs, oldVal, newVal) -> calculateAndUpdateShippingFee());

        // Setup time slots for rush delivery
        List<String> timeSlots = new ArrayList<>();
        for (int i = 8; i < 20; i += 2) { // Example: 8 AM to 8 PM, 2-hour slots
            timeSlots.add(String.format("%02d:00 - %02d:00", i, i + 2));
        }
        rushDeliveryTimeComboBox.setItems(FXCollections.observableArrayList(timeSlots));
        rushDeliveryDatePicker.setValue(LocalDate.now()); // Default to today

        // Ẩn thông báo lỗi ban đầu
        errorMessageLabel.setText("");
        errorMessageLabel.setVisible(false);
        proceedToPaymentButton.setDisable(true); // Disable cho đến khi tính được phí
    }

    /**
     * Được gọi từ controller trước đó để truyền thông tin đơn hàng đang xử lý
     */
    public void setOrderData(OrderEntity order) {
        this.currentOrder = order;
        if (currentOrder != null) {
            populateOrderSummary();
            calculateAndUpdateShippingFee(); // Tính phí lần đầu khi có đơn hàng
             // Pre-fill form if user has saved delivery info
            // if (order.getDeliveryInfo() != null) { // This would be if editing
            //     populateForm(order.getDeliveryInfo());
            // }
        } else {
            // AlertHelper.showErrorAlert("Error", "No order data received to proceed.");
            System.err.println("DeliveryInfoScreen: No order data received.");
            // Disable form or navigate back
        }
    }

    private void populateOrderSummary() {
        orderItemsVBox.getChildren().clear();
        if (currentOrder.getOrderItems() != null) {
            for (OrderItem item : currentOrder.getOrderItems()) {
                Product product = item.getProduct();
                Label itemLabel = new Label(String.format("%s (x%d) - %,.0f VND",
                        product.getTitle(), item.getQuantity(), item.getPriceAtTimeOfOrder() * item.getQuantity()));
                itemLabel.setWrapText(true);
                orderItemsVBox.getChildren().add(itemLabel);
            }
        }
        subtotalLabel.setText(String.format("Subtotal (excl. VAT): %,.0f VND", currentOrder.getTotalProductPriceExclVAT()));
        float vatAmount = currentOrder.getTotalProductPriceExclVAT() * VAT_RATE;
        vatLabel.setText(String.format("VAT (10%%): %,.0f VND", vatAmount));
    }

    private void populateForm(DeliveryInfo deliveryInfo) {
        nameField.setText(deliveryInfo.getRecipientName());
        emailField.setText(deliveryInfo.getEmail());
        phoneField.setText(deliveryInfo.getPhoneNumber());
        provinceCityComboBox.setValue(deliveryInfo.getDeliveryProvinceCity());
        addressArea.setText(deliveryInfo.getDeliveryAddress());
        instructionsArea.setText(deliveryInfo.getDeliveryInstructions());
        if ("RUSH_DELIVERY".equalsIgnoreCase(deliveryInfo.getDeliveryMethodChosen())) {
            rushOrderCheckBox.setSelected(true);
            handleRushOrderToggle(null); // To show details box and populate
            if (deliveryInfo.getRequestedRushDeliveryTime() != null) {
                rushDeliveryDatePicker.setValue(deliveryInfo.getRequestedRushDeliveryTime().toLocalDate());
                // TODO: Set time slot in ComboBox based on LocalDateTime
            }
        }
    }

    @FXML
    void handleRushOrderToggle(ActionEvent event) {
        boolean selected = rushOrderCheckBox.isSelected();
        rushOrderDetailsBox.setVisible(selected);
        rushOrderDetailsBox.setManaged(selected);
        rushOrderEligibilityLabel.setText(""); // Clear previous message

        if (selected) {
            DeliveryInfo tempDeliveryInfo = buildDeliveryInfoFromForm(false); // Build without rush time first
            if (deliveryService != null && !deliveryService.isRushDeliveryAddressEligible(tempDeliveryInfo)) {
                rushOrderEligibilityLabel.setText("Warning: Your selected address may not be eligible for rush delivery. It's available for inner Hanoi districts only.");
            } else if (deliveryService == null) {
                 rushOrderEligibilityLabel.setText("Warning: Could not verify rush delivery eligibility (service unavailable).");
            }
            // Check if any item is eligible (this logic should be more robust based on product properties)
            boolean anyItemSupportsRush = currentOrder.getOrderItems().stream().anyMatch(OrderItem::isEligibleForRushDelivery);
            if (!anyItemSupportsRush) {
                rushOrderEligibilityLabel.setText(rushOrderEligibilityLabel.getText() + "\nWarning: No items in your order are eligible for rush delivery.");
                rushOrderCheckBox.setSelected(false); // Uncheck if no items eligible
                rushOrderDetailsBox.setVisible(false);
                rushOrderDetailsBox.setManaged(false);
            }
        }
        calculateAndUpdateShippingFee();
    }

    @FXML
    void handleProvinceCityChange(ActionEvent event){
        // Khi tỉnh thành thay đổi, kiểm tra lại điều kiện giao nhanh nếu đang chọn
        if(rushOrderCheckBox.isSelected()){
            handleRushOrderToggle(null); // Re-evaluate eligibility
        }
        calculateAndUpdateShippingFee();
    }


    private DeliveryInfo buildDeliveryInfoFromForm(boolean includeRushTime) {
        DeliveryInfo info = new DeliveryInfo();
        info.setRecipientName(nameField.getText());
        info.setEmail(emailField.getText());
        info.setPhoneNumber(phoneField.getText());
        info.setDeliveryProvinceCity(provinceCityComboBox.getValue());
        info.setDeliveryAddress(addressArea.getText());
        info.setDeliveryInstructions(instructionsArea.getText());
        info.setDeliveryMethodChosen(rushOrderCheckBox.isSelected() ? "RUSH_DELIVERY" : "STANDARD");

        if (rushOrderCheckBox.isSelected() && includeRushTime) {
            LocalDate date = rushDeliveryDatePicker.getValue();
            String timeSlot = rushDeliveryTimeComboBox.getValue();
            if (date != null && timeSlot != null) {
                try {
                    // Assuming time slot is "HH:00 - HH+2:00", take the start time.
                    int startHour = Integer.parseInt(timeSlot.substring(0, 2));
                    info.setRequestedRushDeliveryTime(LocalDateTime.of(date, LocalTime.of(startHour, 0)));
                } catch (NumberFormatException e) {
                    // AlertHelper.showErrorAlert("Invalid Time", "Please select a valid time slot for rush delivery.");
                    System.err.println("Invalid rush delivery time slot: " + timeSlot);
                    return null; // Indicate error
                }
            } else if (includeRushTime) { // If includeRushTime is true, these fields are required
                 // AlertHelper.showErrorAlert("Missing Information", "Please select date and time for rush delivery.");
                 System.err.println("Missing date/time for rush delivery.");
                 return null; // Indicate error
            }
        }
        return info;
    }

    private void calculateAndUpdateShippingFee() {
        if (currentOrder == null || orderService == null) {
            shippingFeeLabel.setText("Shipping Fee: Error calculating");
            totalAmountLabel.setText("TOTAL AMOUNT: Error");
            proceedToPaymentButton.setDisable(true);
            return;
        }
        // Build a temporary DeliveryInfo from form to pass for calculation
        DeliveryInfo tempDeliveryInfo = buildDeliveryInfoFromForm(false); // Don't need rush time for eligibility check here for fee calc
        if (tempDeliveryInfo.getDeliveryProvinceCity() == null || tempDeliveryInfo.getDeliveryAddress() == null ||
            tempDeliveryInfo.getDeliveryProvinceCity().trim().isEmpty() || tempDeliveryInfo.getDeliveryAddress().trim().isEmpty()) {
            shippingFeeLabel.setText("Shipping Fee: Enter address");
            totalAmountLabel.setText("TOTAL AMOUNT: Enter address");
            proceedToPaymentButton.setDisable(true);
            return;
        }

        try {
            float fee = orderService.calculateShippingFee(currentOrder.getOrderId(), tempDeliveryInfo, rushOrderCheckBox.isSelected());
            shippingFeeLabel.setText(String.format("Shipping Fee: %,.0f VND", fee));
            float totalAmount = currentOrder.getTotalProductPriceInclVAT() + fee;
            totalAmountLabel.setText(String.format("TOTAL AMOUNT: %,.0f VND", totalAmount));
            proceedToPaymentButton.setDisable(false);
            setErrorMessage("", false);
        } catch (SQLException | ResourceNotFoundException | ValidationException e) {
            shippingFeeLabel.setText("Shipping Fee: Error");
            totalAmountLabel.setText("TOTAL AMOUNT: Error");
            // AlertHelper.showErrorAlert("Fee Calculation Error", "Could not calculate shipping fee: " + e.getMessage());
            setErrorMessage("Could not calculate shipping fee: " + e.getMessage(), true);
            proceedToPaymentButton.setDisable(true);
            e.printStackTrace();
        }
    }

    private void setErrorMessage(String message, boolean visible) {
        errorMessageLabel.setText(message);
        errorMessageLabel.setVisible(visible);
        errorMessageLabel.setManaged(visible);
    }


    @FXML
    void handleBackToCartAction(ActionEvent event) {
        System.out.println("Back to Cart action triggered");
        if (mainLayoutController != null) {
            try {
                mainLayoutController.loadContent("/com/aims/presentation/views/cart_screen.fxml");
                mainLayoutController.setHeaderTitle("Your Shopping Cart");
                System.out.println("Successfully navigated back to cart");
            } catch (Exception e) {
                System.err.println("Error navigating back to cart: " + e.getMessage());
                setErrorMessage("Navigation error. Please try again.", true);
            }
        } else {
            System.err.println("MainLayoutController not available for back navigation");
            setErrorMessage("Navigation error. Please refresh the page.", true);
        }
    }

    @FXML
    void handleProceedToPaymentAction(ActionEvent event) {
        if (currentOrder == null || orderService == null) {
            // AlertHelper.showErrorAlert("Error", "Order data is missing.");
            return;
        }
        setErrorMessage("", false);

        DeliveryInfo deliveryInfo = buildDeliveryInfoFromForm(true); // Get full info including rush time
        if (deliveryInfo == null && rushOrderCheckBox.isSelected()){ // Error building DTO, likely missing rush time
            setErrorMessage("Please select date and time if requesting rush delivery.", true);
            return;
        }
        if (deliveryInfo == null) { // General error building DTO
            setErrorMessage("Please ensure all delivery details are correct.", true);
            return;
        }


        try {
            // This call updates the order in the backend with delivery info and new total
            currentOrder = orderService.setDeliveryInformation(currentOrder.getOrderId(), deliveryInfo, rushOrderCheckBox.isSelected());

            System.out.println("Proceed to Payment action triggered for Order ID: " + currentOrder.getOrderId());
            // Navigate to payment method selection screen
            // if (sceneManager != null && mainLayoutController != null) {
            // PaymentMethodScreenController paymentCtrl = (PaymentMethodScreenController) sceneManager.loadFXMLIntoPane(
            // mainLayoutController.getContentPane(), FXMLSceneManager.PAYMENT_METHOD_SCREEN
            // );
            // paymentCtrl.setOrderData(currentOrder); // Pass the updated order
            // paymentCtrl.setMainLayoutController(mainLayoutController);
            // mainLayoutController.setHeaderTitle("Select Payment Method");
            // }

        } catch (SQLException | ResourceNotFoundException | ValidationException e) {
            e.printStackTrace();
            // AlertHelper.showErrorAlert("Error Processing Delivery Info", e.getMessage());
            setErrorMessage("Error: " + e.getMessage(), true);
        }
    }
}