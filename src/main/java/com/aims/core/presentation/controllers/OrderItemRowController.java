package com.aims.core.presentation.controllers;

import com.aims.core.entities.OrderItem;
import com.aims.core.entities.Product;
import com.aims.core.application.dtos.OrderItemDTO;
import com.aims.core.presentation.utils.OrderSummaryUIHelper;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Enhanced OrderItemRowController with support for both entity and DTO data structures
 *
 * Provides comprehensive display capabilities with enhanced error handling and fallback mechanisms.
 */
public class OrderItemRowController {

    @FXML
    private HBox orderItemRowPane;
    @FXML
    private ImageView productImageView;
    @FXML
    private Label productTitleLabel;
    @FXML
    private Label productIdLabel;
    @FXML
    private Label quantityLabel;
    @FXML
    private Label unitPriceLabel;
    @FXML
    private Label totalItemPriceLabel;
    @FXML
    private Label rushDeliveryEligibleLabel; // New field for rush delivery eligibility

    private OrderItem orderItem;
    private OrderItemDTO orderItemDTO;
    
    private static final Logger logger = Logger.getLogger(OrderItemRowController.class.getName());

    public OrderItemRowController() {
        // Enhanced constructor
    }

    public void initialize() {
        logger.info("OrderItemRowController.initialize: Initializing enhanced order item row controller");
        
        // Initialize rush delivery label if available
        if (rushDeliveryEligibleLabel != null) {
            rushDeliveryEligibleLabel.setVisible(false);
            rushDeliveryEligibleLabel.setManaged(false);
        }
    }

    /**
     * Enhanced setData method for OrderItem entity with comprehensive error handling
     */
    public void setData(OrderItem item) {
        logger.info("OrderItemRowController.setData: Setting data from OrderItem entity");
        
        this.orderItem = item;
        this.orderItemDTO = null; // Clear DTO reference
        
        if (item == null) {
            logger.warning("OrderItemRowController.setData: Received null OrderItem");
            hideRow();
            return;
        }
        
        try {
            showRow();
            populateFromEntity(item);
            logger.info("OrderItemRowController.setData: Successfully populated from OrderItem entity");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "OrderItemRowController.setData: Error setting data from entity", e);
            populateFallbackDisplay("Error loading item data");
        }
    }
    
    /**
     * Enhanced setData method for OrderItemDTO with comprehensive error handling
     */
    public void setData(OrderItemDTO itemDTO) {
        logger.info("OrderItemRowController.setData: Setting data from OrderItemDTO");
        
        this.orderItemDTO = itemDTO;
        this.orderItem = null; // Clear entity reference
        
        if (itemDTO == null) {
            logger.warning("OrderItemRowController.setData: Received null OrderItemDTO");
            hideRow();
            return;
        }
        
        try {
            showRow();
            populateFromDTO(itemDTO);
            logger.info("OrderItemRowController.setData: Successfully populated from OrderItemDTO");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "OrderItemRowController.setData: Error setting data from DTO", e);
            populateFallbackDisplay("Error loading item data");
        }
    }
    
    /**
     * Populate UI from OrderItem entity
     */
    private void populateFromEntity(OrderItem item) {
        Product product = item.getProduct();

        if (product != null) {
            OrderSummaryUIHelper.setTextSafely(productTitleLabel, product.getTitle());
            OrderSummaryUIHelper.setTextSafely(productIdLabel, "ID: " + product.getProductId());
            loadProductImage(product.getImageUrl(), product.getTitle());
        } else {
            logger.warning("OrderItemRowController.populateFromEntity: Product data missing");
            OrderSummaryUIHelper.setTextSafely(productTitleLabel, "Product Data Missing");
            OrderSummaryUIHelper.setTextSafely(productIdLabel, "ID: N/A");
            loadPlaceholderImage();
        }

        // Populate quantity and pricing
        OrderSummaryUIHelper.setTextSafely(quantityLabel, "Qty: " + item.getQuantity());
        OrderSummaryUIHelper.setTextSafely(unitPriceLabel,
            OrderSummaryUIHelper.formatCurrency(item.getPriceAtTimeOfOrder()));
        OrderSummaryUIHelper.setTextSafely(totalItemPriceLabel,
            OrderSummaryUIHelper.formatCurrency(item.getPriceAtTimeOfOrder() * item.getQuantity()));
            
        // Rush delivery eligibility (if available in future enhancements)
        updateRushDeliveryDisplay(false); // Default to false for entities
    }
    
    /**
     * Populate UI from OrderItemDTO
     */
    private void populateFromDTO(OrderItemDTO itemDTO) {
        OrderSummaryUIHelper.setTextSafely(productTitleLabel, itemDTO.getTitle());
        OrderSummaryUIHelper.setTextSafely(productIdLabel, "ID: " + itemDTO.getProductId());
        loadProductImage(itemDTO.getImageUrl(), itemDTO.getTitle());

        // Populate quantity and pricing from DTO
        OrderSummaryUIHelper.setTextSafely(quantityLabel, "Qty: " + itemDTO.getQuantity());
        OrderSummaryUIHelper.setTextSafely(unitPriceLabel,
            "Unit: " + OrderSummaryUIHelper.formatCurrency(itemDTO.getPriceAtTimeOfOrder()));
        OrderSummaryUIHelper.setTextSafely(totalItemPriceLabel,
            "Total: " + OrderSummaryUIHelper.formatCurrency(itemDTO.getTotalPriceAtTimeOfOrder()));
            
        // Rush delivery eligibility from DTO
        updateRushDeliveryDisplay(itemDTO.isEligibleForRushDelivery());
    }
    
    /**
     * Load product image with enhanced error handling
     */
    private void loadProductImage(String imageUrl, String productTitle) {
        if (productImageView == null) {
            logger.warning("OrderItemRowController.loadProductImage: ProductImageView is null");
            return;
        }
        
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            try {
                Image image = new Image(imageUrl, true); // Background loading
                productImageView.setImage(image);
                
                // Handle image loading errors
                if (image.isError()) {
                    logger.warning("OrderItemRowController.loadProductImage: Image loading failed for " +
                                 (productTitle != null ? productTitle : "unknown product") + ": " + imageUrl);
                    loadPlaceholderImage();
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "OrderItemRowController.loadProductImage: Exception loading image", e);
                loadPlaceholderImage();
            }
        } else {
            loadPlaceholderImage();
        }
    }
    
    /**
     * Update rush delivery eligibility display
     */
    private void updateRushDeliveryDisplay(boolean isEligible) {
        if (rushDeliveryEligibleLabel != null) {
            OrderSummaryUIHelper.setVisibilityAndManaged(rushDeliveryEligibleLabel, isEligible);
            if (isEligible) {
                rushDeliveryEligibleLabel.setText("Rush Eligible");
                rushDeliveryEligibleLabel.setStyle("-fx-text-fill: green; -fx-font-size: 10px;");
            }
        }
    }
    
    /**
     * Show the row
     */
    private void showRow() {
        OrderSummaryUIHelper.setVisibilityAndManaged(orderItemRowPane, true);
    }
    
    /**
     * Hide the row
     */
    private void hideRow() {
        OrderSummaryUIHelper.setVisibilityAndManaged(orderItemRowPane, false);
    }
    
    /**
     * Populate fallback display when data loading fails
     */
    private void populateFallbackDisplay(String errorMessage) {
        OrderSummaryUIHelper.setTextSafely(productTitleLabel, errorMessage);
        OrderSummaryUIHelper.setTextSafely(productIdLabel, "ID: N/A");
        OrderSummaryUIHelper.setTextSafely(quantityLabel, "Qty: N/A");
        OrderSummaryUIHelper.setTextSafely(unitPriceLabel, "Unit: N/A");
        OrderSummaryUIHelper.setTextSafely(totalItemPriceLabel, "Total: N/A");
        loadPlaceholderImage();
        updateRushDeliveryDisplay(false);
    }
/**
 * Enhanced placeholder image loading with comprehensive error handling
 */
private void loadPlaceholderImage() {
    if (productImageView == null) {
        logger.warning("OrderItemRowController.loadPlaceholderImage: ProductImageView is null");
        return;
    }
    
    try {
        // Try multiple placeholder image locations
        String[] placeholderPaths = {
            "/images/product_placeholder.png",
            "/assets/images/product_placeholder.png",
            "/com/aims/presentation/images/product_placeholder.png"
        };
        
        boolean placeholderLoaded = false;
        
        for (String path : placeholderPaths) {
            try {
                java.io.InputStream placeholderStream = getClass().getResourceAsStream(path);
                
                if (placeholderStream != null) {
                    Image placeholder = new Image(placeholderStream);
                    if (!placeholder.isError()) {
                        productImageView.setImage(placeholder);
                        placeholderLoaded = true;
                        logger.info("OrderItemRowController.loadPlaceholderImage: Successfully loaded placeholder from: " + path);
                        break;
                    } else {
                        logger.warning("OrderItemRowController.loadPlaceholderImage: Image error for path: " + path);
                    }
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "OrderItemRowController.loadPlaceholderImage: Error loading from path: " + path, e);
            }
        }
        
        if (!placeholderLoaded) {
            logger.warning("OrderItemRowController.loadPlaceholderImage: Could not load any placeholder image, using empty image view");
            // Optionally set a default style or clear the image view
            productImageView.setImage(null);
        }
        
    } catch (Exception e) {
        logger.log(Level.SEVERE, "OrderItemRowController.loadPlaceholderImage: Unexpected error in placeholder loading", e);
        productImageView.setImage(null);
    }
}
    /**
     * Get current order item (entity)
     */
    public OrderItem getOrderItem() {
        return orderItem;
    }
    
    /**
     * Get current order item DTO
     */
    public OrderItemDTO getOrderItemDTO() {
        return orderItemDTO;
    }
    
    /**
     * Check if the controller is displaying entity data
     */
    public boolean isDisplayingEntity() {
        return orderItem != null;
    }
    
    /**
     * Check if the controller is displaying DTO data
     */
    public boolean isDisplayingDTO() {
        return orderItemDTO != null;
    }
    
    /**
     * Refresh display with current data
     */
    public void refreshDisplay() {
        if (orderItem != null) {
            setData(orderItem);
        } else if (orderItemDTO != null) {
            setData(orderItemDTO);
        } else {
            logger.warning("OrderItemRowController.refreshDisplay: No data available to refresh");
            hideRow();
        }
    }
    
    /**
     * Clear all data and hide the row
     */
    public void clearData() {
        this.orderItem = null;
        this.orderItemDTO = null;
        hideRow();
        logger.info("OrderItemRowController.clearData: Data cleared and row hidden");
    }
    
    /**
     * Validate that UI components are properly initialized
     */
    private boolean validateUIComponents() {
        boolean isValid = true;
        StringBuilder missing = new StringBuilder();
        
        if (orderItemRowPane == null) {
            missing.append("orderItemRowPane ");
            isValid = false;
        }
        if (productImageView == null) {
            missing.append("productImageView ");
            isValid = false;
        }
        if (productTitleLabel == null) {
            missing.append("productTitleLabel ");
            isValid = false;
        }
        if (quantityLabel == null) {
            missing.append("quantityLabel ");
            isValid = false;
        }
        if (unitPriceLabel == null) {
            missing.append("unitPriceLabel ");
            isValid = false;
        }
        if (totalItemPriceLabel == null) {
            missing.append("totalItemPriceLabel ");
            isValid = false;
        }
        
        if (!isValid) {
            logger.severe("OrderItemRowController.validateUIComponents: Missing UI components: " + missing.toString());
        }
        
        return isValid;
    }
    
    /**
     * Get display summary for debugging
     */
    public String getDisplaySummary() {
        if (orderItem != null) {
            return String.format("OrderItemRowController[Entity]: %s (x%d) - %,.0f VND", 
                orderItem.getProduct() != null ? orderItem.getProduct().getTitle() : "Unknown",
                orderItem.getQuantity(),
                orderItem.getPriceAtTimeOfOrder() * orderItem.getQuantity());
        } else if (orderItemDTO != null) {
            return String.format("OrderItemRowController[DTO]: %s (x%d) - %,.0f VND", 
                orderItemDTO.getTitle() != null ? orderItemDTO.getTitle() : "Unknown",
                orderItemDTO.getQuantity(),
                orderItemDTO.getTotalPriceAtTimeOfOrder());
        } else {
            return "OrderItemRowController[Empty]";
        }
    }
}