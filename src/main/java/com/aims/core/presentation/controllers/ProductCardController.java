package com.aims.core.presentation.controllers;

import com.aims.core.entities.Product;
import com.aims.core.application.services.ICartService;
import com.aims.core.presentation.utils.CartSessionManager;
// import com.aims.presentation.utils.AlertHelper;
// import com.aims.presentation.utils.FXMLSceneManager; // For navigation
// import com.aims.MainLayoutController; // To navigate

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;

import java.sql.SQLException; // From ICartService
import com.aims.core.shared.exceptions.ValidationException;
import com.aims.core.shared.exceptions.ResourceNotFoundException;
import com.aims.core.shared.exceptions.InventoryException;


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
    private ICartService cartService; // Sẽ được inject hoặc set từ controller cha (HomeScreenController)
    private MainLayoutController mainLayoutController; // Để điều hướng khi click vào card
    // private FXMLSceneManager sceneManager;

    // Biến để tham chiếu đến controller cha nếu cần gọi lại (ví dụ: refresh home screen sau khi thêm vào giỏ)
    private HomeScreenController homeScreenController;


    public ProductCardController() {
        // Constructor - services will be injected or set
    }

    /**
     * Setter cho CartService, được gọi bởi controller cha (HomeScreenController).
     */
    public void setCartService(ICartService cartService) {
        this.cartService = cartService;
    }

    /**
     * Setter cho MainLayoutController, được gọi bởi controller cha.
     */
    public void setMainLayoutController(MainLayoutController mainLayoutController) {
        this.mainLayoutController = mainLayoutController;
    }

    /**
     * Setter cho HomeScreenController, để có thể gọi lại các phương thức của nó nếu cần.
     */
    public void setHomeScreenController(HomeScreenController homeScreenController) {
        this.homeScreenController = homeScreenController;
    }

    /**
     * Điền dữ liệu sản phẩm vào card.
     * @param product Đối tượng Product (đã bao gồm giá có VAT nếu hiển thị cho khách hàng).
     */
    public void setData(Product product) {
        this.product = product;
        if (product == null) {
            // Xử lý trường hợp product là null, ví dụ ẩn card hoặc hiển thị placeholder
            productCardVBox.setVisible(false);
            productCardVBox.setManaged(false);
            return;
        }
        productCardVBox.setVisible(true);
        productCardVBox.setManaged(true);

        productTitleLabel.setText(product.getTitle());
        // Giá hiển thị trên card nên là giá đã bao gồm VAT.
        // ProductService.getProductsForDisplay() nên trả về sản phẩm với giá đã tính VAT.
        productPriceLabel.setText(String.format("%,.0f VND", product.getPrice()));
        productAvailabilityLabel.setText("Available: " + product.getQuantityInStock());

        updateAddToCartButtonState();

        // Load image
        if (product.getImageUrl() != null && !product.getImageUrl().trim().isEmpty()) {
            try {
                // Giả sử getImageUrl() trả về một URL hợp lệ hoặc đường dẫn file resource
                // Nếu là URL từ web: new Image(product.getImageUrl(), true) // true for background loading
                // Nếu là resource trong project: new Image(getClass().getResourceAsStream(product.getImageUrl()))
                Image image = new Image(product.getImageUrl(), true); // true for background loading
                productImageView.setImage(image);
            } catch (Exception e) {
                System.err.println("Error loading image for product card (" + product.getTitle() + "): " + product.getImageUrl() + " - " + e.getMessage());
                loadPlaceholderImage();
            }
        } else {
            loadPlaceholderImage();
        }
    }

    private void loadPlaceholderImage() {
        try {
            // Image placeholder = new Image(getClass().getResourceAsStream("/assets/images/product_placeholder.png"));
            // productImageView.setImage(placeholder);
            // System.out.println("Loaded placeholder image for product card.");
        } catch (Exception e) {
            System.err.println("Error loading placeholder image: " + e.getMessage());
        }
    }

    private void updateAddToCartButtonState() {
        if (product.getQuantityInStock() <= 0) {
            addToCartButton.setText("Out of Stock");
            addToCartButton.setDisable(true);
            productAvailabilityLabel.setText("Out of Stock");
            productAvailabilityLabel.setStyle("-fx-text-fill: red;");
        } else {
            addToCartButton.setText("Add to Cart");
            addToCartButton.setDisable(false);
            productAvailabilityLabel.setText("Available: " + product.getQuantityInStock());
            productAvailabilityLabel.setStyle(""); // Reset style
        }
    }

    @FXML
    void handleAddToCartAction(ActionEvent event) {
        if (product == null) return;
        System.out.println("Add to cart clicked for (Card): " + product.getTitle());

        // Validate and initialize services if needed
        validateAndInitializeServices();

        if (cartService == null) {
            System.err.println("CartService not available in ProductCardController after validation.");
            showCartServiceUnavailableMessage();
            return;
        }

        // Check stock availability before attempting to add
        if (product.getQuantityInStock() <= 0) {
            updateOutOfStockState();
            return;
        }

        // Disable button temporarily to prevent double-clicks
        addToCartButton.setDisable(true);
        String originalButtonText = addToCartButton.getText();
        addToCartButton.setText("Adding...");

        try {
            // Generate or get session ID using centralized session manager
            String currentCartSessionId = CartSessionManager.getOrCreateCartSessionId();
            cartService.addItemToCart(currentCartSessionId, product.getProductId(), 1);
            
            // Success feedback
            System.out.println("Successfully added " + product.getTitle() + " to cart");
            showSuccessFeedback();

            // Update product stock and UI
            product.setQuantityInStock(product.getQuantityInStock() - 1);
            updateAddToCartButtonState();

            // Notify parent controller if available
            if (homeScreenController != null) {
                homeScreenController.refreshCartSummary();
            }

        } catch (SQLException e) {
            System.err.println("Database error adding product to cart: " + e.getMessage());
            showErrorFeedback("Database error occurred. Please try again.");
        } catch (ResourceNotFoundException e) {
            System.err.println("Product not found: " + e.getMessage());
            showErrorFeedback("Product no longer available.");
        } catch (ValidationException e) {
            System.err.println("Validation error: " + e.getMessage());
            showErrorFeedback("Invalid request. Please try again.");
        } catch (InventoryException e) {
            System.err.println("Inventory error: " + e.getMessage());
            updateOutOfStockState();
            showErrorFeedback("Product is out of stock.");
        } catch (Exception e) {
            System.err.println("Unexpected error adding to cart: " + e.getMessage());
            showErrorFeedback("An error occurred. Please try again.");
        } finally {
            // Re-enable button
            addToCartButton.setDisable(false);
            if (product.getQuantityInStock() > 0) {
                addToCartButton.setText(originalButtonText);
            }
        }
        
        event.consume();
    }


    /**
     * Validates that required services are available and attempts to initialize them if needed.
     * This provides a fallback mechanism when dependency injection fails.
     */
    private void validateAndInitializeServices() {
        if (cartService == null) {
            System.err.println("ProductCardController: CartService is null - attempting to initialize from ServiceFactory");
            try {
                com.aims.core.shared.ServiceFactory serviceFactory = com.aims.core.shared.ServiceFactory.getInstance();
                this.cartService = serviceFactory.getCartService();
                System.out.println("ProductCardController: CartService initialized from ServiceFactory: " + (cartService != null));
            } catch (Exception e) {
                System.err.println("ProductCardController: Failed to initialize CartService: " + e.getMessage());
                // Cart service failure will be handled by the calling method
            }
        }
    }

    private void updateOutOfStockState() {
        addToCartButton.setText("Out of Stock");
        addToCartButton.setDisable(true);
        productAvailabilityLabel.setText("Out of Stock");
        productAvailabilityLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
    }

    private void showCartServiceUnavailableMessage() {
        addToCartButton.setText("Service Unavailable");
        addToCartButton.setDisable(true);
        productAvailabilityLabel.setText("Cart service unavailable");
        productAvailabilityLabel.setStyle("-fx-text-fill: orange; -fx-font-weight: bold;");
    }

    private void showSuccessFeedback() {
        // Temporarily change button appearance to show success
        String originalStyle = addToCartButton.getStyle();
        addToCartButton.setStyle(originalStyle + "; -fx-background-color: #4CAF50; -fx-text-fill: white;");
        addToCartButton.setText("Added!");
        
        // Reset after 1 second
        javafx.concurrent.Task<Void> resetTask = new javafx.concurrent.Task<Void>() {
            @Override
            protected Void call() throws Exception {
                Thread.sleep(1000);
                return null;
            }
            
            @Override
            protected void succeeded() {
                javafx.application.Platform.runLater(() -> {
                    addToCartButton.setStyle(originalStyle);
                    updateAddToCartButtonState();
                });
            }
        };
        new Thread(resetTask).start();
    }

    private void showErrorFeedback(String message) {
        // Temporarily show error state
        String originalStyle = addToCartButton.getStyle();
        addToCartButton.setStyle(originalStyle + "; -fx-background-color: #f44336; -fx-text-fill: white;");
        addToCartButton.setText("Error");
        
        // Show error message in availability label temporarily
        String originalAvailabilityText = productAvailabilityLabel.getText();
        String originalAvailabilityStyle = productAvailabilityLabel.getStyle();
        productAvailabilityLabel.setText(message);
        productAvailabilityLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
        
        // Reset after 2 seconds
        javafx.concurrent.Task<Void> resetTask = new javafx.concurrent.Task<Void>() {
            @Override
            protected Void call() throws Exception {
                Thread.sleep(2000);
                return null;
            }
            
            @Override
            protected void succeeded() {
                javafx.application.Platform.runLater(() -> {
                    addToCartButton.setStyle(originalStyle);
                    productAvailabilityLabel.setText(originalAvailabilityText);
                    productAvailabilityLabel.setStyle(originalAvailabilityStyle);
                    updateAddToCartButtonState();
                });
            }
        };
        new Thread(resetTask).start();
    }

    @FXML
    void handleViewProductDetails(MouseEvent event) {
        // Chỉ xử lý nếu click không phải vào nút "Add to Cart" (nếu nút nằm trong VBox)
        // Hoặc nếu muốn toàn bộ card là clickable để xem chi tiết
        if (event.getTarget() == addToCartButton || (addToCartButton.getGraphic() != null && event.getTarget() == addToCartButton.getGraphic())) {
            return; // Sự kiện đã được nút xử lý, không làm gì thêm
        }

        if (product == null) return;
        System.out.println("ProductCardController.handleViewProductDetails: View details action for product: " + product.getTitle());
        System.out.println("ProductCardController.handleViewProductDetails: Product ID: " + product.getProductId());
        System.out.println("ProductCardController.handleViewProductDetails: MainLayoutController available: " + (mainLayoutController != null));

        if (mainLayoutController != null && product != null) {
            try {
                System.out.println("ProductCardController.handleViewProductDetails: About to call loadContent for product_detail_screen.fxml");
                
                // Use MainLayoutController's loadContent method which handles service injection
                Object controller = mainLayoutController.loadContent("/com/aims/presentation/views/product_detail_screen.fxml");
                
                System.out.println("ProductCardController.handleViewProductDetails: LoadContent returned controller: " + 
                    (controller != null ? controller.getClass().getSimpleName() : "null"));
                
                // Set the product ID on the ProductDetailScreenController after ensuring services are ready
                if (controller instanceof ProductDetailScreenController) {
                    System.out.println("ProductCardController.handleViewProductDetails: Controller is ProductDetailScreenController, deferring product ID setting");
                    ProductDetailScreenController detailController = (ProductDetailScreenController) controller;
                    
                    // Defer the setProductId call to ensure services are injected first
                    javafx.application.Platform.runLater(() -> {
                        System.out.println("ProductCardController.handleViewProductDetails: Setting product ID after service injection");
                        detailController.setProductId(product.getProductId());
                        System.out.println("ProductCardController.handleViewProductDetails: Product ID set on ProductDetailScreenController");
                    });
                } else {
                    System.err.println("ProductCardController.handleViewProductDetails: Controller is not ProductDetailScreenController, it's: " +
                        (controller != null ? controller.getClass().getSimpleName() : "null"));
                }
                
                // Update header title
                System.out.println("ProductCardController.handleViewProductDetails: Updating header title");
                mainLayoutController.setHeaderTitle("Product Details: " + product.getTitle());
                System.out.println("ProductCardController.handleViewProductDetails: Navigation completed successfully");

            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("ProductCardController.handleViewProductDetails: Navigation Error: Could not load product details screen - " + e.getMessage());
            }
        } else {
            System.err.println("ProductCardController.handleViewProductDetails: MainLayoutController or product is null - cannot navigate");
        }
    }
}