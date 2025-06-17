package com.aims.core.presentation.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import com.aims.core.application.services.IOrderService;
import com.aims.core.application.services.IDeliveryCalculationService;
import com.aims.core.application.services.IOrderDataLoaderService;
import com.aims.core.application.services.ICartDataValidationService;
import com.aims.core.application.services.IOrderValidationService;
import com.aims.core.application.dtos.OrderSummaryDTO;
import com.aims.core.entities.OrderEntity;
import com.aims.core.entities.DeliveryInfo;
import com.aims.core.entities.OrderItem;
import com.aims.core.presentation.controllers.MainLayoutController;
import com.aims.core.shared.NavigationService;
import com.aims.core.shared.ServiceFactory;
import com.aims.core.shared.exceptions.ValidationException;
import com.aims.core.shared.exceptions.ResourceNotFoundException;
import com.aims.core.utils.DeliveryInfoValidator;
import com.aims.core.utils.EnhancedDeliveryInfoValidator;
import com.aims.core.presentation.utils.RealTimeValidationManager;
import com.aims.core.presentation.utils.FormStateManager;
import com.aims.core.presentation.utils.EnhancedNavigationManager;

import java.sql.SQLException;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import javafx.util.StringConverter;

public class DeliveryInfoScreenController {

    // FXML fields matching the FXML file exactly
    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private ComboBox<String> provinceCityComboBox;
    @FXML private TextArea addressArea;
    @FXML private TextArea instructionsArea;
    @FXML private CheckBox rushOrderCheckBox;
    @FXML private VBox rushOrderDetailsBox;
    @FXML private DatePicker rushDeliveryDatePicker;
    @FXML private ComboBox<String> rushDeliveryTimeComboBox;
    @FXML private Label rushOrderEligibilityLabel;
    @FXML private VBox orderItemsVBox;
    @FXML private Label subtotalLabel;
    @FXML private Label shippingFeeLabel;
    @FXML private Label vatLabel;
    @FXML private Label totalAmountLabel;
    @FXML private Label errorMessageLabel;
    @FXML private Button proceedToPaymentButton;

    // Legacy field references for backward compatibility
    @FXML private TextField recipientNameField;
    @FXML private TextField phoneNumberField;
    @FXML private TextField streetAddressField;
    @FXML private TextField districtField;
    @FXML private TextField cityField;
    @FXML private TextArea deliveryInstructionsArea;
    @FXML private CheckBox rushDeliveryCheckBox;
    @FXML private VBox rushDeliveryTimeBox;
    @FXML private DatePicker deliveryDatePicker;
    @FXML private ComboBox<String> deliveryTimeComboBox;
    @FXML private Button submitButton;
    @FXML private Button backButton;

    private IOrderService orderService;
    private IDeliveryCalculationService deliveryService;
    private IOrderDataLoaderService orderDataLoaderService;
    private ICartDataValidationService cartDataValidationService;
    private IOrderValidationService orderValidationService;
    private MainLayoutController mainLayoutController;
    private OrderEntity currentOrder;
    private boolean isEditing;
    
    // Enhanced logging and validation
    private static final Logger logger = Logger.getLogger(DeliveryInfoScreenController.class.getName());
    private OrderSummaryDTO currentOrderSummaryDTO;
    
    // Enhanced validation and form state management
    private RealTimeValidationManager validationManager;
    private FormStateManager formStateManager;
    private String currentFormSessionId;

    public DeliveryInfoScreenController() {
        this.orderService = ServiceFactory.getOrderService();
        this.deliveryService = ServiceFactory.getDeliveryCalculationService();
        // Enhanced services will be injected by FXMLSceneManager
        initializeEnhancedServices();
    }
    
    /**
     * Initialize enhanced services with dependency injection fallback
     */
    private void initializeEnhancedServices() {
        try {
            ServiceFactory serviceFactory = ServiceFactory.getInstance();
            if (this.orderDataLoaderService == null) {
                this.orderDataLoaderService = serviceFactory.getOrderDataLoaderService();
                logger.info("DeliveryInfoScreenController: OrderDataLoaderService initialized");
            }
            if (this.cartDataValidationService == null) {
                this.cartDataValidationService = serviceFactory.getCartDataValidationService();
                logger.info("DeliveryInfoScreenController: CartDataValidationService initialized");
            }
            if (this.orderValidationService == null) {
                this.orderValidationService = serviceFactory.getOrderValidationService();
                logger.info("DeliveryInfoScreenController: OrderValidationService initialized");
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "DeliveryInfoScreenController: Error initializing enhanced services", e);
        }
    }

    public void setMainLayoutController(MainLayoutController controller) {
        this.mainLayoutController = controller;
    }

    public void setOrderService(IOrderService orderService) {
        this.orderService = orderService;
    }

    public void setDeliveryService(IDeliveryCalculationService deliveryService) {
        this.deliveryService = deliveryService;
    }
    
    /**
     * Enhanced service injection for OrderDataLoaderService
     */
    public void setOrderDataLoaderService(IOrderDataLoaderService orderDataLoaderService) {
        this.orderDataLoaderService = orderDataLoaderService;
        logger.info("DeliveryInfoScreenController: OrderDataLoaderService injected via setter");
    }
    
    /**
     * Enhanced service injection for CartDataValidationService
     */
    public void setCartDataValidationService(ICartDataValidationService cartDataValidationService) {
        this.cartDataValidationService = cartDataValidationService;
        logger.info("DeliveryInfoScreenController: CartDataValidationService injected via setter");
    }
    
    /**
     * Enhanced service injection for DeliveryCalculationService
     */
    public void setDeliveryCalculationService(IDeliveryCalculationService deliveryCalculationService) {
        this.deliveryService = deliveryCalculationService;
        logger.info("DeliveryInfoScreenController: DeliveryCalculationService injected via setter");
    }
    
    /**
     * Enhanced service injection for OrderValidationService
     */
    public void setOrderValidationService(IOrderValidationService orderValidationService) {
        this.orderValidationService = orderValidationService;
        logger.info("DeliveryInfoScreenController: OrderValidationService injected via setter");
    }

    @FXML
    public void initialize() {
        logger.info("DeliveryInfoScreenController.initialize: Starting enhanced initialization with validation and form state management");
        
        try {
            // Initialize province/city combo box with Vietnamese provinces
            if (provinceCityComboBox != null) {
                provinceCityComboBox.getItems().addAll(
                    "Hanoi", "Ho Chi Minh City", "Da Nang", "Hai Phong", "Can Tho",
                    "An Giang", "Ba Ria - Vung Tau", "Bac Giang", "Bac Kan", "Bac Lieu",
                    "Bac Ninh", "Ben Tre", "Binh Dinh", "Binh Duong", "Binh Phuoc",
                    "Binh Thuan", "Ca Mau", "Cao Bang", "Dak Lak", "Dak Nong",
                    "Dien Bien", "Dong Nai", "Dong Thap", "Gia Lai", "Ha Giang",
                    "Ha Nam", "Ha Tinh", "Hai Duong", "Hau Giang", "Hoa Binh",
                    "Hung Yen", "Khanh Hoa", "Kien Giang", "Kon Tum", "Lai Chau",
                    "Lam Dong", "Lang Son", "Lao Cai", "Long An", "Nam Dinh",
                    "Nghe An", "Ninh Binh", "Ninh Thuan", "Phu Tho", "Phu Yen",
                    "Quang Binh", "Quang Nam", "Quang Ngai", "Quang Ninh", "Quang Tri",
                    "Soc Trang", "Son La", "Tay Ninh", "Thai Binh", "Thai Nguyen",
                    "Thanh Hoa", "Thua Thien Hue", "Tien Giang", "Tra Vinh", "Tuyen Quang",
                    "Vinh Long", "Vinh Phuc", "Yen Bai"
                );
            }
            
            // Initialize rush delivery time combo box with time slots
            if (rushDeliveryTimeComboBox != null) {
                rushDeliveryTimeComboBox.getItems().addAll(
                    "09:00 - 11:00", "11:00 - 13:00", "13:00 - 15:00", "15:00 - 17:00", "17:00 - 19:00"
                );
            }
            
            // Initialize DatePicker with proper validation
            setupDatePickerValidation();
            
            // Initialize with empty state
            if (rushOrderDetailsBox != null) {
                rushOrderDetailsBox.setVisible(false);
                rushOrderDetailsBox.setManaged(false);
            }
            if (errorMessageLabel != null) {
                errorMessageLabel.setVisible(false);
            }
            
            // Initialize legacy components if they exist
            if (deliveryTimeComboBox != null) {
                deliveryTimeComboBox.getItems().addAll(
                    "09:00", "10:00", "11:00", "13:00", "14:00", "15:00", "16:00", "17:00"
                );
                if (!deliveryTimeComboBox.getItems().isEmpty()) {
                    deliveryTimeComboBox.setValue(deliveryTimeComboBox.getItems().get(0));
                }
            }
            
            if (rushDeliveryTimeBox != null) {
                rushDeliveryTimeBox.setVisible(false);
                rushDeliveryTimeBox.setManaged(false);
            }
            
            // Initialize enhanced validation and form state management
            initializeEnhancedValidationAndFormState();
            
            logger.info("DeliveryInfoScreenController.initialize: Enhanced initialization completed successfully");
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "DeliveryInfoScreenController.initialize: Error during enhanced initialization", e);
        }
    }
    
    /**
     * Initialize enhanced validation and form state management
     */
    private void initializeEnhancedValidationAndFormState() {
        try {
            // Initialize real-time validation manager
            validationManager = new RealTimeValidationManager();
            validationManager.setValidationMode(RealTimeValidationManager.ValidationMode.PROGRESSIVE);
            
            // Initialize form state manager
            formStateManager = new FormStateManager();
            
            // Generate session ID for this form
            currentFormSessionId = "delivery_info_" + System.currentTimeMillis();
            formStateManager.initializeForm(currentFormSessionId, "delivery_info");
            
            // Register fields for validation and state management
            registerFieldsForEnhancedValidation();
            
            // Set up callbacks
            formStateManager.setOnFormDirtyChanged(formId -> {
                logger.fine("DeliveryInfoScreenController: Form state changed - dirty: " + formStateManager.hasUnsavedChanges());
            });
            
            logger.info("DeliveryInfoScreenController.initializeEnhancedValidationAndFormState: Enhanced validation and form state management initialized");
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "DeliveryInfoScreenController.initializeEnhancedValidationAndFormState: Error initializing enhanced features", e);
        }
    }
    
    /**
     * Register form fields for enhanced validation and state management
     */
    private void registerFieldsForEnhancedValidation() {
        try {
            // Register primary fields with validation and state management
            if (nameField != null) {
                validationManager.registerTextField("recipientName", nameField, null);
                formStateManager.registerTextField("recipientName", nameField);
            }
            
            if (phoneField != null) {
                validationManager.registerTextField("phoneNumber", phoneField, null);
                formStateManager.registerTextField("phoneNumber", phoneField);
            }
            
            if (emailField != null) {
                validationManager.registerTextField("email", emailField, null);
                formStateManager.registerTextField("email", emailField);
            }
            
            if (addressArea != null) {
                validationManager.registerTextArea("deliveryAddress", addressArea, null);
                formStateManager.registerTextArea("deliveryAddress", addressArea);
            }
            
            if (provinceCityComboBox != null) {
                validationManager.registerComboBox("provinceCity", provinceCityComboBox, null);
                formStateManager.registerComboBox("provinceCity", provinceCityComboBox);
            }
            
            if (instructionsArea != null) {
                formStateManager.registerTextArea("deliveryInstructions", instructionsArea);
            }
            
            if (rushOrderCheckBox != null) {
                formStateManager.registerCheckBox("rushDelivery", rushOrderCheckBox);
            }
            
            if (rushDeliveryDatePicker != null) {
                formStateManager.registerDatePicker("rushDeliveryDate", rushDeliveryDatePicker);
            }
            
            if (rushDeliveryTimeComboBox != null) {
                formStateManager.registerComboBox("rushDeliveryTime", rushDeliveryTimeComboBox);
            }
            
            // Register legacy fields as fallbacks
            if (recipientNameField != null) {
                validationManager.registerTextField("recipientName", recipientNameField, null);
                formStateManager.registerTextField("recipientName", recipientNameField);
            }
            
            if (phoneNumberField != null) {
                validationManager.registerTextField("phoneNumber", phoneNumberField, null);
                formStateManager.registerTextField("phoneNumber", phoneNumberField);
            }
            
            logger.info("DeliveryInfoScreenController.registerFieldsForEnhancedValidation: Registered fields for enhanced validation and state management");
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "DeliveryInfoScreenController.registerFieldsForEnhancedValidation: Error registering fields", e);
        }
    }

    public void initData(OrderEntity order) {
        this.currentOrder = order;
        this.isEditing = order.getDeliveryInfo() != null;
        
        if (isEditing) {
            populateExistingDeliveryInfo(order.getDeliveryInfo());
        }
        setupListeners();
        updateShippingFeePreview();
    }

    /**
     * Enhanced setOrderData with comprehensive validation, fallback mechanisms, and service integration.
     * Integrates with OrderDataLoaderService for robust data loading and validation.
     * @param order The order entity to work with
     */
    public void setOrderData(OrderEntity order) {
        logger.info("DeliveryInfoScreenController.setOrderData: Setting order data with enhanced validation");
        
        try {
            if (order == null) {
                logger.severe("DeliveryInfoScreenController.setOrderData: Received null order");
                handleOrderDataError("Order data validation", new IllegalArgumentException("No order data received"));
                return;
            }
            
            // Enhanced data loading with comprehensive fallbacks
            OrderEntity processedOrder = loadOrderDataWithFallbacks(order);
            
            // Validate order data completeness
            if (validateOrderReadinessForDelivery(processedOrder)) {
                // Create enhanced OrderSummaryDTO for structured data handling
                this.currentOrderSummaryDTO = createOrderSummaryDTOWithFallbacks(processedOrder);
                
                // Initialize the screen with processed data
                initData(processedOrder);
                
                // Populate order summary with enhanced validation
                populateOrderSummaryFromDTO();
                
                logger.info("DeliveryInfoScreenController.setOrderData: Order data set successfully for Order ID: " + processedOrder.getOrderId());
            } else {
                logger.warning("DeliveryInfoScreenController.setOrderData: Order validation failed, using basic initialization");
                initData(processedOrder);
                showError("Some order information may be incomplete. Please verify details before proceeding.");
            }
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "DeliveryInfoScreenController.setOrderData: Critical error processing order data", e);
            handleOrderDataError("Order data processing", e);
        }
    }

    private void populateExistingDeliveryInfo(DeliveryInfo info) {
        // Use new field names
        if (nameField != null) nameField.setText(info.getRecipientName());
        if (phoneField != null) phoneField.setText(info.getPhoneNumber());
        if (emailField != null) emailField.setText(info.getEmail());
        if (addressArea != null) addressArea.setText(info.getDeliveryAddress());
        if (instructionsArea != null) instructionsArea.setText(info.getDeliveryInstructions());
        if (provinceCityComboBox != null) provinceCityComboBox.setValue(info.getDeliveryProvinceCity());
        if (rushOrderCheckBox != null) rushOrderCheckBox.setSelected(info.isRushDelivery());
        
        // Legacy field support
        if (recipientNameField != null) recipientNameField.setText(info.getRecipientName());
        if (phoneNumberField != null) phoneNumberField.setText(info.getPhoneNumber());
        if (streetAddressField != null) streetAddressField.setText(info.getStreetAddress());
        if (districtField != null) districtField.setText(info.getDistrict());
        if (cityField != null) cityField.setText(info.getCity());
        if (deliveryInstructionsArea != null) deliveryInstructionsArea.setText(info.getDeliveryInstructions());
        if (rushDeliveryCheckBox != null) rushDeliveryCheckBox.setSelected(info.isRushDelivery());
        
        if (info.getRequestedRushDeliveryTime() != null) {
            if (rushDeliveryDatePicker != null) {
                rushDeliveryDatePicker.setValue(info.getRequestedRushDeliveryTime().toLocalDate());
            }
            if (rushDeliveryTimeComboBox != null) {
                rushDeliveryTimeComboBox.setValue(info.getRequestedRushDeliveryTime().toLocalTime().toString());
            }
            if (rushOrderDetailsBox != null) {
                rushOrderDetailsBox.setVisible(true);
                rushOrderDetailsBox.setManaged(true);
            }
            
            // Legacy support
            if (deliveryDatePicker != null) {
                deliveryDatePicker.setValue(info.getRequestedRushDeliveryTime().toLocalDate());
            }
            if (deliveryTimeComboBox != null) {
                deliveryTimeComboBox.setValue(info.getRequestedRushDeliveryTime().toLocalTime().toString());
            }
            if (rushDeliveryTimeBox != null) {
                rushDeliveryTimeBox.setVisible(true);
                rushDeliveryTimeBox.setManaged(true);
            }
        }
    }

    private void setupListeners() {
        // Setup listeners for new fields
        if (rushOrderCheckBox != null) {
            rushOrderCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
                if (rushOrderDetailsBox != null) {
                    rushOrderDetailsBox.setVisible(newVal);
                    rushOrderDetailsBox.setManaged(newVal);
                }
                updateShippingFeePreview();
            });
        }

        // Add real-time validation and shipping fee updates for new fields
        if (addressArea != null) {
            addressArea.textProperty().addListener((obs, old, newVal) -> updateShippingFeePreview());
        }
        if (provinceCityComboBox != null) {
            provinceCityComboBox.valueProperty().addListener((obs, old, newVal) -> updateShippingFeePreview());
        }
        
        // Legacy field listeners
        if (rushDeliveryCheckBox != null) {
            rushDeliveryCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
                if (rushDeliveryTimeBox != null) {
                    rushDeliveryTimeBox.setVisible(newVal);
                    rushDeliveryTimeBox.setManaged(newVal);
                }
                updateShippingFeePreview();
            });
        }

        if (streetAddressField != null) {
            streetAddressField.textProperty().addListener((obs, old, newVal) -> updateShippingFeePreview());
        }
        if (districtField != null) {
            districtField.textProperty().addListener((obs, old, newVal) -> updateShippingFeePreview());
        }
        if (cityField != null) {
            cityField.textProperty().addListener((obs, old, newVal) -> updateShippingFeePreview());
        }
    }

    private void updateShippingFeePreview() {
        // ENHANCED: Better validation and null-safety for shipping fee calculation
        if (currentOrder == null) {
            System.err.println("DELIVERY_INFO_ERROR: currentOrder is null during shipping fee calculation");
            if (shippingFeeLabel != null) {
                shippingFeeLabel.setText("Shipping Fee: Order data missing");
            }
            showError("Order data is missing");
            return;
        }
        
        System.out.println("DELIVERY_INFO: Updating shipping fee preview for Order " + currentOrder.getOrderId());

        try {
            DeliveryInfo previewInfo = createDeliveryInfoFromFields();
            
            // ENHANCED: Validate delivery info before calculation
            if (previewInfo.getDeliveryProvinceCity() == null || previewInfo.getDeliveryAddress() == null ||
                previewInfo.getDeliveryProvinceCity().trim().isEmpty() || previewInfo.getDeliveryAddress().trim().isEmpty()) {
                System.out.println("DELIVERY_INFO: Incomplete address info, waiting for user input");
                if (shippingFeeLabel != null) {
                    shippingFeeLabel.setText("Shipping Fee: Enter address");
                }
                return;
            }
            
            // ENHANCED: Use direct delivery calculation service instead of non-existent method
            // Create a temporary order with delivery info for calculation
            OrderEntity tempOrder = new OrderEntity();
            tempOrder.setOrderId(currentOrder.getOrderId());
            tempOrder.setOrderItems(currentOrder.getOrderItems());
            tempOrder.setDeliveryInfo(previewInfo);
            
            System.out.println("DELIVERY_INFO: Calculating shipping fee with province: " +
                             previewInfo.getDeliveryProvinceCity() +
                             ", address: " + previewInfo.getDeliveryAddress());
            
            float fee = deliveryService.calculateShippingFee(tempOrder, isRushDeliverySelected());
            
            System.out.println("DELIVERY_INFO: Shipping fee calculated successfully: " + fee + " VND");
            
            if (shippingFeeLabel != null) {
                shippingFeeLabel.setText(String.format("Shipping Fee: %,.0f VND", fee));
            }
            if (errorMessageLabel != null) {
                errorMessageLabel.setVisible(false);
            }
            
        } catch (ValidationException e) {
            System.err.println("DELIVERY_INFO_ERROR: Validation error during shipping calculation: " + e.getMessage());
            if (shippingFeeLabel != null) {
                shippingFeeLabel.setText("Shipping Fee: " + e.getMessage());
            }
            showError("Shipping calculation error: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("DELIVERY_INFO_ERROR: Unexpected error during shipping calculation: " + e.getMessage());
            e.printStackTrace();
            if (shippingFeeLabel != null) {
                shippingFeeLabel.setText("Shipping Fee: Calculation failed");
            }
            showError("Unable to calculate shipping fee: " + e.getMessage());
        }
    }

    private boolean isRushDeliverySelected() {
        if (rushOrderCheckBox != null) {
            return rushOrderCheckBox.isSelected();
        }
        if (rushDeliveryCheckBox != null) {
            return rushDeliveryCheckBox.isSelected();
        }
        return false;
    }

    private DeliveryInfo createDeliveryInfoFromFields() {
        DeliveryInfo info = new DeliveryInfo();
        
        // Use new fields if available, fallback to legacy fields
        String recipientName = getFieldText(nameField, recipientNameField);
        String phoneNumber = getFieldText(phoneField, phoneNumberField);
        String email = getFieldText(emailField, emailField);
        String address = getFieldText(addressArea, streetAddressField);
        String instructions = getFieldText(instructionsArea, deliveryInstructionsArea);
        String provinceCity = getComboBoxValue(provinceCityComboBox, cityField);
        
        info.setRecipientName(recipientName);
        info.setPhoneNumber(phoneNumber);
        info.setEmail(email);
        info.setDeliveryAddress(address);
        info.setDeliveryProvinceCity(provinceCity);
        info.setDeliveryInstructions(instructions);
        
        // Set rush delivery flag
        boolean isRushDelivery = isRushDeliverySelected();
        info.setRushDelivery(isRushDelivery);
        
        // Set rush delivery time if applicable with enhanced validation
        if (isRushDelivery) {
            DatePicker datePicker = rushDeliveryDatePicker != null ? rushDeliveryDatePicker : deliveryDatePicker;
            ComboBox<String> timeComboBox = rushDeliveryTimeComboBox != null ? rushDeliveryTimeComboBox : deliveryTimeComboBox;
            
            if (datePicker != null && timeComboBox != null) {
                try {
                    LocalDate selectedDate = validateAndGetDatePickerValue(datePicker);
                    String timeValue = timeComboBox.getValue();
                    
                    if (selectedDate != null && timeValue != null && !timeValue.trim().isEmpty()) {
                        LocalTime parsedTime = validateAndParseTimeValue(timeValue);
                        if (parsedTime != null) {
                            LocalDateTime rushTime = LocalDateTime.of(selectedDate, parsedTime);
                            info.setRequestedRushDeliveryTime(rushTime);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error parsing rush delivery date/time: " + e.getMessage());
                    showError("Invalid date or time format for rush delivery. Please select a valid date and time.");
                }
            }
        }
        
        return info;
    }
    
    private String getFieldText(TextField primary, TextField fallback) {
        if (primary != null && primary.getText() != null) {
            return primary.getText().trim();
        }
        if (fallback != null && fallback.getText() != null) {
            return fallback.getText().trim();
        }
        return "";
    }
    
    private String getFieldText(TextArea primary, TextField fallback) {
        if (primary != null && primary.getText() != null) {
            return primary.getText().trim();
        }
        if (fallback != null && fallback.getText() != null) {
            return fallback.getText().trim();
        }
        return "";
    }
    
    private String getFieldText(TextArea primary, TextArea fallback) {
        if (primary != null && primary.getText() != null) {
            return primary.getText().trim();
        }
        if (fallback != null && fallback.getText() != null) {
            return fallback.getText().trim();
        }
        return "";
    }
    
    private String getComboBoxValue(ComboBox<String> primary, TextField fallback) {
        if (primary != null && primary.getValue() != null) {
            return primary.getValue().trim();
        }
        if (fallback != null && fallback.getText() != null) {
            return fallback.getText().trim();
        }
        return "";
    }

    // FXML Event Handlers - These methods are required by the FXML file

    @FXML
    void handleProvinceCityChange(ActionEvent event) {
        // When province/city changes, re-evaluate rush order eligibility if selected
        if (isRushDeliverySelected()) {
            handleRushOrderToggle(null); // Re-evaluate eligibility
        }
        calculateAndUpdateShippingFee();
    }

    @FXML
    void handleRushOrderToggle(ActionEvent event) {
        boolean isSelected = isRushDeliverySelected();
        
        // Show/hide rush order details
        if (rushOrderDetailsBox != null) {
            rushOrderDetailsBox.setVisible(isSelected);
            rushOrderDetailsBox.setManaged(isSelected);
        }
        if (rushDeliveryTimeBox != null) {
            rushDeliveryTimeBox.setVisible(isSelected);
            rushDeliveryTimeBox.setManaged(isSelected);
        }
        
        if (isSelected) {
            // Check eligibility for rush delivery
            try {
                DeliveryInfo tempInfo = createDeliveryInfoFromFields();
                boolean isEligible = deliveryService.isRushDeliveryAddressEligible(tempInfo);
                
                if (rushOrderEligibilityLabel != null) {
                    if (isEligible) {
                        rushOrderEligibilityLabel.setText("✓ Rush delivery is available for this address");
                        rushOrderEligibilityLabel.setStyle("-fx-text-fill: green;");
                    } else {
                        rushOrderEligibilityLabel.setText("✗ Rush delivery is not available for this address (Only available for inner city Hanoi)");
                        rushOrderEligibilityLabel.setStyle("-fx-text-fill: red;");
                    }
                }
            } catch (Exception e) {
                if (rushOrderEligibilityLabel != null) {
                    rushOrderEligibilityLabel.setText("Unable to check rush delivery eligibility");
                    rushOrderEligibilityLabel.setStyle("-fx-text-fill: orange;");
                }
            }
        }
        
        calculateAndUpdateShippingFee();
    }

    @FXML
    void handleBackToCartAction(ActionEvent event) {
        System.out.println("Back to Cart action triggered");
        if (mainLayoutController != null) {
            mainLayoutController.loadContent("/com/aims/presentation/views/cart_screen.fxml");
            mainLayoutController.setHeaderTitle("Shopping Cart");
        } else {
            NavigationService.navigateTo("cart_screen.fxml", null, null);
        }
    }

    @FXML
    void handleProceedToPaymentActionEnhanced(ActionEvent event) {
        logger.info("DeliveryInfoScreenController.handleProceedToPaymentActionEnhanced: CRITICAL FIX 3 - Enhanced proceed to payment with comprehensive validation and state preservation");
        
        try {
            // ====== PHASE 1: PRE-VALIDATION AND SAFETY CHECKS ======
            
            // 1. Validate current order state exists
            if (currentOrder == null) {
                logger.severe("DeliveryInfoScreenController.handleProceedToPaymentActionEnhanced: Critical - currentOrder is null");
                showError("Order information is missing. Please restart the order process.");
                return;
            }
            
            logger.info("DeliveryInfoScreenController.handleProceedToPaymentActionEnhanced: Processing Order ID: " + currentOrder.getOrderId());
            
            // 2. Validate delivery information input completeness
            if (!validateDeliveryInfoInput()) {
                logger.warning("DeliveryInfoScreenController.handleProceedToPaymentActionEnhanced: Delivery info input validation failed");
                return; // Error message already displayed by validation method
            }
            
            // 3. Create delivery info from validated fields
            DeliveryInfo deliveryInfo = createDeliveryInfoFromFields();
            if (deliveryInfo == null) {
                logger.severe("DeliveryInfoScreenController.handleProceedToPaymentActionEnhanced: Critical - failed to create delivery info");
                showError("Failed to process delivery information. Please check all fields and try again.");
                return;
            }
            
            // ====== PHASE 2: ORDER STATE VALIDATION AND PERSISTENCE ======
            
            // 4. Save delivery information to order and persist changes
            logger.info("DeliveryInfoScreenController.handleProceedToPaymentActionEnhanced: Saving delivery information to order");
            try {
                currentOrder = orderService.setDeliveryInformation(
                    currentOrder.getOrderId(),
                    deliveryInfo,
                    isRushDeliverySelected()
                );
                
                if (currentOrder == null) {
                    logger.severe("DeliveryInfoScreenController.handleProceedToPaymentActionEnhanced: Critical - order service returned null after setting delivery info");
                    showError("Failed to save delivery information. Please try again.");
                    return;
                }
                
                logger.info("DeliveryInfoScreenController.handleProceedToPaymentActionEnhanced: Delivery information saved successfully");
                
            } catch (Exception e) {
                logger.log(Level.SEVERE, "DeliveryInfoScreenController.handleProceedToPaymentActionEnhanced: Failed to save delivery information", e);
                showError("Failed to save delivery information: " + e.getMessage());
                return;
            }
            
            // 5. Reload complete order data to ensure state consistency
            if (orderDataLoaderService != null) {
                try {
                    logger.info("DeliveryInfoScreenController.handleProceedToPaymentActionEnhanced: Reloading complete order data for state consistency");
                    currentOrder = orderDataLoaderService.loadCompleteOrderData(currentOrder.getOrderId());
                    
                    // Validate data completeness after reload
                    if (!orderDataLoaderService.validateOrderDataCompleteness(currentOrder)) {
                        logger.warning("DeliveryInfoScreenController.handleProceedToPaymentActionEnhanced: Order data completeness validation failed after reload");
                        showError("Order data may be incomplete after saving. Please verify all information.");
                        return;
                    }
                    
                    logger.info("DeliveryInfoScreenController.handleProceedToPaymentActionEnhanced: Complete order data reloaded and validated successfully");
                    
                } catch (ResourceNotFoundException e) {
                    logger.severe("DeliveryInfoScreenController.handleProceedToPaymentActionEnhanced: Critical - order not found after saving: " + e.getMessage());
                    showError("Order data could not be reloaded. Please try again or restart the checkout process.");
                    return;
                } catch (Exception e) {
                    logger.log(Level.WARNING, "DeliveryInfoScreenController.handleProceedToPaymentActionEnhanced: Error reloading order data", e);
                    // Continue with current order data but log the issue
                }
            }
            
            // ====== PHASE 3: COMPREHENSIVE ORDER VALIDATION FOR PAYMENT READINESS ======
            
            // 6. Validate order business rules using OrderValidationService
            if (orderValidationService != null) {
                try {
                    logger.info("DeliveryInfoScreenController.handleProceedToPaymentActionEnhanced: Validating order business rules");
                    orderValidationService.validateOrderBusinessRules(currentOrder);
                    
                    // Check if order is ready for payment
                    boolean isReadyForPayment = orderValidationService.isOrderReadyForPayment(currentOrder.getOrderId());
                    if (!isReadyForPayment) {
                        logger.warning("DeliveryInfoScreenController.handleProceedToPaymentActionEnhanced: Order not ready for payment according to OrderValidationService");
                        showError("Order is not ready for payment. Please ensure all required information is complete and valid.");
                        return;
                    }
                    
                    logger.info("DeliveryInfoScreenController.handleProceedToPaymentActionEnhanced: Order validation passed - ready for payment");
                    
                } catch (ValidationException e) {
                    logger.log(Level.WARNING, "DeliveryInfoScreenController.handleProceedToPaymentActionEnhanced: Order validation failed", e);
                    showError("Order validation failed: " + e.getMessage());
                    return;
                } catch (Exception e) {
                    logger.log(Level.WARNING, "DeliveryInfoScreenController.handleProceedToPaymentActionEnhanced: Error during order validation", e);
                    // Continue but log the issue
                }
            } else {
                // Fallback validation if OrderValidationService is not available
                logger.warning("DeliveryInfoScreenController.handleProceedToPaymentActionEnhanced: OrderValidationService not available, using fallback validation");
                if (!validateOrderReadinessForDelivery(currentOrder)) {
                    showError("Order is not ready for payment. Please check all required information.");
                    return;
                }
            }
            
            // ====== PHASE 4: FORM STATE PRESERVATION ======
            
            // 7. Save current form state before navigation
            if (formStateManager != null) {
                try {
                    logger.info("DeliveryInfoScreenController.handleProceedToPaymentActionEnhanced: Saving form state before navigation");
                    formStateManager.saveFormState();
                } catch (Exception e) {
                    logger.log(Level.WARNING, "DeliveryInfoScreenController.handleProceedToPaymentActionEnhanced: Failed to save form state", e);
                    // Continue navigation but log the issue
                }
            }
            
            // ====== PHASE 5: ENHANCED NAVIGATION WITH COMPREHENSIVE FALLBACKS ======
            
            logger.info("ENHANCED_NAVIGATION: DeliveryInfo -> OrderSummary for Order " + currentOrder.getOrderId() + " with enhanced state preservation");
            
            // 8. Primary navigation attempt using EnhancedNavigationManager
            boolean navigationSuccessful = false;
            
            try {
                EnhancedNavigationManager.NavigationResult result = EnhancedNavigationManager.navigateToOrderSummary(
                    currentOrder, mainLayoutController);
                
                if (result.isSuccess()) {
                    logger.info("DeliveryInfoScreenController.handleProceedToPaymentActionEnhanced: Enhanced navigation successful: " + result.getDescription());
                    navigationSuccessful = true;
                } else {
                    logger.warning("DeliveryInfoScreenController.handleProceedToPaymentActionEnhanced: Enhanced navigation failed: " + result.getDescription());
                }
                
            } catch (Exception e) {
                logger.log(Level.WARNING, "DeliveryInfoScreenController.handleProceedToPaymentActionEnhanced: EnhancedNavigationManager threw exception", e);
            }
            
            // 9. Fallback navigation attempts
            if (!navigationSuccessful) {
                logger.info("DeliveryInfoScreenController.handleProceedToPaymentActionEnhanced: Attempting CheckoutNavigationWrapper fallback");
                
                try {
                    navigationSuccessful = com.aims.core.presentation.utils.CheckoutNavigationWrapper.navigateToOrderSummary(
                            currentOrder, mainLayoutController, this);
                    
                    if (navigationSuccessful) {
                        logger.info("DeliveryInfoScreenController.handleProceedToPaymentActionEnhanced: CheckoutNavigationWrapper fallback successful");
                    } else {
                        logger.warning("DeliveryInfoScreenController.handleProceedToPaymentActionEnhanced: CheckoutNavigationWrapper fallback failed");
                    }
                    
                } catch (Exception e) {
                    logger.log(Level.WARNING, "DeliveryInfoScreenController.handleProceedToPaymentActionEnhanced: CheckoutNavigationWrapper threw exception", e);
                }
            }
            
            // 10. Final fallback using direct navigation
            if (!navigationSuccessful) {
                logger.info("DeliveryInfoScreenController.handleProceedToPaymentActionEnhanced: Attempting direct navigation fallback");
                
                try {
                    navigationSuccessful = navigateToOrderSummaryEnhanced(currentOrder);
                    
                    if (navigationSuccessful) {
                        logger.info("DeliveryInfoScreenController.handleProceedToPaymentActionEnhanced: Direct navigation fallback successful");
                    } else {
                        logger.warning("DeliveryInfoScreenController.handleProceedToPaymentActionEnhanced: Direct navigation fallback failed");
                    }
                    
                } catch (Exception e) {
                    logger.log(Level.WARNING, "DeliveryInfoScreenController.handleProceedToPaymentActionEnhanced: Direct navigation threw exception", e);
                }
            }
            
            // 11. Ultimate fallback using NavigationService
            if (!navigationSuccessful) {
                logger.warning("DeliveryInfoScreenController.handleProceedToPaymentActionEnhanced: All primary navigation methods failed, attempting NavigationService fallback");
                attemptFallbackOrderSummaryNavigation();
            }
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "DeliveryInfoScreenController.handleProceedToPaymentActionEnhanced: Critical error in enhanced payment navigation", e);
            handleOrderDataError("Enhanced payment navigation", e);
        }
    }
    
    @FXML
    void handleProceedToPaymentAction(ActionEvent event) {
        // Enhanced method as primary, fallback to basic if needed
        try {
            handleProceedToPaymentActionEnhanced(event);
        } catch (Exception e) {
            logger.log(Level.WARNING, "DeliveryInfoScreenController.handleProceedToPaymentAction: Enhanced method failed, using basic fallback", e);
            handleProceedToPaymentActionBasic(event);
        }
    }
    
    /**
     * Basic fallback implementation for payment navigation
     */
    private void handleProceedToPaymentActionBasic(ActionEvent event) {
        try {
            // ENHANCED: Better validation before processing
            if (currentOrder == null) {
                showError("Order information is missing. Please restart the order process.");
                return;
            }
            
            DeliveryInfo deliveryInfo = createDeliveryInfoFromFields();
            
            // ENHANCED: Validate delivery info before sending to service
            if (deliveryInfo.getDeliveryProvinceCity() == null || deliveryInfo.getDeliveryProvinceCity().trim().isEmpty()) {
                showError("Please select a province/city for delivery.");
                return;
            }
            if (deliveryInfo.getDeliveryAddress() == null || deliveryInfo.getDeliveryAddress().trim().isEmpty()) {
                showError("Please enter a complete delivery address.");
                return;
            }
            
            System.out.println("NAVIGATION: DeliveryInfo -> OrderSummary for Order " + currentOrder.getOrderId());
            
            // Update order with delivery information
            currentOrder = orderService.setDeliveryInformation(
                currentOrder.getOrderId(),
                deliveryInfo,
                isRushDeliverySelected()
            );
            
            // ENHANCED: Validate updated order before navigation
            if (currentOrder == null) {
                showError("Failed to update order with delivery information. Please try again.");
                return;
            }
            
            System.out.println("NAVIGATION: Order updated successfully, proceeding to summary");
            
            if (mainLayoutController != null) {
                Object controller = mainLayoutController.loadContent("/com/aims/presentation/views/order_summary_screen.fxml");
                mainLayoutController.setHeaderTitle("Order Summary & Confirmation");
                
                // ENHANCED: More robust controller data injection
                if (controller != null) {
                    if (controller instanceof OrderSummaryController) {
                        ((OrderSummaryController) controller).setOrderData(currentOrder);
                        ((OrderSummaryController) controller).setMainLayoutController(mainLayoutController);
                        System.out.println("NAVIGATION: Order data successfully passed to OrderSummaryController");
                    } else {
                        // Fallback using reflection
                        try {
                            controller.getClass().getMethod("setOrderData", OrderEntity.class).invoke(controller, currentOrder);
                            System.out.println("NAVIGATION: Order data set via reflection");
                        } catch (Exception e) {
                            System.err.println("NAVIGATION ERROR: Could not set order data on controller: " + e.getMessage());
                            showError("Navigation error occurred. Order data may not be properly loaded.");
                        }
                    }
                } else {
                    System.err.println("NAVIGATION ERROR: Controller is null after loading");
                    showError("Failed to load order summary screen properly.");
                }
            } else {
                System.err.println("NAVIGATION FALLBACK: Using NavigationService");
                NavigationService.navigateToOrderSummary(currentOrder.getOrderId());
            }
            
        } catch (ValidationException e) {
            System.err.println("VALIDATION ERROR in delivery info: " + e.getMessage());
            showError("Please check your input: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("UNEXPECTED ERROR in delivery flow: " + e.getMessage());
            e.printStackTrace();
            showError("An error occurred: " + e.getMessage());
        }
    }

    private void calculateAndUpdateShippingFee() {
        updateShippingFeePreview();
    }

    // Legacy methods for backward compatibility
    @FXML
    private void handleSubmit() {
        handleProceedToPaymentAction(null);
    }

    @FXML
    private void handleBack() {
        handleBackToCartAction(null);
    }

    private void showError(String message) {
        if (errorMessageLabel != null) {
            errorMessageLabel.setText(message);
            errorMessageLabel.setVisible(true);
        } else {
            System.err.println("Error: " + message);
        }
    }

    /**
     * Sets up comprehensive validation for DatePicker components to prevent DateTimeParseException
     */
    private void setupDatePickerValidation() {
        // Configure rush delivery date picker with validation
        if (rushDeliveryDatePicker != null) {
            setupDatePickerWithValidation(rushDeliveryDatePicker);
        }
        
        // Configure legacy date picker with validation if it exists
        if (deliveryDatePicker != null) {
            setupDatePickerWithValidation(deliveryDatePicker);
        }
    }

    /**
     * Configures a DatePicker with comprehensive input validation and error handling
     */
    private void setupDatePickerWithValidation(DatePicker datePicker) {
        // Create a custom StringConverter to handle date parsing with validation
        datePicker.setConverter(new StringConverter<LocalDate>() {
            private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            
            @Override
            public String toString(LocalDate date) {
                if (date != null) {
                    return dateFormatter.format(date);
                }
                return "";
            }
            
            @Override
            public LocalDate fromString(String string) {
                if (string != null && !string.trim().isEmpty()) {
                    try {
                        // Try to parse with the expected format first
                        return LocalDate.parse(string.trim(), dateFormatter);
                    } catch (DateTimeParseException e) {
                        try {
                            // Try ISO format as fallback
                            return LocalDate.parse(string.trim());
                        } catch (DateTimeParseException e2) {
                            // Show user-friendly error for invalid input
                            showError("Invalid date format. Please use DD/MM/YYYY format (e.g., 25/12/2024).");
                            System.err.println("Invalid date input: '" + string + "' - " + e2.getMessage());
                            return null; // Return null for invalid input
                        }
                    }
                }
                return null;
            }
        });
        
        // Set prompt text to guide user input
        datePicker.setPromptText("DD/MM/YYYY");
        
        // Add real-time validation listener
        datePicker.getEditor().textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.trim().isEmpty()) {
                validateDateInput(newValue.trim(), datePicker);
            }
        });
        
        // Validate on focus lost
        datePicker.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused && datePicker.getEditor().getText() != null) {
                String inputText = datePicker.getEditor().getText().trim();
                if (!inputText.isEmpty()) {
                    validateDateInput(inputText, datePicker);
                }
            }
        });
        
        // Restrict to future dates for rush delivery
        if (datePicker == rushDeliveryDatePicker) {
            datePicker.setDayCellFactory(picker -> new javafx.scene.control.DateCell() {
                @Override
                public void updateItem(LocalDate date, boolean empty) {
                    super.updateItem(date, empty);
                    LocalDate today = LocalDate.now();
                    
                    if (date.isBefore(today)) {
                        setDisable(true);
                        setStyle("-fx-background-color: #ffc0cb;"); // Light red for past dates
                        setTooltip(new javafx.scene.control.Tooltip("Rush delivery cannot be scheduled for past dates"));
                    }
                }
            });
        }
    }

    /**
     * Validates date input and provides immediate feedback to user
     */
    private void validateDateInput(String input, DatePicker datePicker) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            LocalDate parsedDate = LocalDate.parse(input, formatter);
            
            // Additional validation for rush delivery dates
            if (datePicker == rushDeliveryDatePicker && parsedDate.isBefore(LocalDate.now())) {
                showError("Rush delivery date cannot be in the past. Please select a future date.");
                datePicker.setValue(null);
                return;
            }
            
            // Clear any previous error if date is valid
            if (errorMessageLabel != null && errorMessageLabel.getText().contains("Invalid date")) {
                errorMessageLabel.setVisible(false);
            }
            
        } catch (DateTimeParseException e) {
            // Only show error for non-empty, complete-looking inputs to avoid spam during typing
            if (input.length() >= 8) {
                showError("Invalid date format. Please use DD/MM/YYYY format (e.g., 25/12/2024).");
            }
        }
    }

    /**
     * Safely validates and retrieves the DatePicker value with comprehensive error handling
     */
    private LocalDate validateAndGetDatePickerValue(DatePicker datePicker) {
        if (datePicker == null) {
            return null;
        }
        
        try {
            LocalDate value = datePicker.getValue();
            
            // Additional validation for the selected date
            if (value != null) {
                // Check if it's a rush delivery date picker and validate future date constraint
                if (datePicker == rushDeliveryDatePicker && value.isBefore(LocalDate.now())) {
                    showError("Rush delivery date cannot be in the past. Please select a future date.");
                    return null;
                }
                
                // Check if the date is too far in the future (e.g., more than 1 year)
                if (value.isAfter(LocalDate.now().plusYears(1))) {
                    showError("Delivery date cannot be more than 1 year in the future.");
                    return null;
                }
            }
            
            return value;
            
        } catch (Exception e) {
            System.err.println("Error retrieving DatePicker value: " + e.getMessage());
            showError("Invalid date selection. Please choose a valid date.");
            return null;
        }
    }

    /**
     * Validates and parses time value from ComboBox with comprehensive error handling
     */
    private LocalTime validateAndParseTimeValue(String timeValue) {
        if (timeValue == null || timeValue.trim().isEmpty()) {
            return null;
        }
        
        try {
            String cleanTimeValue = timeValue.trim();
            
            // Handle both "HH:mm" and "HH:mm - HH:mm" formats
            if (cleanTimeValue.contains(" - ")) {
                cleanTimeValue = cleanTimeValue.split(" - ")[0].trim(); // Take the start time
            }
            
            // Validate time format
            if (!cleanTimeValue.matches("^([01]?[0-9]|2[0-3]):[0-5][0-9]$")) {
                showError("Invalid time format. Expected format: HH:MM (e.g., 09:00).");
                return null;
            }
            
            LocalTime parsedTime = LocalTime.parse(cleanTimeValue);
            
            // Additional business rule validation for rush delivery
            if (parsedTime.isBefore(LocalTime.of(9, 0)) || parsedTime.isAfter(LocalTime.of(19, 0))) {
                showError("Rush delivery is only available between 09:00 and 19:00.");
                return null;
            }
            
            return parsedTime;
            
        } catch (DateTimeParseException e) {
            System.err.println("Error parsing time value '" + timeValue + "': " + e.getMessage());
            showError("Invalid time format. Please select a valid time from the dropdown or use HH:MM format.");
            return null;
        } catch (Exception e) {
            System.err.println("Unexpected error parsing time: " + e.getMessage());
            showError("Error processing time selection. Please try again.");
            return null;
        }
    }
    
    // ==================== ENHANCED METHODS FOR SERVICE INTEGRATION ====================
    
    /**
     * Loads order data with comprehensive fallback mechanisms
     */
    private OrderEntity loadOrderDataWithFallbacks(OrderEntity order) {
        if (orderDataLoaderService == null) {
            logger.warning("DeliveryInfoScreenController.loadOrderDataWithFallbacks: OrderDataLoaderService not available, using basic order");
            return order;
        }
        
        try {
            // Validate data completeness first
            if (!orderDataLoaderService.validateOrderDataCompleteness(order)) {
                logger.info("DeliveryInfoScreenController.loadOrderDataWithFallbacks: Order data incomplete, attempting to refresh");
                try {
                    order = orderDataLoaderService.refreshOrderRelationships(order);
                    logger.info("DeliveryInfoScreenController.loadOrderDataWithFallbacks: Order relationships refreshed successfully");
                } catch (ResourceNotFoundException e) {
                    logger.warning("DeliveryInfoScreenController.loadOrderDataWithFallbacks: Failed to refresh order relationships: " + e.getMessage());
                    showError("Some order information could not be loaded. Display may be incomplete.");
                }
            }
            
            // Validate lazy loading initialization
            if (!orderDataLoaderService.validateLazyLoadingInitialization(order)) {
                logger.warning("DeliveryInfoScreenController.loadOrderDataWithFallbacks: Lazy loading issues detected");
                try {
                    // Attempt to reload complete order data
                    order = orderDataLoaderService.loadCompleteOrderData(order.getOrderId());
                    logger.info("DeliveryInfoScreenController.loadOrderDataWithFallbacks: Complete order data loaded successfully");
                } catch (ResourceNotFoundException e) {
                    logger.warning("DeliveryInfoScreenController.loadOrderDataWithFallbacks: Could not reload complete order data: " + e.getMessage());
                }
            }
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "DeliveryInfoScreenController.loadOrderDataWithFallbacks: Error during enhanced loading", e);
        }
        
        return order;
    }
    
    /**
     * Validates that order is ready for delivery information input
     */
    private boolean validateOrderReadinessForDelivery(OrderEntity order) {
        if (order == null) {
            logger.warning("DeliveryInfoScreenController.validateOrderReadinessForDelivery: Order is null");
            return false;
        }
        
        if (order.getOrderId() == null || order.getOrderId().trim().isEmpty()) {
            logger.warning("DeliveryInfoScreenController.validateOrderReadinessForDelivery: Order ID is missing");
            return false;
        }
        
        if (order.getOrderItems() == null || order.getOrderItems().isEmpty()) {
            logger.warning("DeliveryInfoScreenController.validateOrderReadinessForDelivery: Order items are missing");
            return false;
        }
        
        // Validate order items have required data for delivery calculation
        for (OrderItem item : order.getOrderItems()) {
            if (item.getProduct() == null) {
                logger.warning("DeliveryInfoScreenController.validateOrderReadinessForDelivery: Product information missing for an order item");
                return false;
            }
            if (item.getProduct().getWeightKg() <= 0) {
                logger.warning("DeliveryInfoScreenController.validateOrderReadinessForDelivery: Product weight missing for: " + item.getProduct().getTitle());
                return false;
            }
        }
        
        logger.info("DeliveryInfoScreenController.validateOrderReadinessForDelivery: Order validation passed");
        return true;
    }
    
    /**
     * Creates OrderSummaryDTO with comprehensive fallback handling
     */
    private OrderSummaryDTO createOrderSummaryDTOWithFallbacks(OrderEntity order) {
        if (orderDataLoaderService == null) {
            logger.warning("DeliveryInfoScreenController.createOrderSummaryDTOWithFallbacks: OrderDataLoaderService not available");
            return null;
        }
        
        try {
            OrderSummaryDTO dto = orderDataLoaderService.createOrderSummaryDTO(order);
            logger.info("DeliveryInfoScreenController.createOrderSummaryDTOWithFallbacks: OrderSummaryDTO created successfully");
            return dto;
        } catch (ValidationException e) {
            logger.warning("DeliveryInfoScreenController.createOrderSummaryDTOWithFallbacks: OrderSummaryDTO validation failed: " + e.getMessage());
            return null;
        } catch (Exception e) {
            logger.log(Level.WARNING, "DeliveryInfoScreenController.createOrderSummaryDTOWithFallbacks: Unexpected error creating DTO", e);
            return null;
        }
    }
    
    /**
     * Populate order summary from enhanced DTO
     */
    private void populateOrderSummaryFromDTO() {
        if (currentOrderSummaryDTO == null) {
            logger.warning("DeliveryInfoScreenController.populateOrderSummaryFromDTO: No DTO available, using entity fallback");
            populateOrderSummary();
            return;
        }
        
        try {
            logger.info("DeliveryInfoScreenController.populateOrderSummaryFromDTO: Populating UI from OrderSummaryDTO");
            
            // Update subtotal labels if available
            if (subtotalLabel != null) {
                subtotalLabel.setText(String.format("Subtotal: %,.0f VND", currentOrderSummaryDTO.totalProductPriceExclVAT()));
            }
            
            // Update VAT label if available
            if (vatLabel != null) {
                vatLabel.setText(String.format("VAT: %,.0f VND", currentOrderSummaryDTO.vatAmount()));
            }
            
            // Update total amount label if available
            if (totalAmountLabel != null) {
                totalAmountLabel.setText(String.format("Total (excl. shipping): %,.0f VND", currentOrderSummaryDTO.totalProductPriceInclVAT()));
            }
            
            logger.info("DeliveryInfoScreenController.populateOrderSummaryFromDTO: DTO-based population completed successfully");
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "DeliveryInfoScreenController.populateOrderSummaryFromDTO: Error populating from DTO, using fallback", e);
            populateOrderSummary();
        }
    }
    
    /**
     * Enhanced order summary population from entity (fallback method)
     */
    private void populateOrderSummary() {
        if (currentOrder == null) {
            logger.warning("DeliveryInfoScreenController.populateOrderSummary: No current order available");
            return;
        }
        
        try {
            // Update subtotal labels if available
            if (subtotalLabel != null) {
                subtotalLabel.setText(String.format("Subtotal: %,.0f VND", currentOrder.getTotalProductPriceExclVAT()));
            }
            
            // Update VAT label if available
            if (vatLabel != null) {
                float vatAmount = currentOrder.getTotalProductPriceInclVAT() - currentOrder.getTotalProductPriceExclVAT();
                vatLabel.setText(String.format("VAT: %,.0f VND", vatAmount));
            }
            
            // Update total amount label if available
            if (totalAmountLabel != null) {
                totalAmountLabel.setText(String.format("Total (excl. shipping): %,.0f VND", currentOrder.getTotalProductPriceInclVAT()));
            }
            
            logger.info("DeliveryInfoScreenController.populateOrderSummary: Entity-based population completed successfully");
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "DeliveryInfoScreenController.populateOrderSummary: Error populating order summary", e);
        }
    }
    
    /**
     * Handle order data errors with comprehensive error management
     */
    private void handleOrderDataError(String context, Exception error) {
        String errorMessage = "Error in " + context + ": " + error.getMessage();
        logger.log(Level.SEVERE, "DeliveryInfoScreenController.handleOrderDataError: " + errorMessage, error);
        
        // Display user-friendly error message
        showError(errorMessage);
        
        // Disable proceed button if available
        if (proceedToPaymentButton != null) {
            proceedToPaymentButton.setDisable(true);
        }
        
        // Clear shipping fee display
        if (shippingFeeLabel != null) {
            shippingFeeLabel.setText("Shipping Fee: Error");
        }
        if (totalAmountLabel != null) {
            totalAmountLabel.setText("TOTAL AMOUNT: Error");
        }
    }
/**
 * Enhanced delivery information validation with real-time feedback
 */
private boolean validateDeliveryInfoInput() {
    DeliveryInfo deliveryInfo = createDeliveryInfoFromFields();
    
    try {
        // Use Enhanced Delivery Info Validator for comprehensive validation
        EnhancedDeliveryInfoValidator.EnhancedValidationResult result =
            EnhancedDeliveryInfoValidator.validateComprehensive(deliveryInfo, currentOrder != null ? currentOrder.getOrderItems() : null);
        
        if (!result.isValid()) {
            logger.warning("DeliveryInfoScreenController.validateDeliveryInfoInput: Enhanced delivery info validation failed");
            
            // Show specific field errors
            StringBuilder errorMessage = new StringBuilder();
            for (EnhancedDeliveryInfoValidator.ValidationError error : result.getErrors()) {
                if (errorMessage.length() > 0) errorMessage.append("; ");
                errorMessage.append(error.getMessage());
                if (error.getSuggestion() != null) {
                    errorMessage.append(" (").append(error.getSuggestion()).append(")");
                }
            }
            
            showError("Please complete all required fields: " + errorMessage.toString());
            
            // Update validation manager if available
            if (validationManager != null) {
                validationManager.validateAllFields();
            }
            
            return false;
        }
        
        // Show warnings if any
        if (!result.getWarnings().isEmpty()) {
            StringBuilder warningMessage = new StringBuilder("Warnings: ");
            for (EnhancedDeliveryInfoValidator.ValidationWarning warning : result.getWarnings()) {
                if (warningMessage.length() > 10) warningMessage.append("; ");
                warningMessage.append(warning.getMessage());
            }
            logger.info("DeliveryInfoScreenController.validateDeliveryInfoInput: " + warningMessage.toString());
        }
        
        logger.info("DeliveryInfoScreenController.validateDeliveryInfoInput: Enhanced delivery info validation passed with score: " +
                   String.format("%.1f", result.getCompletenessScore()));
        return true;
        
    } catch (Exception e) {
        logger.log(Level.WARNING, "DeliveryInfoScreenController.validateDeliveryInfoInput: Error during enhanced validation, falling back to basic", e);
        
        // Fallback to basic validation
        DeliveryInfoValidator.ValidationResult basicResult = DeliveryInfoValidator.validateBasicFields(deliveryInfo);
        
        if (!basicResult.isValid()) {
            logger.warning("DeliveryInfoScreenController.validateDeliveryInfoInput: Basic delivery info validation failed: " + basicResult.getErrorMessage());
            showError("Please complete all required fields: " + basicResult.getErrorMessage());
            return false;
        }
        
        // Additional validation for rush delivery
        if (isRushDeliverySelected()) {
            DeliveryInfoValidator.ValidationResult rushResult = DeliveryInfoValidator.validateRushDelivery(deliveryInfo);
            if (!rushResult.isValid()) {
                logger.warning("DeliveryInfoScreenController.validateDeliveryInfoInput: Rush delivery validation failed: " + rushResult.getErrorMessage());
                showError("Rush delivery validation failed: " + rushResult.getErrorMessage());
                return false;
            }
        }
        
        logger.info("DeliveryInfoScreenController.validateDeliveryInfoInput: Basic delivery info validation passed");
        return true;
    }
}
    
    /**
     * Enhanced navigation to order summary screen
     */
    private boolean navigateToOrderSummaryEnhanced(OrderEntity completeOrder) {
        if (mainLayoutController == null) {
            logger.severe("DeliveryInfoScreenController.navigateToOrderSummaryEnhanced: MainLayoutController not available for navigation");
            return false;
        }
        
        try {
            Object controller = mainLayoutController.loadContent("/com/aims/presentation/views/order_summary_screen.fxml");
            mainLayoutController.setHeaderTitle("Order Summary & Confirmation");
            
            // Enhanced controller validation and injection
            if (controller instanceof OrderSummaryController) {
                OrderSummaryController orderSummaryController = (OrderSummaryController) controller;
                
                // Inject enhanced services if available
                if (orderDataLoaderService != null) {
                    orderSummaryController.setOrderDataLoaderService(orderDataLoaderService);
                }
                if (cartDataValidationService != null) {
                    orderSummaryController.setCartDataValidationService(cartDataValidationService);
                }
                
                // Set order data
                orderSummaryController.setOrderData(completeOrder);
                orderSummaryController.setMainLayoutController(mainLayoutController);
                
                logger.info("DeliveryInfoScreenController.navigateToOrderSummaryEnhanced: Enhanced navigation completed successfully");
                return true;
            } else {
                logger.warning("DeliveryInfoScreenController.navigateToOrderSummaryEnhanced: Controller is not OrderSummaryController: " +
                              (controller != null ? controller.getClass().getSimpleName() : "null"));
                
                // Fallback using reflection
                try {
                    controller.getClass().getMethod("setOrderData", OrderEntity.class).invoke(controller, completeOrder);
                    logger.info("DeliveryInfoScreenController.navigateToOrderSummaryEnhanced: Order data set via reflection");
                    return true;
                } catch (Exception e) {
                    logger.log(Level.WARNING, "DeliveryInfoScreenController.navigateToOrderSummaryEnhanced: Reflection fallback failed", e);
                    return false;
                }
            }
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "DeliveryInfoScreenController.navigateToOrderSummaryEnhanced: Navigation exception", e);
            return false;
        }
    }
    
    /**
     * Attempt fallback navigation using NavigationService
     */
    private void attemptFallbackOrderSummaryNavigation() {
        try {
            logger.warning("DeliveryInfoScreenController.attemptFallbackOrderSummaryNavigation: Attempting NavigationService fallback");
            NavigationService.navigateToOrderSummary(currentOrder.getOrderId());
            logger.info("DeliveryInfoScreenController.attemptFallbackOrderSummaryNavigation: NavigationService fallback succeeded");
        } catch (Exception fallbackException) {
            logger.log(Level.SEVERE, "DeliveryInfoScreenController.attemptFallbackOrderSummaryNavigation: Fallback navigation also failed", fallbackException);
            handleOrderDataError("Fallback order summary navigation", fallbackException);
        }
    }
}