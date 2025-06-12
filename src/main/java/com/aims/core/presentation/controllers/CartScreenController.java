package com.aims.core.presentation.controllers;

import com.aims.core.application.dtos.CartItemDTO;
// import com.aims.core.application.dtos.CartViewDTO; // Could be used by service to return all cart data
import com.aims.core.application.services.ICartService;
import com.aims.core.application.services.IOrderService;
import com.aims.core.entities.Product; // For creating DTOs if service returns entities
import com.aims.core.entities.Cart; // If service returns Cart entity
import com.aims.core.entities.CartItem; // If service returns CartItem entity
import com.aims.core.entities.OrderEntity; // For order creation
import com.aims.core.presentation.utils.AlertHelper; // Added import
// import com.aims.presentation.utils.FXMLSceneManager;
import com.aims.core.shared.exceptions.InventoryException;
import com.aims.core.shared.exceptions.ResourceNotFoundException;
import com.aims.core.shared.exceptions.ValidationException;
import com.aims.core.presentation.utils.CartSessionManager;
import com.aims.core.shared.ServiceFactory;


import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CartScreenController implements MainLayoutController.IChildController {

    @FXML
    private ScrollPane cartScrollPane;
    @FXML
    private VBox cartItemsContainerVBox; // Container for individual cart item cards
    @FXML
    private Label totalCartPriceLabel;
    @FXML
    private Button checkoutButton;
    @FXML
    private Label stockWarningLabel; // General stock warnings for the cart

    // @Inject
    private ICartService cartService;
    private IOrderService orderService;
    private MainLayoutController mainLayoutController;
    // private FXMLSceneManager sceneManager;

    // This list can hold the DTOs that are bound or represented by the loaded FXML cards
    private ObservableList<CartItemDTO> currentCartItemDTOs = FXCollections.observableArrayList();
    private String cartSessionId; // Will be set using CartSessionManager
    
    // CRITICAL FIX: Add thread-safe updating flag to prevent infinite loops
    private volatile boolean isUpdatingCart = false;

    public CartScreenController() {
        // cartService = new CartServiceImpl(...); // DI example
    }

    @Override
    public void setMainLayoutController(MainLayoutController mainLayoutController) {
        this.mainLayoutController = mainLayoutController;
        // this.sceneManager = mainLayoutController.getSceneManager();
    }

    public void setCartService(ICartService cartService) {
        this.cartService = cartService;
    }
    
    public void setOrderService(IOrderService orderService) {
        this.orderService = orderService;
    }

    // public void setCartSessionId(String cartSessionId) {
    //     this.cartSessionId = cartSessionId;
    //     loadCartDetails(); // Load details when session ID is set
    // }

    public void initialize() {
        setStockWarning("", false);
        
        // CRITICAL FIX: Use CartSessionManager for consistent session ID
        this.cartSessionId = CartSessionManager.getOrCreateCartSessionId();
        System.out.println("CartScreenController.initialize: Using cart session ID: " + cartSessionId);
        
        // Validate services before loading
        validateAndInitializeServices();
        
        if (cartSessionId != null && !cartSessionId.trim().isEmpty()) {
            loadCartDetails();
        } else {
            System.err.println("CartScreenController: cartSessionId is null or empty. Cart will be empty.");
            displayEmptyCart();
        }
    }

    private void loadCartDetails() {
        // CRITICAL FIX: Prevent concurrent cart operations
        if (isUpdatingCart) {
            System.out.println("loadCartDetails: Cart update already in progress, skipping");
            return;
        }
        
        System.out.println("CartScreenController.loadCartDetails: Loading cart for session: " + cartSessionId);
        
        validateAndInitializeServices();
        
        if (cartService == null) {
            System.err.println("CartService is unavailable");
            displayEmptyCart();
            setStockWarning("Cart service is temporarily unavailable. Please refresh the page.", true);
            return;
        }
        
        if (cartSessionId == null || cartSessionId.trim().isEmpty()) {
            System.err.println("Cart session ID is invalid");
            displayEmptyCart();
            setStockWarning("Invalid cart session. Please refresh the page.", true);
            return;
        }
        
        try {
            isUpdatingCart = true;
            
            // CRITICAL FIX: Additional validation before proceeding
            if (cartSessionId == null || cartSessionId.trim().isEmpty()) {
                System.err.println("CRITICAL: Invalid cart session ID in loadCartDetails");
                displayEmptyCart();
                return;
            }
            
            // CRITICAL FIX: Single cart load attempt to prevent infinite loops
            // Removed retry mechanism that amplifies DatabaseSchemaValidator calls
            Cart cartEntity = cartService.getCart(cartSessionId);
            System.out.println("CartScreenController.loadCartDetails: Cart load attempt completed");
            
            cartItemsContainerVBox.getChildren().clear();
            currentCartItemDTOs.clear();
            setStockWarning("", false);
            
            if (cartEntity != null && cartEntity.getItems() != null && !cartEntity.getItems().isEmpty()) {
                System.out.println("CartScreenController.loadCartDetails: Found " + cartEntity.getItems().size() + " items in cart");
                
                float grandTotalExclVAT = 0f;
                boolean hasStockIssues = false;
                
                for (CartItem itemEntity : cartEntity.getItems()) {
                    Product product = itemEntity.getProduct();
                    if (product == null) {
                        System.err.println("Product is null for cart item - skipping");
                        continue;
                    }
                    
                    // Create CartItemDTO for UI binding
                    CartItemDTO dto = new CartItemDTO(
                        product.getProductId(),
                        product.getTitle(),
                        itemEntity.getQuantity(),
                        product.getPrice(), // Price ex-VAT from Product entity
                        product.getImageUrl(),
                        product.getQuantityInStock()
                    );
                    currentCartItemDTOs.add(dto);
                    grandTotalExclVAT += dto.getTotalPriceExclVAT();
                    
                    if (!dto.isStockSufficient()) {
                        hasStockIssues = true;
                    }
                    
                    // Load the FXML for each cart item row
                    try {
                        FXMLLoader loader = new FXMLLoader(
                            getClass().getResource("/com/aims/presentation/views/partials/cart_item_row.fxml")
                        );
                        Node itemNode = loader.load();
                        CartItemRowController rowController = loader.getController();
                        rowController.setData(dto, this.cartService, this.cartSessionId, this);
                        cartItemsContainerVBox.getChildren().add(itemNode);
                        
                        System.out.println("CartScreenController.loadCartDetails: Added cart item row for: " + product.getTitle());
                    } catch (IOException e) {
                        e.printStackTrace();
                        System.err.println("Error loading cart item row FXML: " + e.getMessage());
                    }
                }
                
                totalCartPriceLabel.setText(String.format("Total (excl. VAT): %,.0f VND", grandTotalExclVAT));
                checkoutButton.setDisable(hasStockIssues);
                
                if (hasStockIssues) {
                    setStockWarning("One or more items have insufficient stock. Please update quantities.", true);
                }
                
                System.out.println("CartScreenController.loadCartDetails: Successfully loaded cart with " +
                                 cartEntity.getItems().size() + " items, total: " + grandTotalExclVAT + " VND");
                
            } else {
                System.out.println("CartScreenController.loadCartDetails: Cart is empty or null");
                displayEmptyCart();
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Database error loading cart: " + e.getMessage());
            displayEmptyCart();
            setStockWarning("Database error loading cart: " + e.getMessage(), true);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Unexpected error loading cart: " + e.getMessage());
            displayEmptyCart();
            setStockWarning("Error loading cart: " + e.getMessage(), true);
        } finally {
            isUpdatingCart = false;
        }
    }

    private void displayEmptyCart() {
        cartItemsContainerVBox.getChildren().clear();
        currentCartItemDTOs.clear();
        Label emptyCartLabel = new Label("Your shopping cart is currently empty.");
        emptyCartLabel.getStyleClass().add("text-normal");
        cartItemsContainerVBox.getChildren().add(emptyCartLabel);
        totalCartPriceLabel.setText("Total (excl. VAT): 0 VND");
        checkoutButton.setDisable(true);
        setStockWarning("", false);
    }

    private void validateAndInitializeServices() {
        if (cartService == null) {
            System.err.println("CartService is null - attempting recovery");
            try {
                ServiceFactory serviceFactory = ServiceFactory.getInstance();
                this.cartService = serviceFactory.getCartService();
                System.out.println("CartService initialized from ServiceFactory: " + (cartService != null));
            } catch (Exception e) {
                System.err.println("Failed to initialize CartService: " + e.getMessage());
            }
        }
        
        if (orderService == null) {
            System.err.println("OrderService is null - attempting recovery");
            try {
                ServiceFactory serviceFactory = ServiceFactory.getInstance();
                this.orderService = serviceFactory.getOrderService();
                System.out.println("OrderService initialized from ServiceFactory: " + (orderService != null));
            } catch (Exception e) {
                System.err.println("Failed to initialize OrderService: " + e.getMessage());
            }
        }
    }

    private void setStockWarning(String message, boolean visible) {
        stockWarningLabel.setText(message);
        stockWarningLabel.setVisible(visible);
        stockWarningLabel.setManaged(visible);
    }

    /**
     * Helper method to get current user ID for order creation.
     * Returns null for guest checkout.
     */
    private String getCurrentUserId() {
        if (mainLayoutController != null && mainLayoutController.getCurrentUser() != null) {
            return mainLayoutController.getCurrentUser().getUserId();
        }
        return null; // Guest checkout
    }

    @FXML
    void handleClearCartAction(ActionEvent event) {
        // CRITICAL FIX: Add confirmation dialog and source validation
        if (isUpdatingCart) {
            System.out.println("Cart update in progress, skipping clear request");
            return;
        }
        
        if (cartService == null) {
            System.err.println("CartService unavailable");
            return;
        }
        if (cartSessionId == null || cartSessionId.isEmpty()) {
            System.err.println("Invalid cart session for clear action");
            return;
        }

        // CRITICAL FIX: Require explicit user confirmation
        boolean confirmed = AlertHelper.showConfirmationDialog(
            "Clear Cart",
            "Are you sure you want to empty your entire cart? This cannot be undone."
        );
        if (!confirmed) {
            System.out.println("Cart clear cancelled by user");
            return;
        }
        
        // CRITICAL FIX: Log source of clear request for debugging
        System.out.println("Cart clear CONFIRMED by user for session: " + cartSessionId);
        
        try {
            isUpdatingCart = true;
            cartService.clearCart(cartSessionId);
            System.out.println("Cart cleared successfully after user confirmation");
            loadCartDetails(); // Refresh view
        } catch (SQLException | ResourceNotFoundException e) {
            System.err.println("Error clearing cart: " + e.getMessage());
            setStockWarning("Error clearing cart: " + e.getMessage(), true);
        } finally {
            isUpdatingCart = false;
        }
    }

    @FXML
    void handleProceedToCheckoutAction(ActionEvent event) {
        // CRITICAL FIX: Early empty cart detection to prevent infinite loops
        System.out.println("CHECKOUT: Starting checkout process for session: " + cartSessionId);
        
        // CRITICAL FIX: Early validation to prevent database retry loops for empty carts
        if (currentCartItemDTOs == null || currentCartItemDTOs.isEmpty()) {
            System.out.println("CHECKOUT: Early detection - cart DTOs are empty, skipping database validation");
            setStockWarning("Your cart is empty. Please add items before proceeding to checkout.", true);
            checkoutButton.setDisable(true);
            return;
        }
        
        // CRITICAL FIX: Validate services before any database operations
        validateAndInitializeServices();
        if (cartService == null) {
            System.err.println("CHECKOUT ERROR: CartService unavailable");
            setStockWarning("Cart service is temporarily unavailable. Please try again.", true);
            return;
        }
        
        // CRITICAL FIX: Single cart validation call (no retry loop for empty carts)
        try {
            Cart cartValidation = cartService.getCart(cartSessionId);
            if (cartValidation == null || cartValidation.getItems() == null || cartValidation.getItems().isEmpty()) {
                System.out.println("CHECKOUT: Database confirmed cart is empty");
                setStockWarning("Your cart appears to be empty. Please add items before checkout.", true);
                displayEmptyCart(); // Update UI to reflect empty state
                return;
            }
            System.out.println("CHECKOUT: Cart validation passed - " + cartValidation.getItems().size() + " items");
        } catch (Exception e) {
            System.err.println("CHECKOUT ERROR: Failed to validate cart: " + e.getMessage());
            setStockWarning("Unable to validate cart. Please refresh the page.", true);
            return;
        }
        
        boolean hasStockIssues = currentCartItemDTOs.stream().anyMatch(item -> !item.isStockSufficient());
        if (hasStockIssues) {
            // The stock insufficient dialog will be shown by updateCartTotalsAndState if called,
            // or if not, this warning label is a fallback.
            // For checkout, we strictly prevent proceeding if issues exist.
            setStockWarning("Please resolve stock issues in your cart before proceeding.", true);
            // Explicitly call updateCartTotalsAndState to ensure dialog is shown if not already.
            updateCartTotalsAndState();
            return;
        }

        System.out.println("Proceed to Checkout action triggered for cart: " + cartSessionId);
        
        // Validate services before order creation
        validateAndInitializeServices();
        
        if (orderService == null) {
            System.err.println("OrderService is not available for checkout");
            setStockWarning("Order service is temporarily unavailable. Please try again.", true);
            return;
        }
        
        try {
            // Create order from cart items
            String userId = getCurrentUserId();
            System.out.println("Creating order from cart: " + cartSessionId + " for user: " + (userId != null ? userId : "guest"));
            
            OrderEntity createdOrder = orderService.initiateOrderFromCart(cartSessionId, userId);
            
            if (createdOrder == null) {
                System.err.println("Order creation returned null");
                setStockWarning("Failed to create order. Please try again.", true);
                return;
            }
            
            System.out.println("Order created successfully with ID: " + createdOrder.getOrderId());
            
            // Navigate to delivery info screen with the created order
            if (mainLayoutController != null) {
                Object controller = mainLayoutController.loadContent("/com/aims/presentation/views/delivery_info_screen.fxml");
                mainLayoutController.setHeaderTitle("Delivery Information");
                
                // Pass the created order to delivery info controller
                if (controller instanceof DeliveryInfoScreenController) {
                    DeliveryInfoScreenController deliveryController = (DeliveryInfoScreenController) controller;
                    deliveryController.setOrderData(createdOrder);
                    
                    // Inject services into delivery controller
                    try {
                        ServiceFactory serviceFactory = ServiceFactory.getInstance();
                        deliveryController.setOrderService(serviceFactory.getOrderService());
                        deliveryController.setDeliveryService(serviceFactory.getDeliveryCalculationService());
                        System.out.println("Services injected into DeliveryInfoScreenController");
                    } catch (Exception serviceEx) {
                        System.err.println("Error injecting services into DeliveryInfoScreenController: " + serviceEx.getMessage());
                    }
                    
                    System.out.println("Order data passed to delivery info screen");
                } else {
                    System.err.println("Failed to cast controller to DeliveryInfoScreenController");
                }
                
                System.out.println("Successfully navigated to delivery info screen with order data");
            } else {
                System.err.println("MainLayoutController not available for navigation");
                setStockWarning("Navigation error. Please refresh the page.", true);
            }
            
        } catch (SQLException e) {
            System.err.println("Database error during order creation: " + e.getMessage());
            e.printStackTrace();
            setStockWarning("Database error during checkout. Please try again.", true);
        } catch (ResourceNotFoundException e) {
            System.err.println("Cart not found during order creation: " + e.getMessage());
            e.printStackTrace();
            setStockWarning("Cart data not found. Please refresh and try again.", true);
        } catch (InventoryException e) {
            System.err.println("Inventory issues during order creation: " + e.getMessage());
            e.printStackTrace();
            setStockWarning("Some items are no longer available. Please update your cart.", true);
            // Refresh cart to show current stock status
            loadCartDetails();
        } catch (ValidationException e) {
            System.err.println("Validation error during order creation: " + e.getMessage());
            e.printStackTrace();
            setStockWarning("Invalid cart data. Please refresh and try again.", true);
        } catch (Exception e) {
            System.err.println("Unexpected error during checkout: " + e.getMessage());
            e.printStackTrace();
            setStockWarning("Unexpected error during checkout. Please try again.", true);
        }
    }

    /**
     * Called by CartItemRowController when an item's quantity changes.
     * CRITICAL FIX: Prevent infinite loop with thread-safe updating flag.
     */
    public void handleUpdateQuantityFromRow(CartItemDTO itemDto, int newQuantity) {
        // CRITICAL FIX: Prevent concurrent or recursive updates
        if (isUpdatingCart) {
            System.out.println("Cart update already in progress, skipping duplicate request");
            return;
        }
        
        if (cartService == null) {
            System.err.println("CartService unavailable");
            return;
        }
        
        try {
            isUpdatingCart = true;
            System.out.println("Updating quantity for " + itemDto.getProductId() + " to " + newQuantity + " in cart " + cartSessionId);
            
            // Update the database
            cartService.updateItemQuantity(cartSessionId, itemDto.getProductId(), newQuantity);
            
            // CRITICAL FIX: Update only the specific item's data locally instead of full reload
            updateSingleItemQuantity(itemDto, newQuantity);
            
            // CRITICAL FIX: Recalculate totals without triggering UI refresh loops
            recalculateCartTotals();
            
        } catch (SQLException | ResourceNotFoundException | ValidationException | InventoryException e) {
            System.err.println("Update quantity failed: " + e.getMessage());
            setStockWarning("Update failed: " + e.getMessage(), true);
            // Only reload on error to revert changes
            loadCartDetails();
        } finally {
            isUpdatingCart = false;
        }
    }

    /**
     * CRITICAL FIX: Update single item quantity in local DTO without full database reload.
     */
    private void updateSingleItemQuantity(CartItemDTO targetItem, int newQuantity) {
        for (CartItemDTO item : currentCartItemDTOs) {
            if (item.getProductId().equals(targetItem.getProductId())) {
                // Update the quantity in the DTO
                item.setQuantity(newQuantity);
                System.out.println("Updated local DTO quantity for " + item.getTitle() + " to " + newQuantity);
                break;
            }
        }
    }
    
    /**
     * CRITICAL FIX: Recalculate cart totals from current DTOs without database reload.
     */
    private void recalculateCartTotals() {
        float grandTotalExclVAT = 0f;
        boolean hasStockIssues = false;
        
        for (CartItemDTO item : currentCartItemDTOs) {
            grandTotalExclVAT += item.getTotalPriceExclVAT();
            if (!item.isStockSufficient()) {
                hasStockIssues = true;
            }
        }
        
        // Update UI totals
        totalCartPriceLabel.setText(String.format("Total (excl. VAT): %,.0f VND", grandTotalExclVAT));
        checkoutButton.setDisable(hasStockIssues);
        
        if (hasStockIssues) {
            setStockWarning("One or more items have insufficient stock. Please update quantities.", true);
        } else {
            setStockWarning("", false);
        }
        
        System.out.println("Recalculated cart totals: " + grandTotalExclVAT + " VND, hasStockIssues: " + hasStockIssues);
    }
    
    /**
     * CRITICAL FIX: Update cart totals and state without full reload to prevent infinite loop.
     * This method recalculates totals from current DTOs and updates UI accordingly.
     */
    private void updateCartTotalsAndState() {
        // CRITICAL FIX: Prevent recursive calls
        if (isUpdatingCart) {
            System.out.println("updateCartTotalsAndState: Already updating, skipping to prevent recursion");
            return;
        }
        
        if (cartService == null) {
            System.err.println("CartService is null in updateCartTotalsAndState. Cannot update.");
            setStockWarning("Cart service unavailable. Cannot update cart status.", true);
            checkoutButton.setDisable(true);
            return;
        }
        if (cartSessionId == null || cartSessionId.isEmpty()) {
            System.err.println("CartSessionId is null in updateCartTotalsAndState. Cannot update.");
            setStockWarning("Cart session invalid. Cannot update cart status.", true);
            checkoutButton.setDisable(true);
            return;
        }

        try {
            isUpdatingCart = true;
            
            // CRITICAL FIX: Only reload from database if absolutely necessary
            // First try to work with current DTOs, only reload if data is stale
            if (currentCartItemDTOs.isEmpty()) {
                // Reload cart data from database only if we have no current data
                Cart cartEntity = cartService.getCart(cartSessionId);
                if (cartEntity == null || cartEntity.getItems() == null || cartEntity.getItems().isEmpty()) {
                    displayEmptyCart();
                    return;
                }

                // Update the currentCartItemDTOs with fresh data from database
                currentCartItemDTOs.clear();
                for (CartItem itemEntity : cartEntity.getItems()) {
                    Product product = itemEntity.getProduct();
                    if (product == null) {
                        continue;
                    }

                    CartItemDTO dto = new CartItemDTO(
                        product.getProductId(),
                        product.getTitle(),
                        itemEntity.getQuantity(),
                        product.getPrice(),
                        product.getImageUrl(),
                        product.getQuantityInStock()
                    );
                    currentCartItemDTOs.add(dto);
                }
            }
            
            // Calculate totals from current DTOs
            float grandTotalExclVAT = 0f;
            boolean hasStockIssues = false;

            for (CartItemDTO dto : currentCartItemDTOs) {
                grandTotalExclVAT += dto.getTotalPriceExclVAT();
                if (!dto.isStockSufficient()) {
                    hasStockIssues = true;
                }
            }

            // Update UI totals and state
            totalCartPriceLabel.setText(String.format("Total (excl. VAT): %,.0f VND", grandTotalExclVAT));
            checkoutButton.setDisable(hasStockIssues);

            if (hasStockIssues) {
                // Collect messages for the dialog
                List<String> insufficientItemMessages = currentCartItemDTOs.stream()
                    .filter(item -> !item.isStockSufficient())
                    .map(item -> String.format("%s (Requested: %d, Available: %d)",
                                               item.getTitle(), item.getQuantity(), item.getAvailableStock()))
                    .collect(Collectors.toList());

                // Show the dialog only if not already updating to prevent dialog spam
                AlertHelper.showStockInsufficientDialog(insufficientItemMessages,
                    () -> { // onUpdateCart action
                        System.out.println("User chose to update cart from stock insufficient dialog.");
                    },
                    () -> { // onCancelProcess action
                        System.out.println("User chose to cancel/modify order process from stock insufficient dialog.");
                        setStockWarning("Checkout disabled due to stock issues. Please update your cart or clear problematic items.", true);
                        checkoutButton.setDisable(true);
                    });
                
                setStockWarning("One or more items have insufficient stock. Please update quantities.", true);
            } else {
                setStockWarning("", false);
            }

            System.out.println("CartScreenController.updateCartTotalsAndState: Updated totals without full reload");

        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Error updating cart totals: " + e.getMessage());
            setStockWarning("Error updating cart: " + e.getMessage(), true);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Unexpected error updating cart totals: " + e.getMessage());
            setStockWarning("Error updating cart: " + e.getMessage(), true);
        } finally {
            isUpdatingCart = false;
        }
    }

    /**
     * Called by CartItemRowController when an item is to be removed.
     * CRITICAL FIX: Prevent concurrent operations during removal.
     */
    public void handleRemoveItemFromRow(CartItemDTO itemDto) {
        // CRITICAL FIX: Prevent concurrent operations
        if (isUpdatingCart) {
            System.out.println("Cart update in progress, skipping remove request");
            return;
        }
        
        if (cartService == null) {
            System.err.println("CartService unavailable");
            return;
        }
        
        try {
            isUpdatingCart = true;
            System.out.println("Removing item " + itemDto.getProductId() + " from cart " + cartSessionId);
            cartService.removeItemFromCart(cartSessionId, itemDto.getProductId());
            
            // CRITICAL FIX: Remove item from local DTOs to prevent UI inconsistency
            currentCartItemDTOs.removeIf(item -> item.getProductId().equals(itemDto.getProductId()));
            
            // Full reload is acceptable for removal as it's less frequent than quantity updates
            loadCartDetails();
        } catch (SQLException | ResourceNotFoundException e) {
            System.err.println("Remove item failed: " + e.getMessage());
            setStockWarning("Remove failed: " + e.getMessage(), true);
            loadCartDetails(); // Revert UI by reloading
        } finally {
            isUpdatingCart = false;
        }
    }
}