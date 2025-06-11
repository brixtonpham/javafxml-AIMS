package com.aims.core.presentation.controllers;

import com.aims.core.application.dtos.CartItemDTO;
import com.aims.core.application.services.ICartService;
// import com.aims.presentation.utils.AlertHelper;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.beans.value.ChangeListener;

public class CartItemRowController {

    @FXML
    private HBox cartItemRowPane;
    @FXML
    private ImageView productImageView;
    @FXML
    private Label productTitleLabel;
    @FXML
    private Label unitPriceLabel;
    @FXML
    private Spinner<Integer> quantitySpinner;
    @FXML
    private Label totalItemPriceLabel;
    @FXML
    private Button removeButton;

    private CartItemDTO cartItem;
    private ICartService cartService;
    private CartScreenController parentCartScreenController; // Để gọi refresh hoặc xử lý
    private String cartSessionId;
    private ChangeListener<Integer> quantityChangeListener;
    private boolean isUpdatingSpinner = false;


    public void initialize() {
        SpinnerValueFactory<Integer> valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 100, 1); // Min 0 for removal
        quantitySpinner.setValueFactory(valueFactory);

        // Create and store the listener reference for proper management
        quantityChangeListener = (obs, oldValue, newValue) -> {
            // CRITICAL FIX: Prevent listener from triggering during programmatic updates
            if (isUpdatingSpinner) {
                return;
            }
            
            if (this.cartItem != null && newValue != null && !newValue.equals(oldValue)) {
                if (newValue == 0) {
                    parentCartScreenController.handleRemoveItemFromRow(this.cartItem);
                } else if (newValue > cartItem.getAvailableStock()){
                     // AlertHelper.showWarningAlert("Stock Limit", "Cannot exceed available stock: " + cartItem.getAvailableStock());
                     isUpdatingSpinner = true;
                     quantitySpinner.getValueFactory().setValue(oldValue); // Revert
                     isUpdatingSpinner = false;
                     // Consider uncommenting or replacing AlertHelper.showWarningAlert for better user feedback.
                }
                else {
                    parentCartScreenController.handleUpdateQuantityFromRow(this.cartItem, newValue);
                    updateTotalItemPrice(); // Update total price after successful quantity change
                }
            }
        };
        
        quantitySpinner.valueProperty().addListener(quantityChangeListener);
    }

    public void setData(CartItemDTO cartItem, ICartService cartService, String cartSessionId, CartScreenController parentController) {
        this.cartItem = cartItem;
        this.cartService = cartService;
        this.cartSessionId = cartSessionId;
        this.parentCartScreenController = parentController;

        productTitleLabel.setText(cartItem.getTitle());
        unitPriceLabel.setText(String.format("Unit: %,.0f VND", cartItem.getUnitPriceExclVAT()));
        
        // CRITICAL FIX: Prevent listener triggering during initialization
        // Remove listener temporarily, update value, then re-add listener
        if (quantityChangeListener != null) {
            quantitySpinner.valueProperty().removeListener(quantityChangeListener);
        }
        
        isUpdatingSpinner = true;
        quantitySpinner.getValueFactory().setValue(cartItem.getQuantity());
        isUpdatingSpinner = false;
        
        // Re-add the listener after setting the value
        if (quantityChangeListener != null) {
            quantitySpinner.valueProperty().addListener(quantityChangeListener);
        }
        
        updateTotalItemPrice();

        if (cartItem.getImageUrl() != null && !cartItem.getImageUrl().isEmpty()) {
            try {
                productImageView.setImage(new Image(cartItem.getImageUrl(), true));
            } catch (Exception e) {
                System.err.println("Error loading image for cart item ("+ cartItem.getTitle() +"): " + e.getMessage());
                loadPlaceholderImage();
            }
        } else {
            loadPlaceholderImage();
        }
        
        if (cartItem.getQuantity() > cartItem.getAvailableStock()) {
            // Add some visual indication of stock issue for this item
            productTitleLabel.setStyle("-fx-text-fill: red;");
        } else {
            productTitleLabel.setStyle("");
        }
    }

    private void updateTotalItemPrice() {
        if (cartItem != null) {
            float total = cartItem.getUnitPriceExclVAT() * quantitySpinner.getValue();
            totalItemPriceLabel.setText(String.format("Total: %,.0f VND", total));
        }
    }

    @FXML
    void handleRemoveItemAction(ActionEvent event) {
        if (this.cartItem != null && this.parentCartScreenController != null) {
            parentCartScreenController.handleRemoveItemFromRow(this.cartItem);
        }
    }

    private void loadPlaceholderImage() {
        try {
            // Assuming placeholder is in src/main/resources/images/product_placeholder.png
            java.io.InputStream placeholderStream = getClass().getResourceAsStream("/images/product_placeholder.png");
            if (placeholderStream == null) {
                 // Fallback to assets directory if the above is not found.
                placeholderStream = getClass().getResourceAsStream("/assets/images/product_placeholder.png");
            }

            if (placeholderStream == null) {
                System.err.println("Error loading placeholder image for cart item: Resource not found at /images/product_placeholder.png or /assets/images/product_placeholder.png");
                return;
            }
            Image placeholder = new Image(placeholderStream);
            if (placeholder.isError()) {
                String errorMessage = "Error loading placeholder image from resource for cart item.";
                if (placeholder.getException() != null) {
                    errorMessage += " Exception: " + placeholder.getException().getMessage();
                }
                System.err.println(errorMessage);
            } else {
                productImageView.setImage(placeholder);
            }
        } catch (Exception e) {
            System.err.println("Unexpected error in loadPlaceholderImage (CartItemRowController): " + e.getMessage());
        }
    }
}