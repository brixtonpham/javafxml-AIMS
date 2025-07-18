package com.aims.core.presentation.controllers;

import com.aims.core.application.services.IProductService;
import com.aims.core.application.services.ICartService;
import com.aims.core.entities.Product;
import com.aims.core.entities.Book;
import com.aims.core.entities.CD;
import com.aims.core.entities.DVD;
import com.aims.core.shared.ServiceFactory;
import com.aims.core.shared.constants.FXMLPaths;
import com.aims.core.presentation.utils.NavigationContext;
import com.aims.core.presentation.utils.CartSessionManager;
import com.aims.core.presentation.utils.ProductStateManager;
// import com.aims.presentation.utils.AlertHelper;
// import com.aims.presentation.utils.FXMLSceneManager;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox; // If needed for dynamic content
import javafx.scene.layout.VBox;  // If needed for dynamic content
import javafx.scene.text.Text;   // For potentially long wrapped text

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;

public class ProductDetailScreenController implements ProductStateManager.ProductStateListener {

    @FXML
    private BorderPane productDetailPane;
    @FXML
    private HBox productDetailContentBox;
    @FXML
    private Label productTitleLabel;
    @FXML
    private ImageView productImageView;
    @FXML
    private Label productPriceLabel;
    @FXML
    private Label productCategoryLabel;
    @FXML
    private Label productAvailabilityLabel;
    @FXML
    private TextArea productDescriptionArea;
    @FXML
    private GridPane productSpecificsGrid;
    @FXML
    private Spinner<Integer> quantitySpinner;
    @FXML
    private Button addToCartButton;
    @FXML
    private Label errorMessageLabel;

    // @Inject
    private IProductService productService;
    // @Inject
    private ICartService cartService;
    private MainLayoutController mainLayoutController; // For navigation and setting header
    private com.aims.core.presentation.utils.FXMLSceneManager sceneManager;

    private Product currentProduct;
    private String productIdToLoad;
    private boolean isStateListenerRegistered = false; // Track listener registration

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMMM yyyy");

    public ProductDetailScreenController() {
        // productService = new ProductServiceImpl(...); // DI
        // cartService = new CartServiceImpl(...);    // DI
    }

    public void setMainLayoutController(MainLayoutController mainLayoutController) { 
        this.mainLayoutController = mainLayoutController; 
        System.out.println("ProductDetailScreenController.setMainLayoutController: MainLayoutController injected successfully");
    }
    
    public void setSceneManager(com.aims.core.presentation.utils.FXMLSceneManager sceneManager) { 
        this.sceneManager = sceneManager; 
        System.out.println("ProductDetailScreenController.setSceneManager: SceneManager injected successfully");
    }
    
    public void setProductService(IProductService productService) { 
        this.productService = productService; 
        System.out.println("ProductDetailScreenController.setProductService: ProductService injected successfully - Available: " + (productService != null));
    }
    
    public void setCartService(ICartService cartService) { 
        this.cartService = cartService; 
        System.out.println("ProductDetailScreenController.setCartService: CartService injected successfully - Available: " + (cartService != null));
    }

    public void initialize() {
        System.out.println("ProductDetailScreenController.initialize: Starting enhanced controller initialization");
        
        // Check if FXML components are injected
        System.out.println("ProductDetailScreenController.initialize: productDetailPane = " + (productDetailPane != null ? "INJECTED" : "NULL"));
        System.out.println("ProductDetailScreenController.initialize: productDetailContentBox = " + (productDetailContentBox != null ? "INJECTED" : "NULL"));
        System.out.println("ProductDetailScreenController.initialize: productTitleLabel = " + (productTitleLabel != null ? "INJECTED" : "NULL"));
        System.out.println("ProductDetailScreenController.initialize: productImageView = " + (productImageView != null ? "INJECTED" : "NULL"));
        System.out.println("ProductDetailScreenController.initialize: productPriceLabel = " + (productPriceLabel != null ? "INJECTED" : "NULL"));
        System.out.println("ProductDetailScreenController.initialize: productCategoryLabel = " + (productCategoryLabel != null ? "INJECTED" : "NULL"));
        System.out.println("ProductDetailScreenController.initialize: productDescriptionArea = " + (productDescriptionArea != null ? "INJECTED" : "NULL"));
        System.out.println("ProductDetailScreenController.initialize: productSpecificsGrid = " + (productSpecificsGrid != null ? "INJECTED" : "NULL"));
        System.out.println("ProductDetailScreenController.initialize: quantitySpinner = " + (quantitySpinner != null ? "INJECTED" : "NULL"));
        
        // Enhanced text area configuration
        if (productDescriptionArea != null) {
            productDescriptionArea.setWrapText(true);
            productDescriptionArea.setEditable(false);
            productDescriptionArea.setFocusTraversable(false);
        }
        
        // Enhanced spinner configuration
        if (quantitySpinner != null) {
            SpinnerValueFactory<Integer> valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100, 1);
            quantitySpinner.setValueFactory(valueFactory);
            quantitySpinner.setEditable(true);
        }
        
        // Apply responsive layout setup
        setupResponsiveLayout();
        
        // Validate and initialize services if needed
        validateAndInitializeServices();
        
        setErrorMessage("", false);
        System.out.println("ProductDetailScreenController.initialize: Enhanced controller initialization completed");
    }

    // ProductStateManager.ProductStateListener implementation
    @Override
    public String getInterestedProductId() {
        return currentProduct != null ? currentProduct.getProductId() : null;
    }

    @Override
    public void onProductUpdated(Product updatedProduct) {
        if (currentProduct != null &&
            currentProduct.getProductId().equals(updatedProduct.getProductId())) {
            
            System.out.println("ProductDetail: Received state update for " + updatedProduct.getTitle() +
                             " - Stock: " + updatedProduct.getQuantityInStock() +
                             " (was: " + currentProduct.getQuantityInStock() + ")");
            
            // Update current product and refresh UI on JavaFX Application Thread
            javafx.application.Platform.runLater(() -> {
                currentProduct = updatedProduct;
                populateProductData();
            });
        }
    }
    
    /**
     * Setup responsive layout behavior for the product detail screen
     */
    private void setupResponsiveLayout() {
        if (productDetailPane != null) {
            // Apply initial responsive classes
            if (!productDetailPane.getStyleClass().contains("responsive-product-detail-container")) {
                productDetailPane.getStyleClass().add("responsive-product-detail-container");
            }
            
            // Setup responsive content box behavior
            if (productDetailContentBox != null) {
                // Add window resize listener for responsive layout adjustments
                javafx.application.Platform.runLater(() -> {
                    if (productDetailPane.getScene() != null) {
                        productDetailPane.getScene().widthProperty().addListener((observable, oldValue, newValue) -> {
                            applyResponsiveLayout(newValue.doubleValue());
                        });
                        
                        // Apply initial responsive layout
                        applyResponsiveLayout(productDetailPane.getScene().getWidth());
                    }
                });
            }
        }
    }
    
    /**
     * Apply responsive layout based on window width
     */
    private void applyResponsiveLayout(double width) {
        if (productDetailContentBox != null) {
            // Remove existing responsive classes
            productDetailContentBox.getStyleClass().removeIf(styleClass ->
                styleClass.startsWith("responsive-mobile") || styleClass.startsWith("responsive-desktop"));
            
            // Apply responsive classes based on screen size
            if (width < 1024) {
                // Mobile/Tablet layout
                productDetailContentBox.getStyleClass().add("responsive-mobile-layout");
                System.out.println("ProductDetailScreenController: Applied mobile layout classes");
            } else if (width < 1366) {
                // Compact desktop layout
                productDetailContentBox.getStyleClass().add("responsive-desktop-compact-layout");
                System.out.println("ProductDetailScreenController: Applied compact desktop layout classes");
            } else {
                // Standard desktop layout
                productDetailContentBox.getStyleClass().add("responsive-desktop-standard-layout");
                System.out.println("ProductDetailScreenController: Applied standard desktop layout classes");
            }
        }
    }

    /**
     * Validates that required services are available and attempts to initialize them if needed.
     * This provides a fallback mechanism when dependency injection fails.
     */
    private void validateAndInitializeServices() {
        if (productService == null) {
            System.err.println("ProductService is null - attempting to initialize from ServiceFactory");
            try {
                ServiceFactory serviceFactory = ServiceFactory.getInstance();
                this.productService = serviceFactory.getProductService();
                System.out.println("ProductService initialized from ServiceFactory: " + (productService != null));
            } catch (Exception e) {
                System.err.println("Failed to initialize ProductService: " + e.getMessage());
                displayError("Product service unavailable. Please refresh the page.");
            }
        }
        
        if (cartService == null) {
            System.err.println("CartService is null - attempting to initialize from ServiceFactory");
            try {
                ServiceFactory serviceFactory = ServiceFactory.getInstance();
                this.cartService = serviceFactory.getCartService();
                System.out.println("CartService initialized from ServiceFactory: " + (cartService != null));
            } catch (Exception e) {
                System.err.println("Failed to initialize CartService: " + e.getMessage());
                // Cart service failure is not critical for viewing product details
            }
        }
    }

    /**
     * Sets the ID of the product to be displayed and loads its details.
     * This method should be called when navigating to this screen.
     * @param productId The ID of the product to display.
     */
    public void setProductId(String productId) {
        System.out.println("ProductDetailScreenController.setProductId: Called with productId: " + productId);
        System.out.println("ProductDetailScreenController.setProductId: ProductService available: " + (productService != null));
        System.out.println("ProductDetailScreenController.setProductId: CartService available: " + (cartService != null));
        System.out.println("ProductDetailScreenController.setProductId: MainLayoutController available: " + (mainLayoutController != null));
        
        this.productIdToLoad = productId;
        if (productIdToLoad != null) {
            // Validate and initialize services before loading product details
            if (productService == null) {
                System.out.println("ProductDetailScreenController.setProductId: ProductService is null, attempting to initialize");
                validateAndInitializeServices();
                
                // If services are still not available, defer the loading
                if (productService == null) {
                    System.out.println("ProductDetailScreenController.setProductId: Services not ready, deferring product loading");
                    javafx.application.Platform.runLater(() -> {
                        validateAndInitializeServices();
                        if (productService != null) {
                            System.out.println("ProductDetailScreenController.setProductId: Services now available, loading product details");
                            loadProductDetails();
                        } else {
                            System.err.println("ProductDetailScreenController.setProductId: Failed to initialize services after deferral");
                            displayError("Product service is temporarily unavailable. Please refresh the page.");
                        }
                    });
                    return;
                }
            }
            
            System.out.println("ProductDetailScreenController.setProductId: About to call loadProductDetails()");
            loadProductDetails();
        } else {
            System.out.println("ProductDetailScreenController.setProductId: Product ID is null, displaying error");
            displayError("Product ID not provided.");
        }
    }

    private void loadProductDetails() {
        System.out.println("ProductDetailScreenController.loadProductDetails: Starting for product ID: " + productIdToLoad);
        
        // Validate prerequisites
        if (productIdToLoad == null || productIdToLoad.trim().isEmpty()) {
            displayError("Invalid product identifier.");
            return;
        }
        
        if (productService == null) {
            System.err.println("ProductService is null - attempting recovery");
            validateAndInitializeServices();
            
            if (productService == null) {
                displayError("Product service is temporarily unavailable. Please try again later.");
                return;
            }
        }
        
        try {
            // Show loading state
            productTitleLabel.setText("Loading product details...");
            addToCartButton.setDisable(true);
            
            System.out.println("ProductDetailScreenController.loadProductDetails: Calling productService.getProductDetailsForCustomer()");
            // ProductService.getProductDetailsForCustomer should return product with VAT-inclusive price
            currentProduct = productService.getProductDetailsForCustomer(productIdToLoad);
            
            if (currentProduct != null) {
                System.out.println("ProductDetailScreenController.loadProductDetails: Product loaded successfully: " + currentProduct.getTitle());
                
                // CRITICAL FIX: Register for state updates and update state cache
                if (!isStateListenerRegistered) {
                    ProductStateManager.addListener(this);
                    isStateListenerRegistered = true;
                    System.out.println("ProductDetailScreenController.loadProductDetails: Registered with ProductStateManager");
                }
                
                // Update state cache with current product
                ProductStateManager.updateProduct(currentProduct);
                
                populateProductData();
                
                // Update header
                if (mainLayoutController != null) {
                    System.out.println("ProductDetailScreenController.loadProductDetails: Setting header title to: " + currentProduct.getTitle());
                    mainLayoutController.setHeaderTitle("Product Details: " + currentProduct.getTitle());
                } else {
                    System.out.println("ProductDetailScreenController.loadProductDetails: MainLayoutController is null, cannot set header");
                }
            } else {
                displayError("Product not found. It may have been removed or is temporarily unavailable.");
            }
            
        } catch (SQLException e) {
            System.err.println("Database error loading product: " + e.getMessage());
            displayError("Database connection error. Please check your connection and try again.");
        } catch (Exception e) {
            System.err.println("Unexpected error loading product: " + e.getMessage());
            e.printStackTrace();
            displayError("An unexpected error occurred. Please try again or contact support.");
        }
    }

    private void populateProductData() {
        System.out.println("ProductDetailScreenController.populateProductData: Starting to populate UI with product data");
        System.out.println("ProductDetailScreenController.populateProductData: Product title: " + (currentProduct != null ? currentProduct.getTitle() : "NULL"));
        
        if (currentProduct == null) {
            System.err.println("ProductDetailScreenController.populateProductData: currentProduct is null, cannot populate data");
            return;
        }
        
        try {
            System.out.println("ProductDetailScreenController.populateProductData: Setting product title label");
            productTitleLabel.setText(currentProduct.getTitle());
            
            System.out.println("ProductDetailScreenController.populateProductData: Setting product price label");
            productPriceLabel.setText(String.format("%,.0f VND", currentProduct.getPrice())); // Price already includes VAT from service
            
            System.out.println("ProductDetailScreenController.populateProductData: Setting product category label");
            productCategoryLabel.setText(currentProduct.getCategory() != null ? currentProduct.getCategory() : "N/A");
            
            System.out.println("ProductDetailScreenController.populateProductData: Setting product description");
            productDescriptionArea.setText(currentProduct.getDescription() != null ? currentProduct.getDescription() : "No description available.");

            System.out.println("ProductDetailScreenController.populateProductData: Setting availability info with responsive styling");
            if (currentProduct.getQuantityInStock() > 0) {
                productAvailabilityLabel.setText("Available: " + currentProduct.getQuantityInStock());
                // Apply responsive CSS class for in-stock status
                productAvailabilityLabel.getStyleClass().removeAll("out-of-stock");
                if (!productAvailabilityLabel.getStyleClass().contains("in-stock")) {
                    productAvailabilityLabel.getStyleClass().add("in-stock");
                }
                quantitySpinner.setDisable(false);
                addToCartButton.setDisable(false);
                ((SpinnerValueFactory.IntegerSpinnerValueFactory) quantitySpinner.getValueFactory())
                        .setMax(Math.min(100, currentProduct.getQuantityInStock())); // Limit spinner max to stock or 100
            } else {
                productAvailabilityLabel.setText("Out of Stock");
                // Apply responsive CSS class for out-of-stock status
                productAvailabilityLabel.getStyleClass().removeAll("in-stock");
                if (!productAvailabilityLabel.getStyleClass().contains("out-of-stock")) {
                    productAvailabilityLabel.getStyleClass().add("out-of-stock");
                }
                quantitySpinner.setDisable(true);
                addToCartButton.setDisable(true);
            }

            System.out.println("ProductDetailScreenController.populateProductData: Setting product image");
            if (currentProduct.getImageUrl() != null && !currentProduct.getImageUrl().isEmpty()) {
                try {
                    Image image = new Image(currentProduct.getImageUrl(), true); // true for background loading
                    productImageView.setImage(image);
                    System.out.println("ProductDetailScreenController.populateProductData: Product image set successfully");
                } catch (Exception e) {
                    System.err.println("Error loading product image for detail view: " + currentProduct.getImageUrl() + " - " + e.getMessage());
                    // Load placeholder image
                }
            } else {
                System.out.println("ProductDetailScreenController.populateProductData: No image URL available, skipping image");
                // Load placeholder image
            }
            
            System.out.println("ProductDetailScreenController.populateProductData: Calling populateSpecificDetails()");
            populateSpecificDetails();
            
            System.out.println("ProductDetailScreenController.populateProductData: Successfully completed populating product data");
        } catch (Exception e) {
            System.err.println("ProductDetailScreenController.populateProductData: Error populating UI: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void populateSpecificDetails() {
        productSpecificsGrid.getChildren().clear(); // Clear previous details
        int rowIndex = 0;
        // Common details not already displayed prominently
        addDetailToGrid("Product ID:", currentProduct.getProductId(), rowIndex++);
        addDetailToGrid("Barcode:", currentProduct.getBarcode(), rowIndex++);
        addDetailToGrid("Dimensions (cm):", currentProduct.getDimensionsCm(), rowIndex++);
        addDetailToGrid("Weight (kg):", String.format("%.2f", currentProduct.getWeightKg()), rowIndex++);
        addDetailToGrid("Entry Date:", currentProduct.getEntryDate() != null ? currentProduct.getEntryDate().format(DATE_FORMATTER) : "N/A", rowIndex++);


        if (currentProduct instanceof Book book) {
            addDetailToGrid("Authors:", book.getAuthors(), rowIndex++);
            addDetailToGrid("Publisher:", book.getPublisher(), rowIndex++);
            addDetailToGrid("Publication Date:", book.getPublicationDate() != null ? book.getPublicationDate().format(DATE_FORMATTER) : "N/A", rowIndex++);
            addDetailToGrid("Pages:", book.getNumPages() > 0 ? String.valueOf(book.getNumPages()) : "N/A", rowIndex++);
            addDetailToGrid("Cover Type:", book.getCoverType(), rowIndex++);
            addDetailToGrid("Language:", book.getLanguage(), rowIndex++);
            addDetailToGrid("Genre (Book):", book.getBookGenre(), rowIndex++);
        } else if (currentProduct instanceof CD cd) {
            addDetailToGrid("Artists:", cd.getArtists(), rowIndex++);
            addDetailToGrid("Record Label:", cd.getRecordLabel(), rowIndex++);
            addDetailToGrid("Genre (CD):", cd.getCdGenre(), rowIndex++);
            addDetailToGrid("Release Date:", cd.getReleaseDate() != null ? cd.getReleaseDate().format(DATE_FORMATTER) : "N/A", rowIndex++);
            addDetailToGrid("Tracklist:", cd.getTracklist(), rowIndex++, true); // Use Text for potential long content
        } else if (currentProduct instanceof DVD dvd) {
            addDetailToGrid("Disc Type:", dvd.getDiscType(), rowIndex++);
            addDetailToGrid("Director:", dvd.getDirector(), rowIndex++);
            addDetailToGrid("Runtime:", dvd.getRuntimeMinutes() > 0 ? dvd.getRuntimeMinutes() + " mins" : "N/A", rowIndex++);
            addDetailToGrid("Studio:", dvd.getStudio(), rowIndex++);
            addDetailToGrid("Language (DVD):", dvd.getDvdLanguage(), rowIndex++);
            addDetailToGrid("Subtitles:", dvd.getSubtitles(), rowIndex++);
            addDetailToGrid("Release Date (DVD):", dvd.getDvdReleaseDate() != null ? dvd.getDvdReleaseDate().format(DATE_FORMATTER) : "N/A", rowIndex++);
            addDetailToGrid("Genre (DVD):", dvd.getDvdGenre(), rowIndex++);
        }
    }

    private void addDetailToGrid(String labelText, String valueText, int rowIndex) {
        addDetailToGrid(labelText, valueText, rowIndex, false);
    }
    
    private void addDetailToGrid(String labelText, String valueText, int rowIndex, boolean useTextNodeForValue) {
        // Create label with responsive styling
        Label label = new Label(labelText);
        label.getStyleClass().add("responsive-spec-label");
        productSpecificsGrid.add(label, 0, rowIndex);

        if (useTextNodeForValue) {
            // Use Text node for potentially long content
            Text valueNode = new Text(valueText != null ? valueText : "N/A");
            valueNode.getStyleClass().add("responsive-spec-value");
            valueNode.setWrappingWidth(productSpecificsGrid.getColumnConstraints().get(1).getPrefWidth() > 0 ?
                                      productSpecificsGrid.getColumnConstraints().get(1).getPrefWidth() - 10 : 300);
            productSpecificsGrid.add(valueNode, 1, rowIndex);
        } else {
            // Use Label for regular content
            Label valueNode = new Label(valueText != null ? valueText : "N/A");
            valueNode.getStyleClass().add("responsive-spec-value");
            valueNode.setWrapText(true);
            productSpecificsGrid.add(valueNode, 1, rowIndex);
        }
    }

    @FXML
    void handleAddToCartAction(ActionEvent event) {
        if (currentProduct == null) {
            System.err.println("ProductDetailScreenController.handleAddToCartAction: currentProduct is null");
            return;
        }
        
        int quantity = quantitySpinner.getValue();
        System.out.println("ProductDetailScreenController.handleAddToCartAction: Add to cart clicked for: " + currentProduct.getTitle() + ", Quantity: " + quantity);
        
        // Validate quantity
        if (quantity <= 0) {
            setErrorMessage("Quantity must be greater than 0.", true);
            return;
        }
        
        // Check if quantity exceeds available stock
        if (quantity > currentProduct.getQuantityInStock()) {
            setErrorMessage("Requested quantity (" + quantity + ") exceeds available stock (" + currentProduct.getQuantityInStock() + ").", true);
            return;
        }
        
        // Clear any previous error messages
        setErrorMessage("", false);
        
        // Validate and initialize services if needed
        validateAndInitializeServices();
        
        // Check service availability
        if (cartService == null) {
            setErrorMessage("Cart service is not available. Please refresh the page and try again.", true);
            System.err.println("ProductDetailScreenController.handleAddToCartAction: CartService is null after validation");
            return;
        }
        
        // Disable button temporarily to prevent double-clicks
        addToCartButton.setDisable(true);
        String originalButtonText = addToCartButton.getText();
        addToCartButton.setText("Adding...");
        
        try {
            // Get or create cart session ID with proper persistence
            String currentCartSessionId = CartSessionManager.getOrCreateCartSessionId();
            System.out.println("ProductDetailScreenController.handleAddToCartAction: Using cart session ID: " + currentCartSessionId);
            
            // Add item to cart
            cartService.addItemToCart(currentCartSessionId, currentProduct.getProductId(), quantity);
            
            // Success feedback
            System.out.println("ProductDetailScreenController.handleAddToCartAction: Successfully added " + quantity + " of '" + currentProduct.getTitle() + "' to cart");
            setErrorMessage("Added " + quantity + " of '" + currentProduct.getTitle() + "' to your cart.", false);
            
            // CRITICAL FIX: Refresh product details to show updated stock from database
            refreshProductDetailsFromDatabase();
            
        } catch (com.aims.core.shared.exceptions.ValidationException e) {
            System.err.println("ProductDetailScreenController.handleAddToCartAction: Validation error: " + e.getMessage());
            setErrorMessage("Invalid request: " + e.getMessage(), true);
        } catch (com.aims.core.shared.exceptions.ResourceNotFoundException e) {
            System.err.println("ProductDetailScreenController.handleAddToCartAction: Product not found: " + e.getMessage());
            setErrorMessage("Product is no longer available.", true);
        } catch (com.aims.core.shared.exceptions.InventoryException e) {
            System.err.println("ProductDetailScreenController.handleAddToCartAction: Inventory error: " + e.getMessage());
            setErrorMessage("Insufficient stock: " + e.getMessage(), true);
        } catch (SQLException e) {
            System.err.println("ProductDetailScreenController.handleAddToCartAction: Database error: " + e.getMessage());
            setErrorMessage("Database error occurred. Please try again.", true);
        } catch (Exception e) {
            System.err.println("ProductDetailScreenController.handleAddToCartAction: Unexpected error: " + e.getMessage());
            e.printStackTrace();
            setErrorMessage("An unexpected error occurred. Please try again.", true);
        } finally {
            // Re-enable button
            addToCartButton.setDisable(false);
            addToCartButton.setText(originalButtonText);
        }
    }

    /**
     * Refreshes current product details from database to ensure stock consistency
     */
    private void refreshProductDetailsFromDatabase() {
        if (productIdToLoad == null || productIdToLoad.trim().isEmpty()) {
            System.err.println("ProductDetailScreenController.refreshProductDetailsFromDatabase: No product ID to refresh");
            return;
        }
        
        // Validate and initialize services if needed
        validateAndInitializeServices();
        
        if (productService == null) {
            System.err.println("ProductDetailScreenController.refreshProductDetailsFromDatabase: ProductService unavailable");
            return;
        }
        
        try {
            // Fetch fresh product data from database
            Product refreshedProduct = productService.getProductDetailsForCustomer(productIdToLoad);
            if (refreshedProduct != null) {
                System.out.println("ProductDetailScreenController.refreshProductDetailsFromDatabase: Refreshed " +
                                 refreshedProduct.getTitle() + " - Stock: " + refreshedProduct.getQuantityInStock());
                
                // Update current product and refresh UI
                currentProduct = refreshedProduct;
                populateProductData();
            } else {
                System.err.println("ProductDetailScreenController.refreshProductDetailsFromDatabase: Product not found: " + productIdToLoad);
            }
        } catch (SQLException e) {
            System.err.println("ProductDetailScreenController.refreshProductDetailsFromDatabase: Database error: " + e.getMessage());
            // Continue with existing data on error
        } catch (Exception e) {
            System.err.println("ProductDetailScreenController.refreshProductDetailsFromDatabase: Unexpected error: " + e.getMessage());
            // Continue with existing data on error
        }
    }

    @FXML
    void handleBackToListingAction(ActionEvent event) {
        System.out.println("Back to Product Listing action triggered");
        
        boolean navigationSuccess = false;
        
        // First, try to use navigation history for smart back navigation
        if (sceneManager != null) {
            try {
                System.out.println("ProductDetailScreenController.handleBackToListingAction: Attempting smart back navigation");
                navigationSuccess = sceneManager.navigateBack();
                if (navigationSuccess) {
                    System.out.println("ProductDetailScreenController.handleBackToListingAction: Successfully navigated back using history");
                    return;
                }
            } catch (Exception e) {
                System.err.println("ProductDetailScreenController.handleBackToListingAction: Error during smart back navigation: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("ProductDetailScreenController.handleBackToListingAction: SceneManager is null, cannot use smart navigation");
        }
        
        // Fallback to home screen navigation
        if (mainLayoutController != null) {
            try {
                System.out.println("ProductDetailScreenController.handleBackToListingAction: Using fallback navigation to home screen");
                Object controller = mainLayoutController.loadContent(FXMLPaths.HOME_SCREEN);
                mainLayoutController.setHeaderTitle("AIMS - Product Catalog");
                navigationSuccess = true;
                System.out.println("ProductDetailScreenController.handleBackToListingAction: Successfully navigated to home screen");
            } catch (Exception e) {
                System.err.println("ProductDetailScreenController.handleBackToListingAction: Error during fallback navigation: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.err.println("ProductDetailScreenController.handleBackToListingAction: MainLayoutController is null, cannot navigate");
        }
        
        // Final fallback - log error if all navigation attempts failed
        if (!navigationSuccess) {
            System.err.println("ProductDetailScreenController.handleBackToListingAction: All navigation attempts failed - unable to navigate back");
            if (errorMessageLabel != null) {
                setErrorMessage("Navigation error - please refresh the page", true);
            }
        }
    }

    /**
     * Cleanup method to remove state listener when controller is destroyed.
     * This prevents memory leaks and should be called when the controller is no longer needed.
     */
    public void cleanup() {
        if (isStateListenerRegistered) {
            ProductStateManager.removeListener(this);
            isStateListenerRegistered = false;
            System.out.println("ProductDetailScreenController.cleanup: Removed listener from ProductStateManager");
        }
    }
    private void displayError(String message) {
        System.err.println("ProductDetailScreenController Error: " + message);
        
        // Show error in title with visual indication
        productTitleLabel.setText("Unable to Load Product Details");
        productTitleLabel.setStyle("-fx-text-fill: red;");
        
        // Clear other fields
        productImageView.setImage(null);
        productPriceLabel.setText("");
        productCategoryLabel.setText("");
        productAvailabilityLabel.setText("");
        productDescriptionArea.setText("Error: " + message + "\n\nPlease try refreshing the page or contact support if the issue persists.");
        productSpecificsGrid.getChildren().clear();
        
        // Disable controls
        quantitySpinner.setDisable(true);
        addToCartButton.setDisable(true);
        
        // Show error message in the error label
        setErrorMessage(message, true);
    }

    private void setErrorMessage(String message, boolean visible) {
        errorMessageLabel.setText(message);
        errorMessageLabel.setVisible(visible);
        errorMessageLabel.setManaged(visible);
        
        // Remove existing message styling classes
        errorMessageLabel.getStyleClass().removeAll("success", "error");
        
        // Apply responsive styling based on message type
        if (visible) {
            if (message.toLowerCase().contains("added") || message.toLowerCase().contains("success")) {
                errorMessageLabel.getStyleClass().add("success");
            } else {
                errorMessageLabel.getStyleClass().add("error");
            }
        }
    }

}