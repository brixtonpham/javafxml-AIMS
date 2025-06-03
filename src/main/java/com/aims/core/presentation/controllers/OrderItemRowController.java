package com.aims.core.presentation.controllers;

import com.aims.core.entities.OrderItem; // Hoặc OrderItemDTO nếu bạn dùng
import com.aims.core.entities.Product;   // Để lấy title, image URL

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

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

    private OrderItem orderItem;

    public OrderItemRowController() {
        // Constructor
    }

    public void initialize() {
        // Khởi tạo nếu cần
    }

    /**
     * Sets the data for this order item row.
     * @param item The OrderItem entity or DTO.
     */
    public void setData(OrderItem item) {
        this.orderItem = item;
        if (item == null) {
            // Hide or clear the row if item is null
            orderItemRowPane.setVisible(false);
            orderItemRowPane.setManaged(false);
            return;
        }
        orderItemRowPane.setVisible(true);
        orderItemRowPane.setManaged(true);

        Product product = item.getProduct(); // Giả sử OrderItem chứa đối tượng Product

        if (product != null) {
            productTitleLabel.setText(product.getTitle());
            productIdLabel.setText("ID: " + product.getProductId());
            if (product.getImageUrl() != null && !product.getImageUrl().trim().isEmpty()) {
                try {
                    productImageView.setImage(new Image(product.getImageUrl(), true)); // true for background loading
                } catch (Exception e) {
                    System.err.println("Error loading image for order item row (" + product.getTitle() + "): " + product.getImageUrl() + " - " + e.getMessage());
                    loadPlaceholderImage();
                }
            } else {
                loadPlaceholderImage();
            }
        } else {
            productTitleLabel.setText("Product Data Missing");
            productIdLabel.setText("ID: N/A");
            loadPlaceholderImage();
        }

        quantityLabel.setText("Qty: " + item.getQuantity());
        // Giá này là giá tại thời điểm đặt hàng, đã lưu trong OrderItem (chưa VAT)
        unitPriceLabel.setText(String.format("Unit: %,.0f VND", item.getPriceAtTimeOfOrder()));
        totalItemPriceLabel.setText(String.format("Total: %,.0f VND", item.getPriceAtTimeOfOrder() * item.getQuantity()));
    }

    private void loadPlaceholderImage() {
        try {
            // Image placeholder = new Image(getClass().getResourceAsStream("/assets/images/product_placeholder.png"));
            // productImageView.setImage(placeholder);
        } catch (Exception e) {
            System.err.println("Error loading placeholder image for order item row: " + e.getMessage());
        }
    }
}