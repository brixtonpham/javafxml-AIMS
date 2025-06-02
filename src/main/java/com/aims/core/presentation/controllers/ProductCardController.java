package com.aims.presentation.controllers;

import com.aims.core.entities.Product;
import com.aims.core.application.services.ICartService;
// import com.aims.presentation.utils.FXMLSceneManager;
// import com.aims.presentation.utils.AlertHelper;


import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;

import java.sql.SQLException; // For cartService exceptions

public class ProductCardController {

    @FXML
    private VBox productCardVBox;

    @FXML
    private ImageView productImageView;

    @FXML
    private Label productTitleLabel;

    @FXML
    private Label productPriceLabel;

    @FXML
    private Label productAvailabilityLabel;

    @FXML
    private Button addToCartButton;

    private Product product;
    // @Inject
    private ICartService cartService; // Sẽ được inject hoặc set từ HomeScreenController
    // private FXMLSceneManager sceneManager;
    // private MainLayoutController mainLayoutController;

    public ProductCardController() {
         // Ví dụ khởi tạo service (Trong thực tế nên dùng DI)
        // cartService = new CartServiceImpl(...);
    }

    // public void setCartService(ICartService cartService) {
    //     this.cartService = cartService;
    // }
    // public void setSceneManager(FXMLSceneManager sceneManager) { this.sceneManager = sceneManager; }
    // public void setMainLayoutController(MainLayoutController mainLayoutController) { this.mainLayoutController = mainLayoutController; }


    public void setData(Product product) {
        this.product = product;
        productTitleLabel.setText(product.getTitle());
        // Giá này đã bao gồm VAT từ ProductService.getProductDetailsForCustomer()
        productPriceLabel.setText(String.format("%,.0f VND", product.getPrice()));
        productAvailabilityLabel.setText("Available: " + product.getQuantityInStock());
        addToCartButton.setDisable(product.getQuantityInStock() <= 0);

        try {
            if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
                // Giả sử getImageUrl trả về URL hợp lệ hoặc đường dẫn file
                 // Image image = new Image(getClass().getResourceAsStream(product.getImageUrl())); // Nếu là resource
                Image image = new Image(product.getImageUrl(), true); // true for background loading
                productImageView.setImage(image);
            } else {
                // Load placeholder image
                // Image placeholder = new Image(getClass().getResourceAsStream("/assets/images/product_placeholder.png"));
                // productImageView.setImage(placeholder);
            }
        } catch (Exception e) {
            System.err.println("Error loading product image: " + product.getImageUrl() + " - " + e.getMessage());
            // Load placeholder image on error
            // Image placeholder = new Image(getClass().getResourceAsStream("/assets/images/product_placeholder.png"));
            // productImageView.setImage(placeholder);
        }
    }

    @FXML
    void handleAddToCartAction(ActionEvent event) {
        if (product == null) return;
        System.out.println("Add to cart clicked for: " + product.getTitle());
        if (cartService == null) {
             System.err.println("CartService not available in ProductCardController.");
             // AlertHelper.showErrorAlert("Error", "Could not add to cart. Service unavailable.");
             return;
        }

        try {
            // Giả sử có một cartSessionId đang hoạt động (cần cơ chế quản lý session ID này)
            String currentCartSessionId = "guest_cart_session"; // TODO: Lấy session ID thực tế
            cartService.addItemToCart(currentCartSessionId, product.getProductId(), 1);
            System.out.println(product.getTitle() + " added to cart.");
            // AlertHelper.showInfoAlert("Success", product.getTitle() + " added to your cart!");
            // Cập nhật số lượng hiển thị (nếu có) hoặc nút "Add to Cart"
            product.setQuantityInStock(product.getQuantityInStock() - 1); // Cập nhật tạm thời trên UI
            productAvailabilityLabel.setText("Available: " + product.getQuantityInStock());
            addToCartButton.setDisable(product.getQuantityInStock() <= 0);

        } catch (SQLException | ResourceNotFoundException | ValidationException | InventoryException e) {
            e.printStackTrace();
            // AlertHelper.showErrorAlert("Add to Cart Failed", e.getMessage());
        }
    }

    @FXML
    void handleViewProductDetails(MouseEvent event) {
        if (product == null) return;
        System.out.println("View details for: " + product.getTitle());
        // if (sceneManager != null && mainLayoutController != null) {
        //    ProductDetailScreenController detailController = (ProductDetailScreenController) sceneManager.loadFXMLIntoPane(
        //            mainLayoutController.getContentPane(), FXMLSceneManager.PRODUCT_DETAIL_SCREEN
        //    );
        //    detailController.setProductId(product.getProductId());
        //    detailController.setMainLayoutController(mainLayoutController);
        //    mainLayoutController.setHeaderTitle("Product Details: " + product.getTitle());
        // }
    }
}