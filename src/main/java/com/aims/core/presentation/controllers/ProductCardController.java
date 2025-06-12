package com.aims.core.presentation.controllers;

import com.aims.core.entities.Product;
import com.aims.core.application.services.ICartService;
import com.aims.core.presentation.utils.CartSessionManager;
import com.aims.core.presentation.utils.ProductStateManager;
import com.aims.core.presentation.utils.StockLimitDialog;
import com.aims.core.entities.Cart;
import com.aims.core.entities.CartItem;
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


public class ProductCardController implements ProductStateManager.ProductStateListener {

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
    private com.aims.core.application.services.IProductService productService; // For refreshing product data
    private MainLayoutController mainLayoutController; // Để điều hướng khi click vào card
    // private FXMLSceneManager sceneManager;

    // Biến để tham chiếu đến controller cha nếu cần gọi lại (ví dụ: refresh home screen sau khi thêm vào giỏ)
    private HomeScreenController homeScreenController;
    private ProductSearchResultsController productSearchResultsController; // For cards on search results page
    private String searchContext; // Added for preserving search context
    private boolean isStateListenerRegistered = false; // Track listener registration


    public ProductCardController() {
        // Constructor - services will be injected or set
    }

    // ProductStateManager.ProductStateListener implementation
    @Override
    public String getInterestedProductId() {
        return product != null ? product.getProductId() : null;
    }

    @Override
    public void onProductUpdated(Product updatedProduct) {
        if (product != null && product.getProductId().equals(updatedProduct.getProductId())) {
            System.out.println("ProductCard: Received state update for " + updatedProduct.getTitle() +
                             " - Stock: " + updatedProduct.getQuantityInStock() +
                             " (was: " + product.getQuantityInStock() + ")");
            
            // Update product data and refresh UI on JavaFX Application Thread
            javafx.application.Platform.runLater(() -> {
                setData(updatedProduct);
            });
        }
    }

    /**
     * Setter cho CartService, được gọi bởi controller cha (HomeScreenController).
     */
    public void setCartService(ICartService cartService) {
        this.cartService = cartService;
    }

    /**
     * Setter cho ProductService, để refresh product data khi cần.
     */
    public void setProductService(com.aims.core.application.services.IProductService productService) {
        this.productService = productService;
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

    public void setProductSearchResultsController(ProductSearchResultsController productSearchResultsController) {
        this.productSearchResultsController = productSearchResultsController;
    }

    /**
     * Sets the search context, e.g., the query string from a search results page.
     * This context can be passed to the product detail screen.
     * @param searchContext The search context string.
     */
    public void setSearchContext(String searchContext) {
        this.searchContext = searchContext;
    }

    /**
     * Initialize method called by JavaFX after FXML loading
     */
    public void initialize() {
        // Register for product state updates
        if (!isStateListenerRegistered) {
            ProductStateManager.addListener(this);
            isStateListenerRegistered = true;
            System.out.println("ProductCardController.initialize: Registered with ProductStateManager");
        }
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

        // Load image with responsive sizing
        loadProductImage();
        
        // Set up responsive image sizing after card is fully initialized
        javafx.application.Platform.runLater(() -> updateImageSizeForCard());

        // CRITICAL FIX: Update product state cache for consistency
        if (product != null) {
            ProductStateManager.updateProduct(product);
        }
    }

    /**
     * Load product image with proper error handling and responsive sizing
     */
    private void loadProductImage() {
        if (product.getImageUrl() != null && !product.getImageUrl().trim().isEmpty()) {
            try {
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
            // The path "/assets/images/product_placeholder.png" assumes 'assets' is a root classpath directory.
            // If 'assets/images/product_placeholder.png' is at the project root and not on classpath, this will fail.
            // Consider moving it to src/main/resources/images/ and using "/images/product_placeholder.png".
            java.io.InputStream placeholderStream = getClass().getResourceAsStream("/assets/images/product_placeholder.png");
            if (placeholderStream == null) {
                System.err.println("Error loading placeholder image: Resource /assets/images/product_placeholder.png not found.");
                // Attempt to load from a path relative to 'src/main/resources' as a fallback
                placeholderStream = getClass().getResourceAsStream("/images/product_placeholder.png"); // Common for resources
                 if (placeholderStream == null) {
                    System.err.println("Error loading placeholder image: Resource /images/product_placeholder.png also not found.");
                    return;
                 }
            }
            Image placeholder = new Image(placeholderStream);
            if (placeholder.isError()) {
                String errorMessage = "Error loading placeholder image from resource.";
                if (placeholder.getException() != null) {
                    errorMessage += " Exception: " + placeholder.getException().getMessage();
                }
                System.err.println(errorMessage);
            } else {
                productImageView.setImage(placeholder);
                System.out.println("Loaded placeholder image for product card via getResourceAsStream.");
            }
        } catch (Exception e) { // Catch any other unexpected exceptions during the process
            System.err.println("Unexpected error in loadPlaceholderImage: " + e.getMessage());
            // e.printStackTrace(); // For more detailed debugging if needed during development
        }
    }
    
    /**
     * Update image size to scale with the card size for fullscreen responsiveness
     */
    private void updateImageSizeForCard() {
        if (productCardVBox == null || productImageView == null) {
            return;
        }
        
        try {
            double cardWidth = productCardVBox.getWidth();
            double cardHeight = productCardVBox.getHeight();
            
            // If card dimensions are not set yet, use preferred width
            if (cardWidth <= 0) {
                cardWidth = productCardVBox.getPrefWidth();
            }
            if (cardHeight <= 0) {
                cardHeight = productCardVBox.getPrefHeight();
            }
            
            // Default fallback dimensions
            if (cardWidth <= 0) cardWidth = 280;
            if (cardHeight <= 0) cardHeight = 350;
            
            // Calculate optimal image size (roughly 60-70% of card width for good proportion)
            double imageSize = Math.min(cardWidth * 0.7, cardHeight * 0.5);
            imageSize = Math.max(imageSize, 150); // Minimum size
            imageSize = Math.min(imageSize, 400); // Maximum size to prevent oversized images
            
            productImageView.setFitWidth(imageSize);
            productImageView.setFitHeight(imageSize);
            productImageView.setPreserveRatio(true);
            productImageView.setSmooth(true);
            
            System.out.println("ProductCardController: Updated image size to " + imageSize + "x" + imageSize + " for card " + cardWidth + "x" + cardHeight);
            
        } catch (Exception e) {
            System.err.println("ProductCardController.updateImageSizeForCard: Error updating image size: " + e.getMessage());
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

    /**
     * Performs client-side cart validation before adding item to cart
     */
    private void performCartValidation(Product product, ICartService cartService, String cartSessionId)
            throws SQLException, InventoryException {
        
        // Get current cart to check existing quantity
        Cart currentCart = cartService.getCart(cartSessionId);
        int currentQuantityInCart = 0;
        
        if (currentCart != null && currentCart.getItems() != null) {
            for (CartItem item : currentCart.getItems()) {
                if (item.getProduct().getProductId().equals(product.getProductId())) {
                    currentQuantityInCart = item.getQuantity();
                    break;
                }
            }
        }
        
        // Check if we can add one more item
        int availableToAdd = product.getQuantityInStock() - currentQuantityInCart;
        if (availableToAdd <= 0) {
            throw new InventoryException(String.format(
                "Cannot add more %s to cart. You already have %d items (maximum available: %d)",
                product.getTitle(), currentQuantityInCart, product.getQuantityInStock()
            ));
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
            
            // Perform client-side validation first
            performCartValidation(product, cartService, currentCartSessionId);
            
            cartService.addItemToCart(currentCartSessionId, product.getProductId(), 1);
            
            // Success feedback
            System.out.println("Successfully added " + product.getTitle() + " to cart");
            showSuccessFeedback();

            // CRITICAL FIX: Refresh product data from database instead of local modification
            refreshProductFromDatabase();

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
            handleInventoryException(e);
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
        
        if (productService == null) {
            System.err.println("ProductCardController: ProductService is null - attempting to initialize from ServiceFactory");
            try {
                com.aims.core.shared.ServiceFactory serviceFactory = com.aims.core.shared.ServiceFactory.getInstance();
                this.productService = serviceFactory.getProductService();
                System.out.println("ProductCardController: ProductService initialized from ServiceFactory: " + (productService != null));
            } catch (Exception e) {
                System.err.println("ProductCardController: Failed to initialize ProductService: " + e.getMessage());
                // Product service failure will be handled by the calling method
            }
        }
    }

    /**
     * Refreshes product data from database to ensure consistency across UI components.
     * This method prevents stock inconsistency issues between different views.
     */
    private void refreshProductFromDatabase() {
        if (product == null || product.getProductId() == null) {
            return;
        }
        
        // Validate and initialize services if needed
        validateAndInitializeServices();
        
        if (productService == null) {
            System.err.println("ProductCardController.refreshProductFromDatabase: ProductService unavailable, cannot refresh");
            return;
        }
        
        try {
            // Fetch fresh product data from database
            Product refreshedProduct = productService.getProductById(product.getProductId());
            if (refreshedProduct != null) {
                System.out.println("ProductCardController.refreshProductFromDatabase: Refreshed " + refreshedProduct.getTitle() +
                                 " - Stock: " + refreshedProduct.getQuantityInStock());
                // Update UI with fresh data
                setData(refreshedProduct);
            } else {
                System.err.println("ProductCardController.refreshProductFromDatabase: Product not found: " + product.getProductId());
            }
        } catch (SQLException e) {
            System.err.println("ProductCardController.refreshProductFromDatabase: Database error: " + e.getMessage());
            // Continue with existing data on error
        } catch (Exception e) {
            System.err.println("ProductCardController.refreshProductFromDatabase: Unexpected error: " + e.getMessage());
            // Continue with existing data on error
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

    private void handleInventoryException(InventoryException e) {
        String message = e.getMessage();
        if (message.contains("Insufficient stock")) {
            // Parse error message or use current product state for enhanced dialog
            try {
                String currentCartSessionId = CartSessionManager.getOrCreateCartSessionId();
                Cart currentCart = cartService.getCart(currentCartSessionId);
                int currentQuantityInCart = 0;
                
                if (currentCart != null && currentCart.getItems() != null) {
                    for (CartItem item : currentCart.getItems()) {
                        if (item.getProduct().getProductId().equals(product.getProductId())) {
                            currentQuantityInCart = item.getQuantity();
                            break;
                        }
                    }
                }
                
                int availableToAdd = Math.max(0, product.getQuantityInStock() - currentQuantityInCart);
                
                if (product.getQuantityInStock() <= 0) {
                    StockLimitDialog.showOutOfStockWarning(product.getTitle());
                    updateOutOfStockState();
                } else {
                    StockLimitDialog.showStockLimitWarning(product.getTitle(), currentQuantityInCart,
                                                         product.getQuantityInStock(), availableToAdd);
                }
            } catch (Exception ex) {
                // Fallback to simple error message
                showErrorFeedback("Cannot add more items to cart. Stock limit reached.");
            }
        } else {
            // Generic inventory error
            updateOutOfStockState();
            showErrorFeedback("Product is out of stock.");
        }
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
                if (productSearchResultsController != null) {
                    System.out.println("ProductCardController.handleViewProductDetails: Navigating via ProductSearchResultsController");
                    productSearchResultsController.navigateToProductDetail(product.getProductId());
                } else if (homeScreenController != null) {
                    System.out.println("ProductCardController.handleViewProductDetails: Navigating via HomeScreenController");
                    homeScreenController.navigateToProductDetail(product.getProductId());
                } else {
                    // Fallback if no parent controller is set (should ideally not happen if cards are always part of a screen)
                    System.out.println("ProductCardController.handleViewProductDetails: Fallback navigation via MainLayoutController directly");
                    // This will use loadContentWithHistory, but the specific search context (term, filter, page)
                    // from the search results screen won't be explicitly preserved by the ProductCardController itself.
                    // The FXMLSceneManager will preserve the *current screen* (e.g., search results) in history.
                    Object controller = mainLayoutController.loadContentWithHistory(com.aims.core.shared.constants.FXMLPaths.PRODUCT_DETAIL_SCREEN, "Product Details");
                     if (controller instanceof ProductDetailScreenController detailController) {
                        detailController.setProductId(product.getProductId());
                    }
                    mainLayoutController.setHeaderTitle("Product Details: " + product.getTitle());
                }
                System.out.println("ProductCardController.handleViewProductDetails: Navigation initiated for product: " + product.getTitle());

            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("ProductCardController.handleViewProductDetails: Navigation Error: Could not load product details screen - " + e.getMessage());
            }
        } else {
            System.err.println("ProductCardController.handleViewProductDetails: MainLayoutController or product is null - cannot navigate");
        }
    }

    /**
     * Cleanup method to remove state listener when card is destroyed.
     * This prevents memory leaks and should be called when the card is no longer needed.
     */
    public void cleanup() {
        if (isStateListenerRegistered) {
            ProductStateManager.removeListener(this);
            isStateListenerRegistered = false;
            System.out.println("ProductCardController.cleanup: Removed listener from ProductStateManager");
        }
    }
}