package com.aims.core.presentation.controllers;

import com.aims.core.application.services.IProductService;
import com.aims.core.application.services.ICartService;
import com.aims.core.entities.Product;
import com.aims.core.shared.utils.SearchResult;
// import com.aims.presentation.utils.AlertHelper;
// import com.aims.presentation.utils.FXMLSceneManager; // For navigation
// import com.aims.MainLayoutController; // If this controller needs to interact with the main layout

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.geometry.Pos;
import javafx.scene.text.TextAlignment;
import javafx.concurrent.Task;
import javafx.application.Platform;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List; // For example category list

public class HomeScreenController implements MainLayoutController.IChildController { // Implement IChildController

    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> categoryComboBox;
    @FXML
    private ComboBox<String> sortByPriceComboBox;
    @FXML
    private ScrollPane scrollPane;
    @FXML
    private FlowPane productFlowPane;
    @FXML
    private HBox paginationControls;
    @FXML
    private Button prevPageButton;
    @FXML
    private Label currentPageLabel;
    @FXML
    private Button nextPageButton;

    // --- Service Dependencies (to be injected or set) ---
    private IProductService productService;
    private ICartService cartService;
    private MainLayoutController mainLayoutController;
    // private FXMLSceneManager sceneManager;

    private int currentPage = 1;
    private final int PAGE_SIZE = 20; // As per requirement for home/search
    private int totalPages = 1;

    // Current filter/search state
    private String currentSearchTerm = "";
    private String currentCategoryFilter = null; // null means all categories
    private String currentSortBy = null; // null for default, "ASC", "DESC"

    public HomeScreenController() {
        // Constructor for FXML loading. Services should be injected.
        // Example (replace with actual DI):
        // productService = ServiceFactory.getProductService();
        // cartService = ServiceFactory.getCartService();
    }

    @Override
    public void setMainLayoutController(MainLayoutController mainLayoutController) {
        this.mainLayoutController = mainLayoutController;
        // this.sceneManager = mainLayoutController.getSceneManager(); // If MainLayout exposes SceneManager
    }

    // Method for DI if not using a framework
    public void setProductService(IProductService productService) {
        this.productService = productService;
    }
    public void setCartService(ICartService cartService) {
        this.cartService = cartService;
    }


    public void initialize() {
        sortByPriceComboBox.setItems(FXCollections.observableArrayList("Default Sort", "Price: Low to High", "Price: High to Low"));
        sortByPriceComboBox.setValue("Default Sort");

        // Set fallback categories initially
        categoryComboBox.setItems(FXCollections.observableArrayList("All Categories", "Book", "CD", "DVD"));
        categoryComboBox.setValue("All Categories");

        // Add listeners to automatically search/filter when combo box values change
        categoryComboBox.setOnAction(event -> handleFilterOrSortChange());
        sortByPriceComboBox.setOnAction(event -> handleFilterOrSortChange());

        // Debug layout - Check sizes after UI is rendered
        javafx.application.Platform.runLater(() -> {
            if (productFlowPane != null && productFlowPane.getScene() != null) {
                System.out.println("HomeScreen Scene size: " + 
                                 productFlowPane.getScene().getWidth() + "x" + 
                                 productFlowPane.getScene().getHeight());
                System.out.println("ProductFlowPane size: " + 
                                 productFlowPane.getWidth() + "x" + 
                                 productFlowPane.getHeight());
                System.out.println("ScrollPane size: " + 
                                 scrollPane.getWidth() + "x" + 
                                 scrollPane.getHeight());
            }
        });

        // Don't load products here - will be loaded after services are injected
    }
    
    /**
     * Called after services are injected to complete initialization
     */
    public void completeInitialization() {
        // Load categories dynamically from productService
        try {
            if (productService != null) {
                List<String> categories = productService.getAllCategories();
                categories.add(0, "All Categories"); // Add option for no filter
                categoryComboBox.setItems(FXCollections.observableArrayList(categories));
                categoryComboBox.setValue("All Categories");
            }
        } catch (SQLException e) {
            // Keep fallback categories if service fails
            System.err.println("Error loading categories: " + e.getMessage());
        }

        loadProducts();
    }

    private void handleFilterOrSortChange() {
        currentPage = 1; // Reset to first page when filters change
        currentSearchTerm = searchField.getText().trim(); // Keep current search term
        currentCategoryFilter = "All Categories".equals(categoryComboBox.getValue()) ? null : categoryComboBox.getValue();
        
        String selectedSort = sortByPriceComboBox.getValue();
        if ("Price: Low to High".equals(selectedSort)) {
            currentSortBy = "ASC";
        } else if ("Price: High to Low".equals(selectedSort)) {
            currentSortBy = "DESC";
        } else {
            currentSortBy = null; // Default sort
        }
        loadProducts();
    }

    private void loadProducts() {
        System.out.println("HomeScreenController.loadProducts: Starting with page=" + currentPage);
        
        if (productService == null) {
            System.err.println("ProductService is null - attempting recovery");
            validateAndInitializeServices();
            if (productService == null) {
                showError("Product service unavailable. Please refresh the page.");
                return;
            }
        }
        
        try {
            // Clear existing products and show loading
            Platform.runLater(() -> {
                productFlowPane.getChildren().clear();
                showLoadingIndicator();
            });
            
            // Load products asynchronously
            Task<SearchResult<Product>> loadTask = new Task<SearchResult<Product>>() {
                @Override
                protected SearchResult<Product> call() throws Exception {
                    // Use the advanced search method for comprehensive functionality
                    String keyword = currentSearchTerm.trim().isEmpty() ? null : currentSearchTerm;
                    String category = "All Categories".equals(currentCategoryFilter) ? null : currentCategoryFilter;
                    
                    // Convert sort selection to service parameters
                    String sortBy = null;
                    String sortOrder = null;
                    if (currentSortBy != null) {
                        switch (currentSortBy) {
                            case "ASC":
                                sortBy = "price";
                                sortOrder = "ASC";
                                break;
                            case "DESC":
                                sortBy = "price";
                                sortOrder = "DESC";
                                break;
                            default:
                                sortBy = "title";
                                sortOrder = "ASC";
                                break;
                        }
                    } else {
                        sortBy = "title";
                        sortOrder = "ASC";
                    }
                    
                    return productService.advancedSearchProducts(
                        keyword, category, sortBy, sortOrder, currentPage, PAGE_SIZE);
                }
                
                @Override
                protected void succeeded() {
                    Platform.runLater(() -> {
                        SearchResult<Product> result = getValue();
                        hideLoadingIndicator();
                        populateProductCards(result.results());
                        totalPages = result.totalPages();
                        updatePaginationControls(currentPage, totalPages, (int) result.totalResults());
                    });
                }
                
                @Override
                protected void failed() {
                    Platform.runLater(() -> {
                        hideLoadingIndicator();
                        System.err.println("Task failed: " + getException().getMessage());
                        getException().printStackTrace();
                        // Fallback to direct database access
                        loadProductsDirectly();
                    });
                }
            };
            
            new Thread(loadTask).start();
            
        } catch (Exception e) {
            System.err.println("Error in loadProducts: " + e.getMessage());
            e.printStackTrace();
            showError("Error loading products. Please try again.");
        }
    }
    
    private void validateAndInitializeServices() {
        // Attempt to recover services if they're null
        try {
            if (productService == null) {
                // Attempt to get service from ServiceFactory or dependency injection
                System.err.println("Attempting to recover ProductService...");
                com.aims.core.shared.ServiceFactory serviceFactory = com.aims.core.shared.ServiceFactory.getInstance();
                if (serviceFactory != null) {
                    productService = serviceFactory.getProductService();
                    System.out.println("ProductService recovered successfully: " + (productService != null));
                } else {
                    System.err.println("ServiceFactory is null, cannot recover ProductService");
                }
            }
            
            if (cartService == null) {
                System.err.println("Attempting to recover CartService...");
                com.aims.core.shared.ServiceFactory serviceFactory = com.aims.core.shared.ServiceFactory.getInstance();
                if (serviceFactory != null) {
                    cartService = serviceFactory.getCartService();
                    System.out.println("CartService recovered successfully: " + (cartService != null));
                } else {
                    System.err.println("ServiceFactory is null, cannot recover CartService");
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to recover services: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void populateProductCards(List<Product> products) {
        System.out.println("HomeScreenController.populateProductCards: Loading " + products.size() + " products");
        
        if (products.isEmpty()) {
            showEmptyState();
            return;
        }
        
        for (Product product : products) {
            try {
                // Load product card FXML
                FXMLLoader cardLoader = new FXMLLoader(getClass().getResource("/com/aims/presentation/views/partials/product_card.fxml"));
                Parent cardNode = cardLoader.load();
                
                // Get controller and set data
                ProductCardController cardController = cardLoader.getController();
                cardController.setData(product);
                
                if (cartService != null) {
                    cardController.setCartService(cartService);
                }
                if (mainLayoutController != null) {
                    cardController.setMainLayoutController(mainLayoutController);
                }
                
                // Apply dynamic sizing based on screen dimensions
                applyDynamicCardSizing(cardNode);
                
                // Add to flow pane
                productFlowPane.getChildren().add(cardNode);
                
            } catch (Exception e) {
                System.err.println("Error loading product card for: " + product.getTitle() + " - " + e.getMessage());
            }
        }
        
        // Apply responsive layout after all cards are loaded
        javafx.application.Platform.runLater(() -> {
            if (productFlowPane != null && productFlowPane.getScene() != null) {
                updateResponsiveLayout(productFlowPane.getScene().getWidth(), productFlowPane.getScene().getHeight());
            }
        });
        
        System.out.println("HomeScreenController.populateProductCards: Successfully loaded " + productFlowPane.getChildren().size() + " product cards");
    }
    
    // Add visual feedback methods
    private void showLoadingIndicator() {
        Label loadingLabel = new Label("Loading products...");
        loadingLabel.getStyleClass().add("loading-indicator");
        productFlowPane.getChildren().clear();
        productFlowPane.getChildren().add(loadingLabel);
    }
    
    private void hideLoadingIndicator() {
        // Loading indicator will be replaced by actual content
    }
    
    private void showEmptyState() {
        VBox emptyState = new VBox(20);
        emptyState.setAlignment(Pos.CENTER);
        emptyState.getStyleClass().add("empty-state");
        
        Label emptyLabel = new Label("No products found");
        emptyLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: #7f8c8d;");
        
        Label emptyDescription = new Label("Try adjusting your search criteria or browse all categories");
        emptyDescription.setStyle("-fx-font-size: 14px; -fx-text-fill: #95a5a6;");
        
        javafx.scene.control.Button refreshButton = new javafx.scene.control.Button("Refresh");
        refreshButton.getStyleClass().add("primary-button");
        refreshButton.setOnAction(e -> loadProducts());
        
        emptyState.getChildren().addAll(emptyLabel, emptyDescription, refreshButton);
        productFlowPane.getChildren().clear();
        productFlowPane.getChildren().add(emptyState);
    }
    
    private void showError(String message) {
        VBox errorState = new VBox(15);
        errorState.setAlignment(Pos.CENTER);
        errorState.getStyleClass().add("error-state");
        
        Label errorLabel = new Label("Error Loading Products");
        errorLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: #e74c3c; -fx-font-weight: bold;");
        
        Label errorDescription = new Label(message);
        errorDescription.setStyle("-fx-font-size: 14px; -fx-text-fill: #c0392b; -fx-wrap-text: true; -fx-max-width: 400;");
        errorDescription.setTextAlignment(TextAlignment.CENTER);
        
        javafx.scene.control.Button retryButton = new javafx.scene.control.Button("Try Again");
        retryButton.getStyleClass().add("danger-button");
        retryButton.setOnAction(e -> loadProducts());
        
        errorState.getChildren().addAll(errorLabel, errorDescription, retryButton);
        productFlowPane.getChildren().clear();
        productFlowPane.getChildren().add(errorState);
    }
    
    // Fallback method for direct database access when service is not available
    private void loadProductsDirectly() {
        System.out.println("Loading products from database...");
        
        try {
            List<Product> products = loadProductsFromDatabase();
            
            if (products.isEmpty()) {
                productFlowPane.getChildren().add(new Label("No products found."));
                updatePaginationControls(0, 0, 0);
                return;
            }
            
            // Apply pagination
            int start = (currentPage - 1) * PAGE_SIZE;
            int end = Math.min(start + PAGE_SIZE, products.size());
            List<Product> pageProducts = products.subList(start, end);
            
            // Calculate total pages
            totalPages = (int) Math.ceil((double) products.size() / PAGE_SIZE);
            
            for (Product product : pageProducts) {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/aims/presentation/views/partials/product_card.fxml"));
                    Parent productCardNode = loader.load();
                    ProductCardController cardController = loader.getController();
                    
                    // Set data for the product card
                    cardController.setData(product);
                    // Set MainLayoutController reference for navigation
                    if (mainLayoutController != null) {
                        cardController.setMainLayoutController(mainLayoutController);
                    }
                    productFlowPane.getChildren().add(productCardNode);
                    
                } catch (IOException e) {
                    e.printStackTrace();
                    System.err.println("Error loading product card: " + e.getMessage());
                }
            }
            
            updatePaginationControls(currentPage, totalPages, products.size());
            
        } catch (SQLException e) {
            e.printStackTrace();
            productFlowPane.getChildren().add(new Label("Error loading products: " + e.getMessage()));
            updatePaginationControls(0, 0, 0);
        }
    }
    
    private List<Product> loadProductsFromDatabase() throws SQLException {
        List<Product> products = new ArrayList<>();
        
        // Use DatabaseConfig instead of hardcoded path
        com.aims.core.infrastructure.config.DatabaseConfig dbConfig =
            com.aims.core.infrastructure.config.DatabaseConfig.getInstance();
        String dbUrl = dbConfig.getDatabaseUrl();
        
        String sql = "SELECT productID, title, category, price, quantityInStock, description, imageURL, productType " +
                    "FROM PRODUCT " +
                    "ORDER BY title";
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                Product product = new Product();
                product.setProductId(rs.getString("productID"));
                product.setTitle(rs.getString("title"));
                product.setCategory(rs.getString("category"));
                product.setPrice(rs.getFloat("price") * 1.1f); // Add 10% VAT for display
                product.setQuantityInStock(rs.getInt("quantityInStock"));
                product.setDescription(rs.getString("description"));
                product.setImageUrl(rs.getString("imageURL"));
                
                // Set product type
                String typeStr = rs.getString("productType");
                if (typeStr != null) {
                    try {
                        product.setProductType(com.aims.core.enums.ProductType.valueOf(typeStr));
                    } catch (IllegalArgumentException e) {
                        product.setProductType(com.aims.core.enums.ProductType.BOOK); // Default
                    }
                }
                
                products.add(product);
            }
        }
        
        return products;
    }

    private void updatePaginationControls(int current, int total, long totalItems) {
        this.currentPage = current;
        this.totalPages = total;
        if (totalItems == 0) {
            currentPageLabel.setText("No products");
            paginationControls.setVisible(false);
        } else {
            currentPageLabel.setText("Page " + this.currentPage + " / " + this.totalPages);
            paginationControls.setVisible(true);
        }
        prevPageButton.setDisable(this.currentPage <= 1);
        nextPageButton.setDisable(this.currentPage >= this.totalPages);
    }

    @FXML
    void handleSearchAction(ActionEvent event) {
        currentPage = 1; // Reset to first page for new search from UI text field
        currentSearchTerm = searchField.getText().trim();
        // Category and SortBy are already handled by their listeners if they trigger this.
        // If Search button is clicked independently, ensure currentCategoryFilter and currentSortBy are up-to-date.
        currentCategoryFilter = "All Categories".equals(categoryComboBox.getValue()) ? null : categoryComboBox.getValue();
        String selectedSort = sortByPriceComboBox.getValue();
        if ("Price: Low to High".equals(selectedSort)) {
            currentSortBy = "ASC";
        } else if ("Price: High to Low".equals(selectedSort)) {
            currentSortBy = "DESC";
        } else {
            currentSortBy = null; // Default sort
        }
        loadProducts();
    }

    @FXML
    void handlePrevPageAction(ActionEvent event) {
        if (currentPage > 1) {
            currentPage--;
            loadProducts();
        }
    }

    @FXML
    void handleNextPageAction(ActionEvent event) {
        if (currentPage < totalPages) {
            currentPage++;
            loadProducts();
        }
    }

    /**
     * Public method that can be called by ProductCardController or other components
     * to refresh parts of the home screen, e.g., a mini-cart summary if you add one.
     */
    public void refreshCartSummary() {
        // TODO: Implement logic to update a mini-cart display if it exists on this screen.
        System.out.println("HomeScreenController: Cart summary refresh requested.");
    }

    /**
     * Public method to allow navigation to product detail screen from a product card.
     * This would be called by ProductCardController.
     */
    public void navigateToProductDetail(String productId) {
        System.out.println("HomeScreenController.navigateToProductDetail: Called with productId: " + productId);
        
        if (mainLayoutController != null) {
            try {
                // Preserve current search state in FXMLSceneManager's currentContext
                com.aims.core.presentation.utils.FXMLSceneManager sceneManager = com.aims.core.presentation.utils.FXMLSceneManager.getInstance();
                String apiSortBy = "title"; // Default
                if (currentSortBy != null) {
                    if ("ASC".equals(currentSortBy)) {
                        apiSortBy = "price_asc";
                    } else if ("DESC".equals(currentSortBy)) {
                        apiSortBy = "price_desc";
                    }
                }
                sceneManager.preserveSearchContext(currentSearchTerm, currentCategoryFilter, apiSortBy, currentPage);
                System.out.println("HomeScreenController.navigateToProductDetail: Preserved context - Term: " + currentSearchTerm + ", Cat: " + currentCategoryFilter + ", Sort: " + apiSortBy + ", Page: " + currentPage);

                // Load the product detail screen using history-aware navigation
                Object controller = mainLayoutController.loadContentWithHistory(
                    com.aims.core.shared.constants.FXMLPaths.PRODUCT_DETAIL_SCREEN,
                    "Product Details"
                );
                
                if (controller instanceof ProductDetailScreenController detailController) {
                    // Dependencies like ProductService, CartService, MainLayoutController, SceneManager
                    // should be injected by FXMLSceneManager or MainLayoutController during loadContentWithHistory.
                    // We just need to set the specific product ID.
                    detailController.setProductId(productId);
                    System.out.println("HomeScreenController.navigateToProductDetail: Product ID set on ProductDetailScreenController.");
                } else {
                    System.err.println("Failed to cast controller to ProductDetailScreenController");
                }
            } catch (Exception e) {
                System.err.println("Error navigating to product detail: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.err.println("MainLayoutController is null, cannot navigate");
        }
    }

    /**
     * Stores the current search context for later restoration when returning from product details.
     * This method is called before navigating to product detail screen.
     */
    private void storeNavigationContext() {
        System.out.println("HomeScreenController.storeNavigationContext: Storing current search state");
        System.out.println("HomeScreenController.storeNavigationContext: Search term: '" + currentSearchTerm + "'");
        System.out.println("HomeScreenController.storeNavigationContext: Category filter: '" + currentCategoryFilter + "'");
        System.out.println("HomeScreenController.storeNavigationContext: Sort by: '" + currentSortBy + "'");
        System.out.println("HomeScreenController.storeNavigationContext: Current page: " + currentPage);
        
        // The actual context storage is handled by FXMLSceneManager when using loadContentWithHistory
        // This method is mainly for logging and potential future extensions
    }

    /**
     * Restores the search context from navigation history.
     * This method is called by FXMLSceneManager when returning from product detail screen.
     *
     * @param searchTerm The search term to restore
     * @param categoryFilter The category filter to restore
     * @param sortBy The sort order to restore
     * @param page The page number to restore
     */
    public void restoreSearchContext(String searchTerm, String categoryFilter, String sortBy, int page) {
        System.out.println("HomeScreenController.restoreSearchContext: Restoring search context");
        System.out.println("HomeScreenController.restoreSearchContext: Search term: '" + searchTerm + "'");
        System.out.println("HomeScreenController.restoreSearchContext: Category filter: '" + categoryFilter + "'");
        System.out.println("HomeScreenController.restoreSearchContext: Sort by: '" + sortBy + "'");
        System.out.println("HomeScreenController.restoreSearchContext: Page: " + page);

        try {
            // Restore search term
            if (searchTerm != null && !searchTerm.trim().isEmpty()) {
                currentSearchTerm = searchTerm;
                if (searchField != null) {
                    searchField.setText(searchTerm);
                }
            } else {
                currentSearchTerm = "";
                if (searchField != null) {
                    searchField.setText("");
                }
            }

            // Restore category filter
            if (categoryFilter != null && !categoryFilter.trim().isEmpty()) {
                currentCategoryFilter = categoryFilter;
                if (categoryComboBox != null) {
                    categoryComboBox.setValue(categoryFilter);
                }
            } else {
                currentCategoryFilter = null;
                if (categoryComboBox != null) {
                    categoryComboBox.setValue("All Categories");
                }
            }

            // Restore sort order
            if (sortBy != null && !sortBy.trim().isEmpty()) {
                currentSortBy = sortBy;
                if (sortByPriceComboBox != null) {
                    switch (sortBy) {
                        case "ASC":
                            sortByPriceComboBox.setValue("Price: Low to High");
                            break;
                        case "DESC":
                            sortByPriceComboBox.setValue("Price: High to Low");
                            break;
                        default:
                            sortByPriceComboBox.setValue("Default Sort");
                            break;
                    }
                }
            } else {
                currentSortBy = null;
                if (sortByPriceComboBox != null) {
                    sortByPriceComboBox.setValue("Default Sort");
                }
            }

            // Restore page number
            currentPage = Math.max(1, page);

            // Reload products with restored context
            loadProducts();
            
            System.out.println("HomeScreenController.restoreSearchContext: Successfully restored search context and reloaded products");
            
        } catch (Exception e) {
            System.err.println("HomeScreenController.restoreSearchContext: Error restoring search context: " + e.getMessage());
            e.printStackTrace();
            
            // Fallback to loading products without restored context
            loadProducts();
        }
    }

    // =========================================================================
    // VIETNAMESE GUIDE: RESPONSIVE METHODS IMPLEMENTATION (STEP 5)
    // =========================================================================
    
    /**
     * Vietnamese Guide: Update responsive layout based on window size changes
     * Real-time responsive behavior during window resize
     */
    public void updateResponsiveLayout(double width, double height) {
        System.out.println("HomeScreenController.updateResponsiveLayout: Updating layout for " + width + "x" + height);
        
        try {
            // Update product card sizes based on container width
            updateProductCardSizes(width);
            
            // Calculate and apply optimal column count
            int optimalColumns = calculateOptimalColumns(width);
            
            // Apply responsive classes based on screen size
            if (productFlowPane != null) {
                // Remove existing responsive classes
                productFlowPane.getStyleClass().removeIf(styleClass ->
                    styleClass.startsWith("responsive-") && (
                        styleClass.contains("mobile") ||
                        styleClass.contains("tablet") ||
                        styleClass.contains("desktop") ||
                        styleClass.contains("ultrawide")
                    )
                );
                
                // Add new responsive class based on width
                String responsiveClass = getResponsiveClassForWidth(width);
                if (!productFlowPane.getStyleClass().contains(responsiveClass)) {
                    productFlowPane.getStyleClass().add(responsiveClass);
                }
            }
            
            System.out.println("HomeScreenController.updateResponsiveLayout: Applied " + optimalColumns + " columns for width " + width);
            
        } catch (Exception e) {
            System.err.println("HomeScreenController.updateResponsiveLayout: Error updating responsive layout: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Vietnamese Guide: Update product card sizes based on container width
     * Responsive breakpoints: Mobile(1 col), Tablet(2 col), Desktop(3 col), Large Desktop(4 col), Ultra-wide(5 col)
     */
    public void updateProductCardSizes(double containerWidth) {
        System.out.println("HomeScreenController.updateProductCardSizes: Updating for container width: " + containerWidth);
        
        if (productFlowPane == null) {
            return;
        }
        
        try {
            // Calculate optimal card width based on container width and column count
            int columns = calculateOptimalColumns(containerWidth);
            double availableWidth = containerWidth - 60; // Account for padding
            double cardWidth = (availableWidth - (columns - 1) * 20) / columns; // Account for gaps
            
            // Ensure minimum card width
            cardWidth = Math.max(cardWidth, 180);
            
            // Update gap spacing based on screen size
            double gap = containerWidth >= 1920 ? 30 :
                        containerWidth >= 1440 ? 25 :
                        containerWidth >= 1024 ? 20 :
                        containerWidth >= 768 ? 15 : 10;
            
            productFlowPane.setHgap(gap);
            productFlowPane.setVgap(gap);
            
            System.out.println("HomeScreenController.updateProductCardSizes: Set " + columns + " columns with card width " + cardWidth + " and gap " + gap);
            
        } catch (Exception e) {
            System.err.println("HomeScreenController.updateProductCardSizes: Error updating card sizes: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Vietnamese Guide: Calculate optimal column count based on width
     * Responsive breakpoints: Mobile(1 col), Tablet(2 col), Desktop(3 col), Large Desktop(4 col), Ultra-wide(5 col)
     */
    public int calculateOptimalColumns(double width) {
        // Vietnamese guide responsive breakpoints exactly as specified
        if (width < 768) {
            return 1; // Mobile (1 col)
        } else if (width < 1024) {
            return 2; // Tablet (2 col)
        } else if (width < 1440) {
            return 3; // Desktop (3 col)
        } else if (width < 1920) {
            return 4; // Large Desktop (4 col)
        } else {
            return 5; // Ultra-wide (5 col)
        }
    }
    
    /**
     * Get the responsive CSS class for the given width
     */
    private String getResponsiveClassForWidth(double width) {
        if (width < 768) {
            return "responsive-mobile";
        } else if (width < 1024) {
            return "responsive-tablet";
        } else if (width < 1440) {
            return "responsive-desktop";
        } else if (width < 1920) {
            return "responsive-large-desktop";
        } else {
            return "responsive-ultrawide";
        }
    }
    
    /**
     * Apply dynamic sizing to product cards based on screen dimensions
     */
    private void applyDynamicCardSizing(Parent productCardNode) {
        if (productFlowPane == null || productFlowPane.getScene() == null) {
            return;
        }
        
        try {
            double containerWidth = productFlowPane.getWidth();
            if (containerWidth <= 0 && productFlowPane.getScene() != null) {
                containerWidth = productFlowPane.getScene().getWidth() - 100; // Account for margins
            }
            
            if (containerWidth <= 0) {
                containerWidth = 1200; // Default fallback width
            }
            
            // Calculate optimal card dimensions using the existing method
            int columns = calculateOptimalColumns(containerWidth);
            double availableWidth = containerWidth - 20; // Account for padding
            double cardWidth = Math.max(280, (availableWidth - (columns - 1) * 10) / columns); // Account for gaps
            
            // Set preferred card dimensions
            if (productCardNode instanceof javafx.scene.layout.VBox) {
                javafx.scene.layout.VBox cardVBox = (javafx.scene.layout.VBox) productCardNode;
                cardVBox.setPrefWidth(cardWidth);
                cardVBox.setMaxWidth(cardWidth + 100); // Allow some flexibility
                
                // Scale card height proportionally
                double cardHeight = Math.max(350, cardWidth * 1.3);
                cardVBox.setPrefHeight(cardHeight);
                cardVBox.setMaxHeight(cardHeight + 50);
                
                System.out.println("HomeScreenController: Applied card size " + cardWidth + "x" + cardHeight + " for " + columns + " columns");
            }
            
        } catch (Exception e) {
            System.err.println("HomeScreenController.applyDynamicCardSizing: Error applying dynamic sizing: " + e.getMessage());
        }
    }
}