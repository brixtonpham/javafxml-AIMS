package com.aims.core.presentation.controllers;

import com.aims.core.application.dtos.CartItemDTO;
// import com.aims.core.application.dtos.CartViewDTO; // Could be used by service to return all cart data
import com.aims.core.application.services.ICartService;
import com.aims.core.entities.Product; // For creating DTOs if service returns entities
import com.aims.core.entities.Cart; // If service returns Cart entity
import com.aims.core.entities.CartItem; // If service returns CartItem entity
// import com.aims.presentation.utils.AlertHelper;
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
    private MainLayoutController mainLayoutController;
    // private FXMLSceneManager sceneManager;

    // This list can hold the DTOs that are bound or represented by the loaded FXML cards
    private ObservableList<CartItemDTO> currentCartItemDTOs = FXCollections.observableArrayList();
    private String cartSessionId; // Will be set using CartSessionManager

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
            Cart cartEntity = cartService.getCart(cartSessionId);
            
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
    }

    private void setStockWarning(String message, boolean visible) {
        stockWarningLabel.setText(message);
        stockWarningLabel.setVisible(visible);
        stockWarningLabel.setManaged(visible);
    }

    @FXML
    void handleClearCartAction(ActionEvent event) {
        if (cartService == null) {
            System.err.println("CartService unavailable");
            return;
        }
        if (cartSessionId == null || cartSessionId.isEmpty()) return;

        // TODO: Add AlertHelper.showConfirmationDialog when available
        // For now, proceed with clearing
        try {
            cartService.clearCart(cartSessionId);
            System.out.println("Cart cleared successfully");
            loadCartDetails(); // Refresh view
        } catch (SQLException | ResourceNotFoundException e) {
            System.err.println("Error clearing cart: " + e.getMessage());
            setStockWarning("Error clearing cart: " + e.getMessage(), true);
        }
    }

    @FXML
    void handleProceedToCheckoutAction(ActionEvent event) {
        if (currentCartItemDTOs.isEmpty()) {
            setStockWarning("Please add items to your cart before proceeding to checkout.", true);
            return;
        }
        
        boolean hasStockIssues = currentCartItemDTOs.stream().anyMatch(item -> !item.isStockSufficient());
        if (hasStockIssues) {
            setStockWarning("Please resolve stock issues in your cart before proceeding.", true);
            return;
        }

        System.out.println("Proceed to Checkout action triggered for cart: " + cartSessionId);
        
        if (mainLayoutController != null) {
            try {
                // Navigate to delivery info screen
                Object controller = mainLayoutController.loadContent("/com/aims/presentation/views/delivery_info_screen.fxml");
                mainLayoutController.setHeaderTitle("Delivery Information");
                
                // TODO: Pass cart session ID to delivery info controller when the setCartSessionForOrder method is available
                // if (controller instanceof DeliveryInfoScreenController) {
                //     ((DeliveryInfoScreenController) controller).setCartSessionForOrder(this.cartSessionId);
                // }
                
                System.out.println("Successfully navigated to delivery info screen");
            } catch (Exception e) {
                System.err.println("Error navigating to delivery info screen: " + e.getMessage());
                setStockWarning("Error proceeding to checkout. Please try again.", true);
            }
        } else {
            System.err.println("MainLayoutController not available for navigation");
            setStockWarning("Navigation error. Please refresh the page.", true);
        }
    }

    /**
     * Called by CartItemRowController when an item's quantity changes.
     * CRITICAL FIX: Avoid full cart reload to prevent infinite loop.
     */
    public void handleUpdateQuantityFromRow(CartItemDTO itemDto, int newQuantity) {
        if (cartService == null) {
            System.err.println("CartService unavailable");
            return;
        }
        try {
            System.out.println("Updating quantity for " + itemDto.getProductId() + " to " + newQuantity + " in cart " + cartSessionId);
            cartService.updateItemQuantity(cartSessionId, itemDto.getProductId(), newQuantity);
            
            // CRITICAL FIX: Instead of full reload, update only the specific UI components
            updateCartTotalsAndState();
        } catch (SQLException | ResourceNotFoundException | ValidationException | InventoryException e) {
            System.err.println("Update quantity failed: " + e.getMessage());
            setStockWarning("Update failed: " + e.getMessage(), true);
            // Only reload on error to revert changes
            loadCartDetails();
        }
    }

    /**
     * CRITICAL FIX: Update cart totals and state without full reload to prevent infinite loop.
     * This method recalculates totals from current DTOs and updates UI accordingly.
     */
    private void updateCartTotalsAndState() {
        if (currentCartItemDTOs.isEmpty()) {
            displayEmptyCart();
            return;
        }

        try {
            // Reload cart data from database to get updated quantities
            Cart cartEntity = cartService.getCart(cartSessionId);
            if (cartEntity == null || cartEntity.getItems() == null || cartEntity.getItems().isEmpty()) {
                displayEmptyCart();
                return;
            }

            // Update the currentCartItemDTOs with fresh data from database
            currentCartItemDTOs.clear();
            float grandTotalExclVAT = 0f;
            boolean hasStockIssues = false;

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
                grandTotalExclVAT += dto.getTotalPriceExclVAT();

                if (!dto.isStockSufficient()) {
                    hasStockIssues = true;
                }
            }

            // Update UI totals and state
            totalCartPriceLabel.setText(String.format("Total (excl. VAT): %,.0f VND", grandTotalExclVAT));
            checkoutButton.setDisable(hasStockIssues);

            if (hasStockIssues) {
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
        }
    }

    /**
     * Called by CartItemRowController when an item is to be removed.
     */
    public void handleRemoveItemFromRow(CartItemDTO itemDto) {
        if (cartService == null) {
            System.err.println("CartService unavailable");
            return;
        }
        // Confirmation might be handled inside the row controller or here
        try {
            System.out.println("Removing item " + itemDto.getProductId() + " from cart " + cartSessionId);
            cartService.removeItemFromCart(cartSessionId, itemDto.getProductId());
            loadCartDetails(); // Refresh the whole cart view
        } catch (SQLException | ResourceNotFoundException e) {
            System.err.println("Remove item failed: " + e.getMessage());
            setStockWarning("Remove failed: " + e.getMessage(), true);
            loadCartDetails(); // Revert UI by reloading
        }
    }
}