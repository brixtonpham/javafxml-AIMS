package com.aims.core.presentation.controllers;

import com.aims.core.entities.OrderEntity;
import com.aims.core.entities.OrderItem;
import com.aims.core.entities.DeliveryInfo;
import com.aims.core.application.services.IOrderDataLoaderService;
import com.aims.core.application.services.ICartDataValidationService;
import com.aims.core.application.services.IOrderValidationService;
import com.aims.core.application.dtos.OrderSummaryDTO;
import com.aims.core.application.dtos.OrderItemDTO;
import com.aims.core.application.dtos.DeliveryInfoDTO;
import com.aims.core.shared.ServiceFactory;
import com.aims.core.shared.exceptions.ValidationException;
import com.aims.core.shared.exceptions.ResourceNotFoundException;
import com.aims.core.shared.NavigationService;
import com.aims.core.presentation.utils.FXMLSceneManager;
import com.aims.core.presentation.utils.EnhancedNavigationManager;
import com.aims.core.presentation.utils.MainLayoutControllerRegistry;
import com.aims.core.presentation.utils.UnifiedNavigationManager;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.concurrent.Task;
import javafx.application.Platform;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import java.util.logging.Level;

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
    private FXMLSceneManager sceneManager;
    private OrderEntity currentOrder;
    private OrderSummaryDTO currentOrderSummaryDTO;
    
    // Enhanced Services
    private IOrderDataLoaderService orderDataLoaderService;
    private ICartDataValidationService cartDataValidationService;
    private IOrderValidationService orderValidationService;
    
    // UI Enhancement Components
    @FXML
    private ProgressIndicator loadingIndicator;
    @FXML
    private VBox dataIncompleteWarningBox;
    @FXML
    private Label dataIncompleteWarningLabel;
    
    // Constants
    private static final float VAT_RATE = 0.10f;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Logger logger = Logger.getLogger(OrderSummaryController.class.getName());


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
        logger.info("OrderSummaryController.initialize: Starting enhanced initialization");
        
        try {
            // Initialize enhanced services
            initializeServices();
            
            // Initialize UI components
            initializeUIComponents();
            
            // Validate critical UI components
            validateUIComponents();
            
            logger.info("OrderSummaryController.initialize: Enhanced initialization completed successfully");
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "OrderSummaryController.initialize: CRITICAL initialization error", e);
            showNavigationError("Failed to initialize order summary screen. Please try refreshing the page.");
        }
    }
    
    /**
     * Initialize enhanced services with dependency injection
     */
    private void initializeServices() {
        try {
            ServiceFactory serviceFactory = ServiceFactory.getInstance();
            this.orderDataLoaderService = serviceFactory.getOrderDataLoaderService();
            this.cartDataValidationService = serviceFactory.getCartDataValidationService();
            this.orderValidationService = serviceFactory.getOrderValidationService();
            
            logger.info("OrderSummaryController.initializeServices: Enhanced services initialized successfully");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "OrderSummaryController.initializeServices: Failed to initialize services", e);
            throw new RuntimeException("Service initialization failed", e);
        }
    }
    
    /**
     * Initialize UI components with enhanced configuration
     */
    private void initializeUIComponents() {
        // Initialize error display
        if (errorMessageLabel != null) {
            errorMessageLabel.setText("");
            errorMessageLabel.setVisible(false);
        }
        
        // Initialize loading indicator (if available)
        if (loadingIndicator != null) {
            loadingIndicator.setVisible(false);
            loadingIndicator.setManaged(false);
        }
        
        // Initialize data incomplete warning (if available)
        if (dataIncompleteWarningBox != null) {
            dataIncompleteWarningBox.setVisible(false);
            dataIncompleteWarningBox.setManaged(false);
        }
        
        logger.info("OrderSummaryController.initializeUIComponents: UI components initialized");
    }
    
    /**
     * Validate critical UI components are available
     */
    private void validateUIComponents() {
        if (orderIdLabel == null || orderDateLabel == null) {
            logger.severe("OrderSummaryController.validateUIComponents: CRITICAL - Essential UI components are null");
            throw new RuntimeException("Screen initialization failed. Essential UI components missing.");
        }
        
        if (proceedToPaymentMethodButton == null) {
            logger.severe("OrderSummaryController.validateUIComponents: CRITICAL - Payment button is null");
            throw new RuntimeException("Payment functionality is not available. Payment button missing.");
        }
        
        if (orderItemsVBox == null) {
            logger.severe("OrderSummaryController.validateUIComponents: CRITICAL - Order items container is null");
            throw new RuntimeException("Order items display is not available.");
        }
        
        logger.info("OrderSummaryController.validateUIComponents: Critical UI components validation passed");
    }

    /**
     * Enhanced setOrderData with comprehensive validation, fallback mechanisms, and DTO integration.
     * Integrates with OrderDataLoaderService for robust data loading and validation.
     */
    public void setOrderData(OrderEntity order) {
        logger.info("OrderSummaryController.setOrderData: Setting order data with enhanced validation and fallback mechanisms");
        
        try {
            if (order == null) {
                logger.severe("OrderSummaryController.setOrderData: Received null order");
                handleOrderDataError("Order data validation", new IllegalArgumentException("No order data received"));
                return;
            }
            
            // Show loading indicator for data processing
            showLoadingIndicator("Loading order data...");
            
            // Enhanced data loading with comprehensive fallbacks
            OrderEntity processedOrder = loadOrderDataWithFallbacks(order);
            
            // Create enhanced OrderSummaryDTO for structured data handling
            OrderSummaryDTO orderSummaryDTO = createOrderSummaryDTOWithFallbacks(processedOrder);
            
            // Store both entity and DTO for flexible access
            this.currentOrder = processedOrder;
            this.currentOrderSummaryDTO = orderSummaryDTO;
            
            // Enhanced validation with detailed feedback
            if (validateOrderDataComprehensively(processedOrder, orderSummaryDTO)) {
                // Enhanced UI population with progressive loading
                populateOrderSummaryEnhanced();
                enablePaymentProgression();
                
                logger.info("OrderSummaryController.setOrderData: Order data set successfully for Order ID: " + processedOrder.getOrderId());
            } else {
                logger.warning("OrderSummaryController.setOrderData: Order data validation failed, using fallback display");
                populateOrderSummaryWithFallbacks(processedOrder);
                showDataIncompleteWarning("Some order information may be incomplete. Please verify details before proceeding.");
            }
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "OrderSummaryController.setOrderData: Critical error processing order data", e);
            handleOrderDataError("Order data processing", e);
        } finally {
            hideLoadingIndicator();
        }
    }
    
    /**
     * Loads order data with comprehensive fallback mechanisms
     */
    private OrderEntity loadOrderDataWithFallbacks(OrderEntity order) {
        if (orderDataLoaderService == null) {
            logger.warning("OrderSummaryController.loadOrderDataWithFallbacks: OrderDataLoaderService not available, using basic order");
            return order;
        }
        
        try {
            // Validate data completeness first
            if (!orderDataLoaderService.validateOrderDataCompleteness(order)) {
                logger.info("OrderSummaryController.loadOrderDataWithFallbacks: Order data incomplete, attempting to refresh");
                try {
                    order = orderDataLoaderService.refreshOrderRelationships(order);
                    logger.info("OrderSummaryController.loadOrderDataWithFallbacks: Order relationships refreshed successfully");
                } catch (ResourceNotFoundException e) {
                    logger.warning("OrderSummaryController.loadOrderDataWithFallbacks: Failed to refresh order relationships: " + e.getMessage());
                    showDataIncompleteWarning("Some order information could not be loaded. Display may be incomplete.");
                }
            }
            
            // Validate lazy loading initialization
            if (!orderDataLoaderService.validateLazyLoadingInitialization(order)) {
                logger.warning("OrderSummaryController.loadOrderDataWithFallbacks: Lazy loading issues detected");
                try {
                    // Attempt to reload complete order data
                    order = orderDataLoaderService.loadCompleteOrderData(order.getOrderId());
                    logger.info("OrderSummaryController.loadOrderDataWithFallbacks: Complete order data loaded successfully");
                } catch (ResourceNotFoundException e) {
                    logger.warning("OrderSummaryController.loadOrderDataWithFallbacks: Could not reload complete order data: " + e.getMessage());
                }
            }
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "OrderSummaryController.loadOrderDataWithFallbacks: Error during enhanced loading", e);
        }
        
        return order;
    }
    
    /**
     * Creates OrderSummaryDTO with comprehensive fallback handling
     */
    private OrderSummaryDTO createOrderSummaryDTOWithFallbacks(OrderEntity order) {
        if (orderDataLoaderService == null) {
            logger.warning("OrderSummaryController.createOrderSummaryDTOWithFallbacks: OrderDataLoaderService not available");
            return null;
        }
        
        try {
            OrderSummaryDTO dto = orderDataLoaderService.createOrderSummaryDTO(order);
            logger.info("OrderSummaryController.createOrderSummaryDTOWithFallbacks: OrderSummaryDTO created successfully");
            return dto;
        } catch (ValidationException e) {
            logger.warning("OrderSummaryController.createOrderSummaryDTOWithFallbacks: OrderSummaryDTO validation failed: " + e.getMessage());
            // Continue with null DTO, will use entity-based fallbacks
            return null;
        } catch (Exception e) {
            logger.log(Level.WARNING, "OrderSummaryController.createOrderSummaryDTOWithFallbacks: Unexpected error creating DTO", e);
            return null;
        }
    }
    
    /**
     * Comprehensive validation of order data and DTO
     */
    private boolean validateOrderDataComprehensively(OrderEntity order, OrderSummaryDTO dto) {
        boolean entityValid = validateOrderData(order);
        boolean dtoValid = dto != null && validateOrderSummaryData(dto);
        
        if (entityValid && dtoValid) {
            logger.info("OrderSummaryController.validateOrderDataComprehensively: Both entity and DTO validation passed");
            return true;
        } else if (entityValid) {
            logger.info("OrderSummaryController.validateOrderDataComprehensively: Entity validation passed, DTO validation failed/skipped");
            return true;
        } else {
            logger.warning("OrderSummaryController.validateOrderDataComprehensively: Entity validation failed");
            return false;
        }
    }
    
    /**
     * Validates OrderSummaryDTO data completeness
     */
    private boolean validateOrderSummaryData(OrderSummaryDTO orderSummary) {
        if (orderSummary == null) {
            logger.warning("OrderSummaryController.validateOrderSummaryData: OrderSummary DTO is null");
            return false;
        }
        
        if (orderSummary.orderId() == null || orderSummary.orderId().trim().isEmpty()) {
            logger.warning("OrderSummaryController.validateOrderSummaryData: Order ID is missing in DTO");
            return false;
        }
        
        if (orderSummary.items() == null || orderSummary.items().isEmpty()) {
            logger.warning("OrderSummaryController.validateOrderSummaryData: Order items are missing in DTO");
            return false;
        }
        
        if (orderSummary.deliveryInfo() == null) {
            logger.warning("OrderSummaryController.validateOrderSummaryData: Delivery information is missing in DTO");
            return false;
        }
        
        if (orderSummary.totalAmountToBePaid() <= 0) {
            logger.warning("OrderSummaryController.validateOrderSummaryData: Total amount is invalid in DTO");
            return false;
        }
        
        logger.info("OrderSummaryController.validateOrderSummaryData: OrderSummary DTO validation passed");
        return true;
    }
    
    /**
     * Enhanced UI population using DTO when available, entity as fallback
     */
    private void populateOrderSummaryEnhanced() {
        logger.info("OrderSummaryController.populateOrderSummaryEnhanced: Starting enhanced order summary population");
        
        try {
            if (currentOrderSummaryDTO != null) {
                populateOrderSummaryFromDTO(currentOrderSummaryDTO);
            } else {
                populateOrderSummaryWithFallbacks(currentOrder);
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "OrderSummaryController.populateOrderSummaryEnhanced: Error in enhanced population, using fallback", e);
            populateOrderSummaryWithFallbacks(currentOrder);
        }
    }
    
    /**
     * Populate order summary from enhanced DTO
     */
    private void populateOrderSummaryFromDTO(OrderSummaryDTO orderSummary) {
        logger.info("OrderSummaryController.populateOrderSummaryFromDTO: Populating UI from OrderSummaryDTO");
        
        try {
            // Populate basic order information
            populateBasicOrderInfoFromDTO(orderSummary);
            
            // Populate delivery information with enhanced DTO data
            populateDeliveryInformationFromDTO(orderSummary.deliveryInfo(), orderSummary.rushDeliveryDetails());
            
            // Populate order items with enhanced DTO data
            populateOrderItemsFromDTO(orderSummary.items());
            
            // Populate pricing information with enhanced calculations
            populatePricingInformationFromDTO(orderSummary);
            
            logger.info("OrderSummaryController.populateOrderSummaryFromDTO: DTO-based population completed successfully");
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "OrderSummaryController.populateOrderSummaryFromDTO: Error populating from DTO", e);
            throw e; // Re-throw to trigger fallback
        }
    }
    
    /**
     * Populate basic order information from DTO
     */
    private void populateBasicOrderInfoFromDTO(OrderSummaryDTO orderSummary) {
        if (orderIdLabel != null) {
            orderIdLabel.setText(orderSummary.orderId() != null ? orderSummary.orderId() : "N/A");
        }
        
        if (orderDateLabel != null) {
            if (orderSummary.orderDate() != null) {
                orderDateLabel.setText(orderSummary.orderDate().format(DATE_TIME_FORMATTER));
            } else {
                orderDateLabel.setText("N/A");
            }
        }
        
        logger.info("OrderSummaryController.populateBasicOrderInfoFromDTO: Basic order info populated from DTO");
    }
    
    /**
     * Populate delivery information from enhanced DTO
     */
    private void populateDeliveryInformationFromDTO(DeliveryInfoDTO deliveryInfo,
                                                   com.aims.core.application.dtos.RushDeliveryDetailsDTO rushDetails) {
        try {
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
                    String deliveryMethod = deliveryInfo.isRushOrder() ? "RUSH_DELIVERY" : "STANDARD_DELIVERY";
                    deliveryMethodText.setText(deliveryMethod);
                }

                // Handle rush delivery display from DTO
                boolean isRushDelivery = deliveryInfo.isRushOrder() && rushDetails != null;
                
                if (rushTimeLabel != null) {
                    rushTimeLabel.setVisible(isRushDelivery);
                    rushTimeLabel.setManaged(isRushDelivery);
                }
                
                if (rushTimeText != null) {
                    if (isRushDelivery && deliveryInfo.getRequestedRushDeliveryTime() != null) {
                        rushTimeText.setText(deliveryInfo.getRequestedRushDeliveryTime().format(DATE_TIME_FORMATTER));
                    }
                    rushTimeText.setVisible(isRushDelivery);
                    rushTimeText.setManaged(isRushDelivery);
                }
            } else {
                logger.warning("OrderSummaryController.populateDeliveryInformationFromDTO: Delivery info DTO is null");
                showDataIncompleteWarning("Delivery information is missing from the order");
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "OrderSummaryController.populateDeliveryInformationFromDTO: Error populating delivery info from DTO", e);
            throw e;
        }
    }
    
    /**
     * Populate order items from enhanced DTO list
     */
    private void populateOrderItemsFromDTO(java.util.List<OrderItemDTO> items) {
        try {
            if (orderItemsVBox != null) {
                orderItemsVBox.getChildren().clear();
            } else {
                logger.warning("OrderSummaryController.populateOrderItemsFromDTO: orderItemsVBox is null");
                return;
            }
            
            if (items != null && !items.isEmpty()) {
                for (OrderItemDTO itemDTO : items) {
                    try {
                        // Attempt to load FXML partial
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/aims/presentation/views/partials/order_item_row.fxml"));
                        Parent itemNode = loader.load();
                        OrderItemRowController itemController = loader.getController();
                        
                        if (itemController != null) {
                            // Create OrderItem entity from DTO for compatibility
                            OrderItem orderItem = createOrderItemFromDTO(itemDTO);
                            itemController.setData(orderItem);
                            orderItemsVBox.getChildren().add(itemNode);
                        } else {
                            logger.warning("OrderSummaryController.populateOrderItemsFromDTO: ItemController is null, using fallback");
                            createFallbackItemDisplayFromDTO(itemDTO);
                        }

                    } catch (IOException e) {
                        logger.warning("OrderSummaryController.populateOrderItemsFromDTO: Error loading order_item_row.fxml: " + e.getMessage());
                        createFallbackItemDisplayFromDTO(itemDTO);
                    }
                }
            } else {
                logger.warning("OrderSummaryController.populateOrderItemsFromDTO: No order items found in DTO");
                showDataIncompleteWarning("No items found in the order");
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "OrderSummaryController.populateOrderItemsFromDTO: Error populating order items from DTO", e);
            throw e;
        }
    }
    
    /**
     * Create OrderItem entity from DTO for backward compatibility
     */
    private OrderItem createOrderItemFromDTO(OrderItemDTO itemDTO) {
        // This is a simplified conversion - in a real implementation,
        // you might want to create a proper entity or enhance the controller to work with DTOs
        try {
            // Create a basic OrderItem for compatibility
            OrderItem orderItem = new OrderItem();
            orderItem.setQuantity(itemDTO.getQuantity());
            orderItem.setPriceAtTimeOfOrder(itemDTO.getPriceAtTimeOfOrder());
            
            // Create a basic Product object
            com.aims.core.entities.Product product = new com.aims.core.entities.Product();
            product.setProductId(itemDTO.getProductId());
            product.setTitle(itemDTO.getTitle());
            product.setImageUrl(itemDTO.getImageUrl());
            
            orderItem.setProduct(product);
            
            return orderItem;
        } catch (Exception e) {
            logger.warning("OrderSummaryController.createOrderItemFromDTO: Error creating OrderItem from DTO: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Create fallback item display from DTO
     */
    private void createFallbackItemDisplayFromDTO(OrderItemDTO itemDTO) {
        try {
            VBox itemBox = new VBox(2);
            itemBox.setStyle("-fx-padding: 10; -fx-border-color: #e0e0e0; -fx-border-radius: 5;");
            
            String itemTitle = (itemDTO.getTitle() != null) ? itemDTO.getTitle() : "Unknown Product";
            
            Label title = new Label(itemTitle + " (x" + itemDTO.getQuantity() + ")");
            title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
            
            Label price = new Label(String.format("Price/unit: %,.0f VND, Total: %,.0f VND",
                                    itemDTO.getPriceAtTimeOfOrder(),
                                    itemDTO.getTotalPriceAtTimeOfOrder()));
            price.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");
            
            itemBox.getChildren().addAll(title, price);
            orderItemsVBox.getChildren().add(itemBox);
        } catch (Exception e) {
            logger.warning("OrderSummaryController.createFallbackItemDisplayFromDTO: Error creating fallback display from DTO: " + e.getMessage());
        }
    }
    
    /**
     * Populate pricing information from enhanced DTO
     */
    private void populatePricingInformationFromDTO(OrderSummaryDTO orderSummary) {
        try {
            if (subtotalExclVATLabel != null) {
                subtotalExclVATLabel.setText(String.format("%,.0f VND", orderSummary.totalProductPriceExclVAT()));
            }
            
            if (vatLabel != null) {
                vatLabel.setText(String.format("%,.0f VND", orderSummary.vatAmount()));
            }
            
            if (subtotalInclVATLabel != null) {
                subtotalInclVATLabel.setText(String.format("%,.0f VND", orderSummary.totalProductPriceInclVAT()));
            }
            
            if (shippingFeeLabel != null) {
                shippingFeeLabel.setText(String.format("%,.0f VND", orderSummary.deliveryFee()));
            }
            
            if (totalAmountPaidLabel != null) {
                totalAmountPaidLabel.setText(String.format("%,.0f VND", orderSummary.totalAmountToBePaid()));
            }
            
            logger.info("OrderSummaryController.populatePricingInformationFromDTO: Pricing information populated from DTO");
        } catch (Exception e) {
            logger.log(Level.WARNING, "OrderSummaryController.populatePricingInformationFromDTO: Error populating pricing from DTO", e);
            throw e;
        }
    }
    
    /**
     * Populate order summary with comprehensive fallback mechanisms
     */
    private void populateOrderSummaryWithFallbacks(OrderEntity order) {
        logger.info("OrderSummaryController.populateOrderSummaryWithFallbacks: Using entity-based fallback population");
        
        try {
            // Populate order basic information with null checks
            if (orderIdLabel != null) {
                orderIdLabel.setText(order.getOrderId() != null ? order.getOrderId() : "N/A");
            }
            
            if (orderDateLabel != null) {
                if (order.getOrderDate() != null) {
                    orderDateLabel.setText(order.getOrderDate().format(DATE_TIME_FORMATTER));
                } else {
                    orderDateLabel.setText("N/A");
                }
            }

            // Enhanced delivery information population with fallbacks
            populateDeliveryInformationWithFallbacks(order);
            
            // Enhanced order items population with fallbacks
            populateOrderItemsWithFallbacks(order);
            
            // Enhanced pricing information with validation
            populatePricingInformationWithFallbacks(order);
            
            logger.info("OrderSummaryController.populateOrderSummaryWithFallbacks: Fallback population completed successfully");
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "OrderSummaryController.populateOrderSummaryWithFallbacks: Critical error in fallback population", e);
            handleOrderDataError("UI population with fallbacks", e);
        }
    }
    
    /**
     * Enhanced delivery information population with fallbacks
     */
    private void populateDeliveryInformationWithFallbacks(OrderEntity order) {
        try {
            DeliveryInfo deliveryInfo = order.getDeliveryInfo();
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

                // Handle rush delivery display with enhanced logic
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
                logger.warning("OrderSummaryController.populateDeliveryInformationWithFallbacks: Delivery info is null");
                showDataIncompleteWarning("Delivery information is missing from the order");
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "OrderSummaryController.populateDeliveryInformationWithFallbacks: Error in fallback delivery population", e);
            // Continue with minimal display
            if (deliveryMethodText != null) {
                deliveryMethodText.setText("Information unavailable");
            }
        }
    }
    
    /**
     * Enhanced order items population with fallbacks
     */
    private void populateOrderItemsWithFallbacks(OrderEntity order) {
        try {
            if (orderItemsVBox != null) {
                orderItemsVBox.getChildren().clear();
            } else {
                logger.warning("OrderSummaryController.populateOrderItemsWithFallbacks: orderItemsVBox is null");
                return;
            }
            
            if (order.getOrderItems() != null && !order.getOrderItems().isEmpty()) {
                for (OrderItem item : order.getOrderItems()) {
                    try {
                        // Attempt to load FXML partial
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/aims/presentation/views/partials/order_item_row.fxml"));
                        Parent itemNode = loader.load();
                        OrderItemRowController itemController = loader.getController();
                        
                        if (itemController != null) {
                            itemController.setData(item);
                            orderItemsVBox.getChildren().add(itemNode);
                        } else {
                            logger.warning("OrderSummaryController.populateOrderItemsWithFallbacks: ItemController is null, using fallback");
                            createFallbackItemDisplay(item);
                        }

                    } catch (IOException e) {
                        logger.warning("OrderSummaryController.populateOrderItemsWithFallbacks: Error loading order_item_row.fxml: " + e.getMessage());
                        createFallbackItemDisplay(item);
                    }
                }
            } else {
                logger.warning("OrderSummaryController.populateOrderItemsWithFallbacks: No order items found");
                showDataIncompleteWarning("No items found in the order");
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "OrderSummaryController.populateOrderItemsWithFallbacks: Error in fallback items population", e);
        }
    }
    
    /**
     * Enhanced pricing information population with fallbacks
     */
    private void populatePricingInformationWithFallbacks(OrderEntity order) {
        try {
            if (subtotalExclVATLabel != null) {
                subtotalExclVATLabel.setText(String.format("%,.0f VND", order.getTotalProductPriceExclVAT()));
            }
            
            if (vatLabel != null) {
                float vatAmount = order.getTotalProductPriceInclVAT() - order.getTotalProductPriceExclVAT();
                vatLabel.setText(String.format("%,.0f VND", vatAmount));
            }
            
            if (subtotalInclVATLabel != null) {
                subtotalInclVATLabel.setText(String.format("%,.0f VND", order.getTotalProductPriceInclVAT()));
            }
            
            if (shippingFeeLabel != null) {
                shippingFeeLabel.setText(String.format("%,.0f VND", order.getCalculatedDeliveryFee()));
            }
            
            if (totalAmountPaidLabel != null) {
                totalAmountPaidLabel.setText(String.format("%,.0f VND", order.getTotalAmountPaid()));
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "OrderSummaryController.populatePricingInformationWithFallbacks: Error in fallback pricing population", e);
            // Set fallback values
            if (totalAmountPaidLabel != null) {
                totalAmountPaidLabel.setText("Amount calculation unavailable");
            }
        }
    }
    
    /**
     * Show loading indicator with message
     */
    private void showLoadingIndicator(String message) {
        if (loadingIndicator != null) {
            loadingIndicator.setVisible(true);
            loadingIndicator.setManaged(true);
        }
        logger.info("OrderSummaryController.showLoadingIndicator: " + message);
    }
    
    /**
     * Hide loading indicator
     */
    private void hideLoadingIndicator() {
        if (loadingIndicator != null) {
            loadingIndicator.setVisible(false);
            loadingIndicator.setManaged(false);
        }
    }
    
    /**
     * Show data incomplete warning to user
     */
    private void showDataIncompleteWarning(String message) {
        if (dataIncompleteWarningBox != null && dataIncompleteWarningLabel != null) {
            dataIncompleteWarningLabel.setText(message);
            dataIncompleteWarningBox.setVisible(true);
            dataIncompleteWarningBox.setManaged(true);
        } else {
            // Fallback to error message label if warning components not available
            if (errorMessageLabel != null) {
                errorMessageLabel.setText("Warning: " + message);
                errorMessageLabel.setVisible(true);
                errorMessageLabel.setManaged(true);
            }
        }
        logger.warning("OrderSummaryController.showDataIncompleteWarning: " + message);
    }
    
    /**
     * Handle order data errors with comprehensive error management
     */
    private void handleOrderDataError(String context, Exception error) {
        String errorMessage = "Error in " + context + ": " + error.getMessage();
        logger.log(Level.SEVERE, "OrderSummaryController.handleOrderDataError: " + errorMessage, error);
        
        // Display user-friendly error message
        showNavigationError(errorMessage);
        
        // Disable payment functionality
        if (proceedToPaymentMethodButton != null) {
            proceedToPaymentMethodButton.setDisable(true);
        }
        
        // Hide loading indicators
        hideLoadingIndicator();
        
        // Clear any existing data to prevent confusion
        clearOrderDisplay();
    }
    
    /**
     * Enable payment progression after successful data loading
     */
    private void enablePaymentProgression() {
        if (proceedToPaymentMethodButton != null) {
            proceedToPaymentMethodButton.setDisable(false);
        }
        
        // Hide any warning messages
        if (dataIncompleteWarningBox != null) {
            dataIncompleteWarningBox.setVisible(false);
            dataIncompleteWarningBox.setManaged(false);
        }
        
        // Clear any error messages
        if (errorMessageLabel != null) {
            errorMessageLabel.setVisible(false);
            errorMessageLabel.setManaged(false);
        }
        
        logger.info("OrderSummaryController.enablePaymentProgression: Payment progression enabled");
    }
    
    /**
     * Clear order display in case of errors
     */
    private void clearOrderDisplay() {
        try {
            if (orderIdLabel != null) orderIdLabel.setText("N/A");
            if (orderDateLabel != null) orderDateLabel.setText("N/A");
            if (orderItemsVBox != null) orderItemsVBox.getChildren().clear();
            
            if (recipientNameText != null) recipientNameText.setText("N/A");
            if (phoneText != null) phoneText.setText("N/A");
            if (emailText != null) emailText.setText("N/A");
            if (addressText != null) addressText.setText("N/A");
            if (provinceCityText != null) provinceCityText.setText("N/A");
            if (instructionsText != null) instructionsText.setText("N/A");
            if (deliveryMethodText != null) deliveryMethodText.setText("N/A");
            
            if (subtotalExclVATLabel != null) subtotalExclVATLabel.setText("0 VND");
            if (vatLabel != null) vatLabel.setText("0 VND");
            if (subtotalInclVATLabel != null) subtotalInclVATLabel.setText("0 VND");
            if (shippingFeeLabel != null) shippingFeeLabel.setText("0 VND");
            if (totalAmountPaidLabel != null) totalAmountPaidLabel.setText("0 VND");
            
            logger.info("OrderSummaryController.clearOrderDisplay: Order display cleared");
        } catch (Exception e) {
            logger.log(Level.WARNING, "OrderSummaryController.clearOrderDisplay: Error clearing display", e);
        }
    }
    
    /**
     * Enhanced service injection method for dependency management
     */
    public void setOrderDataLoaderService(IOrderDataLoaderService orderDataLoaderService) {
        this.orderDataLoaderService = orderDataLoaderService;
        logger.info("OrderSummaryController.setOrderDataLoaderService: OrderDataLoaderService injected");
    }
    
    /**
     * Enhanced service injection method for cart validation
     */
    public void setCartDataValidationService(ICartDataValidationService cartDataValidationService) {
        this.cartDataValidationService = cartDataValidationService;
        logger.info("OrderSummaryController.setCartDataValidationService: CartDataValidationService injected");
    }
    
    /**
     * Enhanced service injection method for order validation
     */
    public void setOrderValidationService(IOrderValidationService orderValidationService) {
        this.orderValidationService = orderValidationService;
        logger.info("OrderSummaryController.setOrderValidationService: OrderValidationService injected");
    }
    
    /**
     * Enhanced scene manager injection
     */
    public void setSceneManager(FXMLSceneManager sceneManager) {
        this.sceneManager = sceneManager;
        logger.info("OrderSummaryController.setSceneManager: FXMLSceneManager injected");
    }
    
    /**
     * Asynchronous data loading for progressive enhancement
     */
    private void loadOrderDataAsync(String orderId) {
        if (orderDataLoaderService == null) {
            logger.warning("OrderSummaryController.loadOrderDataAsync: OrderDataLoaderService not available");
            return;
        }
        
        showLoadingIndicator("Loading complete order data...");
        
        // Create background task for data loading
        Task<OrderEntity> loadOrderTask = new Task<OrderEntity>() {
            @Override
            protected OrderEntity call() throws Exception {
                return orderDataLoaderService.loadCompleteOrderData(orderId);
            }
            
            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    try {
                        OrderEntity loadedOrder = getValue();
                        if (loadedOrder != null) {
                            setOrderData(loadedOrder);
                            logger.info("OrderSummaryController.loadOrderDataAsync: Async order loading completed successfully");
                        } else {
                            handleOrderDataError("Async order loading", new ResourceNotFoundException("Order not found: " + orderId));
                        }
                    } catch (Exception e) {
                        handleOrderDataError("Async order loading result processing", e);
                    } finally {
                        hideLoadingIndicator();
                    }
                });
            }
            
            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    Throwable exception = getException();
                    handleOrderDataError("Async order loading",
                        exception instanceof Exception ? (Exception) exception : new RuntimeException(exception));
                    hideLoadingIndicator();
                });
            }
        };
        
        // Execute task in background thread
        Thread loadOrderThread = new Thread(loadOrderTask);
        loadOrderThread.setDaemon(true);
        loadOrderThread.setName("OrderDataLoader-" + orderId);
        loadOrderThread.start();
    }
    
    /**
     * Enhanced asynchronous data loading with user feedback
     */
    public void loadOrderDataAsyncWithFeedback(String orderId) {
        logger.info("OrderSummaryController.loadOrderDataAsyncWithFeedback: Starting async loading for order: " + orderId);
        loadOrderDataAsync(orderId);
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
     * Legacy pricing information population method
     * @deprecated Use populatePricingInformationWithFallbacks() for enhanced functionality
     */
    @Deprecated
    private void populatePricingInformation() {
        logger.info("OrderSummaryController.populatePricingInformation: Using legacy pricing population (deprecated)");
        populatePricingInformationWithFallbacks(currentOrder);
    }


    @FXML
    void handleBackToDeliveryInfoAction(ActionEvent event) {
        logger.info("OrderSummaryController.handleBackToDeliveryInfoAction: Back to Delivery Info action triggered with Enhanced Navigation Manager");
        
        try {
            // PHASE 1 FIX: Enhanced validation with registry fallback
            MainLayoutController validatedController = validateMainLayoutControllerWithFallback();
            if (validatedController == null) {
                logger.severe("OrderSummaryController.handleBackToDeliveryInfoAction: MainLayoutController not available after all fallback attempts");
                handleOrderDataError("Navigation validation", new IllegalStateException("MainLayoutController not available for navigation"));
                return;
            }
            
            // Update local reference if it was obtained from registry
            if (mainLayoutController == null) {
                mainLayoutController = validatedController;
                logger.info("OrderSummaryController.handleBackToDeliveryInfoAction: MainLayoutController obtained from registry");
            }
            
            if (currentOrder == null) {
                logger.severe("OrderSummaryController.handleBackToDeliveryInfoAction: Order data not available");
                handleOrderDataError("Navigation validation", new IllegalStateException("Order data not available for navigation"));
                return;
            }
            
            // Validate order data before navigation to ensure consistency
            if (!validateOrderData(currentOrder)) {
                logger.warning("OrderSummaryController.handleBackToDeliveryInfoAction: Order data validation failed before navigation");
                showDataIncompleteWarning("Order data validation failed. Navigation may not preserve all information.");
            }
            
            // PHASE 2: Use Unified Navigation Manager for robust, consolidated navigation
            UnifiedNavigationManager.NavigationResult result = UnifiedNavigationManager.navigateToDeliveryInfo(
                currentOrder, this);
            
            if (result.isSuccess()) {
                logger.info("OrderSummaryController.handleBackToDeliveryInfoAction: Unified navigation successful: " + result.getDescription());
            } else if (result.hasDataPreservation()) {
                logger.warning("OrderSummaryController.handleBackToDeliveryInfoAction: Navigation failed but data was preserved: " + result.getDescription());
                showDataIncompleteWarning("Navigation encountered issues but your order data has been preserved. You can try again.");
            } else {
                logger.warning("OrderSummaryController.handleBackToDeliveryInfoAction: Unified navigation failed, using enhanced fallback");
                
                // Enhanced fallback with validation
                MainLayoutController fallbackController = validateMainLayoutControllerWithFallback();
                if (fallbackController != null) {
                    Object controller = fallbackController.loadContent("/com/aims/presentation/views/delivery_info_screen.fxml");
                    fallbackController.setHeaderTitle("Delivery Information");
                    
                    if (controller instanceof DeliveryInfoScreenController) {
                        DeliveryInfoScreenController deliveryController = (DeliveryInfoScreenController) controller;
                        deliveryController.setOrderData(currentOrder);
                        logger.info("OrderSummaryController.handleBackToDeliveryInfoAction: Order data passed via enhanced fallback");
                    } else {
                        logger.warning("OrderSummaryController.handleBackToDeliveryInfoAction: Enhanced fallback controller validation failed");
                        showDataIncompleteWarning("Navigation completed but order data transfer may have failed.");
                    }
                } else {
                    logger.severe("OrderSummaryController.handleBackToDeliveryInfoAction: All navigation strategies failed");
                    handleOrderDataError("Navigation to delivery info", new IllegalStateException("All navigation strategies failed"));
                }
            }
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "OrderSummaryController.handleBackToDeliveryInfoAction: Error navigating back to delivery info", e);
            handleOrderDataError("Navigation to delivery info", e);
        }
    }

    @FXML
    void handleProceedToPaymentMethodAction(ActionEvent event) {
        logger.info("OrderSummaryController.handleProceedToPaymentMethodAction: Proceeding to payment method with enhanced validation");
        
        try {
            // PHASE 1 FIX: Enhanced validation with MainLayoutController registry check
            MainLayoutController validatedController = validateMainLayoutControllerWithFallback();
            if (validatedController == null) {
                logger.severe("PAYMENT_FLOW_ERROR: CRITICAL - MainLayoutController not available for payment navigation");
                handleOrderDataError("Payment navigation validation",
                    new IllegalStateException("MainLayoutController not available for payment navigation"));
                return;
            }
            
            // Update local reference if it was obtained from registry
            if (mainLayoutController == null) {
                mainLayoutController = validatedController;
                logger.info("OrderSummaryController.handleProceedToPaymentMethodAction: MainLayoutController obtained from registry");
            }
            
            // Enhanced pre-payment validation with comprehensive checks
            if (!validatePrePaymentConditions()) {
                return; // Error handling done in validation method
            }
            
            // Use enhanced order validation service if available
            if (orderDataLoaderService != null) {
                try {
                    if (!orderDataLoaderService.validateOrderDataCompleteness(currentOrder)) {
                        logger.warning("OrderSummaryController.handleProceedToPaymentMethodAction: Order data completeness validation failed");
                        showDataIncompleteWarning("Order data may be incomplete. Please verify all information before proceeding to payment.");
                    }
                } catch (Exception e) {
                    logger.log(Level.WARNING, "OrderSummaryController.handleProceedToPaymentMethodAction: Error in order data validation", e);
                }
            }
            
            logger.info("PAYMENT_FLOW: OrderSummary -> PaymentMethod for Order " + currentOrder.getOrderId());
            logger.info("PAYMENT_FLOW: Enhanced validation passed, proceeding to payment method selection with Unified Navigation");
            
            // PHASE 2: Use Unified Navigation Manager for consolidated, reliable navigation
            UnifiedNavigationManager.NavigationResult result = UnifiedNavigationManager.navigateToPaymentMethod(
                currentOrder, this);
            
            if (result.isSuccess()) {
                logger.info("OrderSummaryController.handleProceedToPaymentMethodAction: Unified navigation successful: " + result.getDescription());
            } else if (result.hasDataPreservation()) {
                logger.warning("OrderSummaryController.handleProceedToPaymentMethodAction: Navigation failed but data preserved: " + result.getDescription());
                showDataIncompleteWarning("Navigation encountered issues but your order data has been preserved. Please try again.");
            } else {
                logger.warning("OrderSummaryController.handleProceedToPaymentMethodAction: Unified navigation failed, using enhanced fallback");
                
                // Enhanced fallback with comprehensive validation
                if (!attemptEnhancedPaymentNavigation()) {
                    // Final fallback attempt
                    attemptFallbackPaymentNavigation();
                }
            }
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "OrderSummaryController.handleProceedToPaymentMethodAction: Critical error in payment navigation", e);
            handleOrderDataError("Payment method navigation", e);
        }
    }
    
    /**
     * Enhanced pre-payment validation using OrderValidationService with comprehensive fallback logic.
     * This method replaces manual validation checks with service-based validation that handles:
     * - Lazy loading issues with delivery info
     * - Database connectivity problems
     * - Order data consistency checks
     * - Comprehensive business rule validation
     */
    private boolean validatePrePaymentConditions() {
        logger.info("OrderSummaryController.validatePrePaymentConditions: Starting enhanced pre-payment validation");
        
        // STEP 1: Basic null checks
        if (currentOrder == null) {
            logger.severe("PAYMENT_FLOW_ERROR: No order data to proceed with payment");
            handleOrderDataError("Pre-payment validation", new IllegalStateException("Order information is not available"));
            return false;
        }
        
        String orderId = currentOrder.getOrderId();
        if (orderId == null || orderId.trim().isEmpty()) {
            logger.severe("PAYMENT_FLOW_ERROR: Order ID is missing");
            handleOrderDataError("Pre-payment validation", new IllegalStateException("Order ID is missing"));
            return false;
        }
        
        // STEP 2: Use OrderValidationService for robust validation
        if (orderValidationService != null) {
            logger.info("OrderSummaryController.validatePrePaymentConditions: Using OrderValidationService for validation");
            
            try {
                // Use the service's isOrderReadyForPayment method which handles lazy loading and edge cases
                boolean isReady = orderValidationService.isOrderReadyForPayment(orderId);
                
                if (isReady) {
                    logger.info("PAYMENT_FLOW: OrderValidationService confirmed order is ready for payment: " + orderId);
                    
                    // Optional: Reload order data to ensure we have the latest state
                    try {
                        OrderEntity refreshedOrder = orderValidationService.getValidatedOrderForPayment(orderId);
                        if (refreshedOrder != null) {
                            // Update current order with refreshed data to prevent stale data issues
                            this.currentOrder = refreshedOrder;
                            logger.info("OrderSummaryController.validatePrePaymentConditions: Order data refreshed from validation service");
                        }
                    } catch (ValidationException ve) {
                        logger.warning("OrderSummaryController.validatePrePaymentConditions: Order validation failed: " + ve.getMessage());
                        handleOrderDataError("Order validation", ve);
                        return false;
                    }
                    
                    return true;
                } else {
                    logger.warning("PAYMENT_FLOW_ERROR: OrderValidationService indicates order is not ready for payment: " + orderId);
                    handleOrderDataError("Pre-payment validation",
                        new IllegalStateException("Order is not ready for payment. Please ensure delivery information is complete."));
                    return false;
                }
                
            } catch (java.sql.SQLException e) {
                logger.log(Level.WARNING, "OrderSummaryController.validatePrePaymentConditions: Database error during validation", e);
                // Fall back to manual validation
                logger.info("OrderSummaryController.validatePrePaymentConditions: Falling back to manual validation due to database error");
                return performManualValidationFallback();
                
            } catch (ResourceNotFoundException e) {
                logger.warning("OrderSummaryController.validatePrePaymentConditions: Order not found during validation: " + e.getMessage());
                handleOrderDataError("Pre-payment validation", e);
                return false;
                
            } catch (Exception e) {
                logger.log(Level.WARNING, "OrderSummaryController.validatePrePaymentConditions: Unexpected error in OrderValidationService", e);
                // Fall back to manual validation
                logger.info("OrderSummaryController.validatePrePaymentConditions: Falling back to manual validation due to service error");
                return performManualValidationFallback();
            }
        } else {
            logger.warning("OrderSummaryController.validatePrePaymentConditions: OrderValidationService not available, using manual validation fallback");
            return performManualValidationFallback();
        }
    }
    
    /**
     * Manual validation fallback for cases where OrderValidationService is unavailable or fails.
     * Provides basic validation with improved error handling and user-friendly messages.
     */
    private boolean performManualValidationFallback() {
        logger.info("OrderSummaryController.performManualValidationFallback: Performing manual validation with enhanced checks");
        
        try {
            // Enhanced delivery info validation with reload attempt
            if (currentOrder.getDeliveryInfo() == null) {
                logger.warning("PAYMENT_FLOW_FALLBACK: Delivery info is null, this may be a lazy loading issue");
                
                // Try to reload order data if possible
                if (orderDataLoaderService != null) {
                    try {
                        logger.info("OrderSummaryController.performManualValidationFallback: Attempting to reload order data");
                        OrderEntity reloadedOrder = orderDataLoaderService.loadCompleteOrderData(currentOrder.getOrderId());
                        if (reloadedOrder != null && reloadedOrder.getDeliveryInfo() != null) {
                            this.currentOrder = reloadedOrder;
                            logger.info("OrderSummaryController.performManualValidationFallback: Successfully reloaded order with delivery info");
                        } else {
                            logger.warning("OrderSummaryController.performManualValidationFallback: Reload did not resolve delivery info issue");
                            handleOrderDataError("Manual validation fallback",
                                new IllegalStateException("Delivery information is missing. Please go back and complete delivery details."));
                            return false;
                        }
                    } catch (ResourceNotFoundException e) {
                        logger.warning("OrderSummaryController.performManualValidationFallback: Order reload failed: " + e.getMessage());
                        handleOrderDataError("Manual validation fallback",
                            new IllegalStateException("Unable to verify order information. Please try refreshing the page."));
                        return false;
                    }
                } else {
                    logger.severe("PAYMENT_FLOW_FALLBACK: Delivery info missing and no reload service available");
                    handleOrderDataError("Manual validation fallback",
                        new IllegalStateException("Delivery information is missing. Please go back and complete delivery details."));
                    return false;
                }
            }
            
            // Enhanced order items validation
            if (currentOrder.getOrderItems() == null || currentOrder.getOrderItems().isEmpty()) {
                logger.severe("PAYMENT_FLOW_FALLBACK: Order items missing for Order " + currentOrder.getOrderId());
                handleOrderDataError("Manual validation fallback",
                    new IllegalStateException("No items found in order. Please add items to your cart and try again."));
                return false;
            }
            
            // Enhanced amount validation
            if (currentOrder.getTotalAmountPaid() <= 0) {
                logger.severe("PAYMENT_FLOW_FALLBACK: Invalid total amount for Order " + currentOrder.getOrderId());
                handleOrderDataError("Manual validation fallback",
                    new IllegalStateException("Order total amount is invalid. Please refresh your cart and try again."));
                return false;
            }
            
            // Additional business rule checks
            try {
                validateDeliveryInfoCompleteness(currentOrder.getDeliveryInfo());
                validateOrderItemsIntegrity(currentOrder.getOrderItems());
                
                logger.info("OrderSummaryController.performManualValidationFallback: Manual validation passed for order: " + currentOrder.getOrderId());
                return true;
                
            } catch (ValidationException e) {
                logger.warning("OrderSummaryController.performManualValidationFallback: Business rule validation failed: " + e.getMessage());
                handleOrderDataError("Manual validation fallback", e);
                return false;
            }
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "OrderSummaryController.performManualValidationFallback: Unexpected error during manual validation", e);
            handleOrderDataError("Manual validation fallback", e);
            return false;
        }
    }
    
    /**
     * Validates delivery info completeness for manual fallback
     */
    private void validateDeliveryInfoCompleteness(DeliveryInfo deliveryInfo) throws ValidationException {
        if (deliveryInfo.getRecipientName() == null || deliveryInfo.getRecipientName().trim().isEmpty()) {
            throw new ValidationException("Recipient name is required for delivery.");
        }
        
        if (deliveryInfo.getDeliveryAddress() == null || deliveryInfo.getDeliveryAddress().trim().isEmpty()) {
            throw new ValidationException("Delivery address is required.");
        }
        
        if (deliveryInfo.getPhoneNumber() == null || deliveryInfo.getPhoneNumber().trim().isEmpty()) {
            throw new ValidationException("Phone number is required for delivery contact.");
        }
        
        if (deliveryInfo.getDeliveryProvinceCity() == null || deliveryInfo.getDeliveryProvinceCity().trim().isEmpty()) {
            throw new ValidationException("Province/City is required for delivery.");
        }
    }
    
    /**
     * Validates order items integrity for manual fallback
     */
    private void validateOrderItemsIntegrity(java.util.List<OrderItem> orderItems) throws ValidationException {
        for (OrderItem item : orderItems) {
            if (item.getProduct() == null) {
                throw new ValidationException("Order contains invalid items. Please refresh your cart.");
            }
            
            if (item.getQuantity() <= 0) {
                throw new ValidationException("All order items must have positive quantities.");
            }
            
            if (item.getPriceAtTimeOfOrder() <= 0) {
                throw new ValidationException("Order contains items with invalid prices. Please refresh your cart.");
            }
        }
    }
    
    /**
     * Enhanced navigation to payment method screen
     */
    private boolean navigateToPaymentMethod() {
        // PHASE 1 FIX: Use registry for MainLayoutController validation
        MainLayoutController validatedController = validateMainLayoutControllerWithFallback();
        if (validatedController == null) {
            logger.severe("PAYMENT_FLOW_ERROR: CRITICAL - MainLayoutController not available for navigation after all attempts");
            return false;
        }
        
        // Update local reference
        mainLayoutController = validatedController;
        
        try {
            Object controller = mainLayoutController.loadContent("/com/aims/presentation/views/payment_method_screen.fxml");
            mainLayoutController.setHeaderTitle("Select Payment Method");
            
            // Enhanced controller validation and injection
            if (controller instanceof PaymentMethodScreenController) {
                PaymentMethodScreenController paymentController = (PaymentMethodScreenController) controller;
                paymentController.setOrderData(currentOrder);
                
                logger.info("PAYMENT_FLOW: Order data successfully injected into PaymentMethodScreenController");
                logger.info("PAYMENT_FLOW: Navigation completed successfully");
                return true;
            } else {
                logger.severe("PAYMENT_FLOW_ERROR: Controller injection failed - invalid controller type: " +
                             (controller != null ? controller.getClass().getSimpleName() : "null"));
                handleOrderDataError("Payment screen controller validation",
                    new IllegalStateException("Failed to initialize payment screen properly. Controller type mismatch."));
                return false;
            }
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "PAYMENT_FLOW_ERROR: Navigation exception", e);
            handleOrderDataError("Payment method screen loading", e);
            return false;
        }
    }
    
    /**
     * PHASE 2: Enhanced payment navigation using validated MainLayoutController
     */
    private boolean attemptEnhancedPaymentNavigation() {
        try {
            MainLayoutController enhancedController = validateMainLayoutControllerWithFallback();
            if (enhancedController != null) {
                Object controller = enhancedController.loadContent("/com/aims/presentation/views/payment_method_screen.fxml");
                enhancedController.setHeaderTitle("Select Payment Method");
                
                if (controller instanceof PaymentMethodScreenController) {
                    PaymentMethodScreenController paymentController = (PaymentMethodScreenController) controller;
                    paymentController.setOrderData(currentOrder);
                    paymentController.setMainLayoutController(enhancedController);
                    
                    logger.info("PAYMENT_FLOW: Enhanced navigation completed successfully");
                    return true;
                } else {
                    logger.warning("PAYMENT_FLOW: Enhanced navigation controller validation failed");
                    return false;
                }
            } else {
                logger.warning("PAYMENT_FLOW: No MainLayoutController available for enhanced navigation");
                return false;
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "PAYMENT_FLOW: Enhanced navigation failed", e);
            return false;
        }
    }
    
    /**
     * Final fallback navigation using NavigationService
     */
    private void attemptFallbackPaymentNavigation() {
        try {
            logger.warning("PAYMENT_FLOW_FALLBACK: Attempting NavigationService final fallback");
            NavigationService.navigateToPaymentMethod(currentOrder, mainLayoutController);
            logger.info("PAYMENT_FLOW_FALLBACK: NavigationService fallback succeeded");
        } catch (Exception fallbackException) {
            logger.log(Level.SEVERE, "PAYMENT_FLOW_ERROR: All navigation strategies failed", fallbackException);
            handleOrderDataError("All payment navigation strategies failed", fallbackException);
        }
    }
    
    private void showNavigationError(String details) {
        errorMessageLabel.setText("Navigation Error: " + details);
        errorMessageLabel.setVisible(true);
        errorMessageLabel.setManaged(true);
        proceedToPaymentMethodButton.setDisable(true);
        
        System.err.println("OrderSummaryController: " + details);
    }
    
    /**
     * PHASE 1 FIX: Validates MainLayoutController with comprehensive fallback strategies.
     * Uses MainLayoutControllerRegistry as primary source with multiple validation attempts.
     *
     * @return A valid MainLayoutController instance, or null if none available
     */
    private MainLayoutController validateMainLayoutControllerWithFallback() {
        try {
            logger.fine("OrderSummaryController.validateMainLayoutControllerWithFallback: Starting validation");
            
            // Strategy 1: Check current instance
            if (mainLayoutController != null) {
                try {
                    // Quick validation
                    if (mainLayoutController.getContentPane() != null && mainLayoutController.getMainContainer() != null) {
                        logger.fine("OrderSummaryController.validateMainLayoutControllerWithFallback: Current instance is valid");
                        return mainLayoutController;
                    } else {
                        logger.warning("OrderSummaryController.validateMainLayoutControllerWithFallback: Current instance failed validation");
                        mainLayoutController = null; // Clear invalid reference
                    }
                } catch (Exception e) {
                    logger.log(Level.WARNING, "OrderSummaryController.validateMainLayoutControllerWithFallback: Current instance validation error", e);
                    mainLayoutController = null; // Clear problematic reference
                }
            }
            
            // Strategy 2: Get from MainLayoutControllerRegistry
            try {
                MainLayoutController registryController = MainLayoutControllerRegistry.getInstance(3, java.util.concurrent.TimeUnit.SECONDS);
                if (registryController != null) {
                    logger.info("OrderSummaryController.validateMainLayoutControllerWithFallback: Successfully obtained controller from registry");
                    return registryController;
                } else {
                    logger.warning("OrderSummaryController.validateMainLayoutControllerWithFallback: Registry returned null controller");
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "OrderSummaryController.validateMainLayoutControllerWithFallback: Registry access failed", e);
            }
            
            // Strategy 3: Force registry re-validation
            try {
                if (MainLayoutControllerRegistry.revalidate()) {
                    MainLayoutController revalidatedController = MainLayoutControllerRegistry.getInstanceImmediate();
                    if (revalidatedController != null) {
                        logger.info("OrderSummaryController.validateMainLayoutControllerWithFallback: Successfully obtained controller after re-validation");
                        return revalidatedController;
                    }
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "OrderSummaryController.validateMainLayoutControllerWithFallback: Re-validation failed", e);
            }
            
            // All strategies failed
            logger.severe("OrderSummaryController.validateMainLayoutControllerWithFallback: All validation strategies failed");
            logMainLayoutControllerDebugInfo();
            return null;
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "OrderSummaryController.validateMainLayoutControllerWithFallback: Unexpected error during validation", e);
            return null;
        }
    }
    
    /**
     * Logs comprehensive debug information about MainLayoutController state for troubleshooting.
     */
    private void logMainLayoutControllerDebugInfo() {
        try {
            StringBuilder debugInfo = new StringBuilder();
            debugInfo.append("OrderSummaryController MainLayoutController Debug Info:\n");
            debugInfo.append("Local Instance: ").append(mainLayoutController != null ? "available" : "null").append("\n");
            debugInfo.append("Registry Debug Info:\n").append(MainLayoutControllerRegistry.getDebugInfo()).append("\n");
            
            // Check if we have access to SceneManager
            if (sceneManager != null) {
                debugInfo.append("SceneManager: available\n");
            } else {
                debugInfo.append("SceneManager: null\n");
            }
            
            logger.severe("OrderSummaryController.logMainLayoutControllerDebugInfo: " + debugInfo.toString());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "OrderSummaryController.logMainLayoutControllerDebugInfo: Error generating debug info", e);
        }
    }
}