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
    private String cartSessionId = "guest_cart_session_id_placeholder"; // TODO: Get this from session management

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
        // Load cart details when the screen is initialized
        // cartSessionId should be set by the time this screen is shown
        // For testing, you can call loadCartDetails directly or set a default session ID.
        if (cartSessionId != null) {
            loadCartDetails();
        } else {
            System.err.println("CartScreenController: cartSessionId is null on initialize. Cart will be empty.");
            displayEmptyCart();
        }
    }

    private void loadCartDetails() {
        // if (cartService == null) {
        //     AlertHelper.showErrorAlert("Service Error", "Cart service is unavailable.");
        //     displayEmptyCart();
        //     return;
        // }
        // if (cartSessionId == null || cartSessionId.isEmpty()) {
        //     AlertHelper.showErrorAlert("Error", "Cart session is not identified.");
        //     displayEmptyCart();
        //     return;
        // }
        //
        // try {
        //     Cart cartEntity = cartService.getCart(cartSessionId); // This service method should return the Cart with its items
        //
        //     cartItemsContainerVBox.getChildren().clear();
        //     currentCartItemDTOs.clear();
        //     setStockWarning("", false);
        //
        //     if (cartEntity != null && cartEntity.getItems() != null && !cartEntity.getItems().isEmpty()) {
        //         float grandTotalExclVAT = 0f;
        //         boolean hasStockIssues = false;
        //
        //         for (com.aims.core.entities.CartItem itemEntity : cartEntity.getItems()) {
        //             Product product = itemEntity.getProduct(); // Assuming product is eagerly loaded or accessible
        //             if (product == null) continue; // Should not happen if data is consistent
        //
        //             CartItemDTO dto = new CartItemDTO(
        //                     product.getProductId(),
        //                     product.getTitle(),
        //                     itemEntity.getQuantity(),
        //                     product.getPrice(), // Price ex-VAT from Product entity
        //                     product.getImageUrl(),
        //                     product.getQuantityInStock()
        //             );
        //             currentCartItemDTOs.add(dto);
        //             grandTotalExclVAT += dto.getTotalPriceExclVAT();
        //
        //             if (!dto.isStockSufficient()) {
        //                 hasStockIssues = true;
        //             }
        //
        //             // Load the FXML for each cart item row/card
        //             try {
        //                 FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/aims/presentation/views/partials/cart_item_row_card_style.fxml"));
        //                 Node itemNode = loader.load();
        //                 CartItemRowController rowController = loader.getController();
        //                 rowController.setData(dto, this.cartService, this.cartSessionId, this);
        //                 cartItemsContainerVBox.getChildren().add(itemNode);
        //             } catch (IOException e) {
        //                 e.printStackTrace();
        //                 System.err.println("Error loading cart item row FXML: " + e.getMessage());
        //             }
        //         }
        //
        //         totalCartPriceLabel.setText(String.format("Total (excl. VAT): %,.0f VND", grandTotalExclVAT));
        //         checkoutButton.setDisable(hasStockIssues); // Disable checkout if any stock issue
        //         if (hasStockIssues) {
        //             setStockWarning("One or more items have insufficient stock. Please update quantities.", true);
        //         }
        //
        //     } else {
        //         displayEmptyCart();
        //     }
        // } catch (SQLException e) {
        //     e.printStackTrace();
        //     // AlertHelper.showErrorAlert("Database Error", "Could not load cart details: " + e.getMessage());
        //     displayEmptyCart();
        //     setStockWarning("Error loading cart: " + e.getMessage(), true);
        // }

        System.out.println("loadCartDetails called - Implement with actual service call and DTO mapping.");
        // Dummy display
        displayEmptyCart();
        // setStockWarning("This is a sample stock warning.", true);
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

    private void setStockWarning(String message, boolean visible) {
        stockWarningLabel.setText(message);
        stockWarningLabel.setVisible(visible);
        stockWarningLabel.setManaged(visible);
    }

    @FXML
    void handleClearCartAction(ActionEvent event) {
        // if (cartService == null) { AlertHelper.showErrorAlert("Error", "Service unavailable."); return; }
        // if (cartSessionId == null || cartSessionId.isEmpty()) return;
        //
        // boolean confirmed = AlertHelper.showConfirmationDialog("Clear Cart", "Are you sure you want to empty your entire cart?");
        // if (confirmed) {
        //     try {
        //         cartService.clearCart(cartSessionId);
        //         AlertHelper.showInfoAlert("Cart Cleared", "Your shopping cart has been emptied.");
        //         loadCartDetails(); // Refresh view
        //     } catch (SQLException | ResourceNotFoundException e) {
        //         AlertHelper.showErrorAlert("Error Clearing Cart", e.getMessage());
        //     }
        // }
        System.out.println("Clear Cart action - implement");
        loadCartDetails(); // Simulate refresh
    }

    @FXML
    void handleProceedToCheckoutAction(ActionEvent event) {
        // if (currentCartItemDTOs.isEmpty()) {
        //     AlertHelper.showWarningAlert("Empty Cart", "Please add items to your cart before proceeding to checkout.");
        //     return;
        // }
        // boolean hasStockIssues = currentCartItemDTOs.stream().anyMatch(item -> !item.isStockSufficient());
        // if (hasStockIssues) {
        //      AlertHelper.showErrorAlert("Stock Issue", "Please resolve stock issues in your cart before proceeding.");
        //      return;
        // }
        //
        // System.out.println("Proceed to Checkout action triggered for cart: " + cartSessionId);
        // if (mainLayoutController != null && sceneManager != null) {
        //     // The OrderService.initiateOrderFromCart will be called first.
        //     // If successful, it will then navigate to DeliveryInfoScreen.
        //     // This logic might be better placed in OrderService or a checkout orchestration service.
        //     // For now, let's assume we navigate and DeliveryInfoScreen handles order initiation.
        //
        //     DeliveryInfoScreenController deliveryCtrl = (DeliveryInfoScreenController) sceneManager.loadFXMLIntoPane(
        //         mainLayoutController.getContentPane(), FXMLSceneManager.DELIVERY_INFO_SCREEN
        //     );
        //     // DeliveryInfoScreenController needs the cartSessionId or the newly created OrderEntity shell
        //     // from OrderService.initiateOrderFromCart()
        //     // deliveryCtrl.setCartSessionForOrder(this.cartSessionId);
        //     // deliveryCtrl.setMainLayoutController(mainLayoutController);
        //     mainLayoutController.setHeaderTitle("Delivery Information");
        // }
        System.out.println("Proceed to Checkout action - implement navigation and order initiation.");
    }

    /**
     * Called by CartItemRowController when an item's quantity changes.
     */
    public void handleUpdateQuantityFromRow(CartItemDTO itemDto, int newQuantity) {
        // if (cartService == null) { return; }
        // try {
        //     System.out.println("Updating quantity for " + itemDto.getProductId() + " to " + newQuantity + " in cart " + cartSessionId);
        //     cartService.updateItemQuantity(cartSessionId, itemDto.getProductId(), newQuantity);
        //     loadCartDetails(); // Refresh the whole cart view
        // } catch (SQLException | ResourceNotFoundException | ValidationException | InventoryException e) {
        //     AlertHelper.showErrorAlert("Update Failed", e.getMessage());
        //     loadCartDetails(); // Revert UI by reloading if update fails
        // }
         System.out.println("Update quantity for " + itemDto.getProductId() + " to " + newQuantity + " - implement");
         loadCartDetails(); // Simulate refresh
    }

    /**
     * Called by CartItemRowController when an item is to be removed.
     */
    public void handleRemoveItemFromRow(CartItemDTO itemDto) {
        // if (cartService == null) { return; }
        // // Confirmation might be handled inside the row controller or here
        // try {
        //     System.out.println("Removing item " + itemDto.getProductId() + " from cart " + cartSessionId);
        //     cartService.removeItemFromCart(cartSessionId, itemDto.getProductId());
        //     loadCartDetails(); // Refresh the whole cart view
        // } catch (SQLException | ResourceNotFoundException e) {
        //     AlertHelper.showErrorAlert("Remove Failed", e.getMessage());
        //     loadCartDetails(); // Revert UI by reloading
        // }
        System.out.println("Remove item " + itemDto.getProductId() + " - implement");
        loadCartDetails(); // Simulate refresh
    }
}