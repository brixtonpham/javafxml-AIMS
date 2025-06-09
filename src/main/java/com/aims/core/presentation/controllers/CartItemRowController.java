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
                }
                else {
                    parentCartScreenController.handleUpdateQuantityFromRow(this.cartItem, newValue);
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
                System.err.println("Error loading image for cart item: " + e.getMessage());
                // Load placeholder
            }
        } else {
            // Load placeholder
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
}