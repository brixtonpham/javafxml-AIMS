package com.aims.core.presentation.utils;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

public class StockLimitDialog {
    
    public static void showStockLimitWarning(String productTitle, int currentInCart, 
                                           int maxAvailable, int availableToAdd) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Stock Limit Reached");
        alert.setHeaderText("Cannot add more items to cart");
        
        String message = String.format(
            "Product: %s%n" +
            "Currently in cart: %d%n" +
            "Maximum available: %d%n" +
            "You can add up to %d more items",
            productTitle, currentInCart, maxAvailable, availableToAdd
        );
        
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    public static void showOutOfStockWarning(String productTitle) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Out of Stock");
        alert.setHeaderText("Product temporarily unavailable");
        alert.setContentText(String.format("Sorry, %s is currently out of stock.", productTitle));
        alert.showAndWait();
    }
}