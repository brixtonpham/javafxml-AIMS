// filepath: /home/namu10x/Desktop/AIMS_Project/src/main/java/com/aims/core/presentation/controllers/ProductSearchResultsController.java
package com.aims.core.presentation.controllers;

import com.aims.core.application.services.IProductService;
import com.aims.core.application.services.ICartService;
import com.aims.core.entities.Product;
import com.aims.core.shared.utils.SearchResult;

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

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import com.aims.core.presentation.utils.FXMLSceneManager; // Added for navigation
import com.aims.core.shared.constants.FXMLPaths; // Added for FXML paths

public class ProductSearchResultsController {

    private MainLayoutController mainLayoutController;
    private FXMLSceneManager sceneManager;

    @FXML
    private Label searchResultsTitleLabel;
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

    // --- Service Dependencies ---
    private IProductService productService;
    private ICartService cartService;

    // --- Pagination and State ---
    private int currentPage = 1;
    private final int PAGE_SIZE = 20;
    private int totalPages = 1;

    private String initialSearchTerm;
    private String initialCategory;

    public ProductSearchResultsController() {
        // Constructor for FXML loading
    }

    public void setProductService(IProductService productService) {
        this.productService = productService;
    }

    public void setCartService(ICartService cartService) {
        this.cartService = cartService;
    }

    public void setMainLayoutController(MainLayoutController mainLayoutController) {
        this.mainLayoutController = mainLayoutController;
        // FXMLSceneManager is a singleton, get instance directly
        this.sceneManager = FXMLSceneManager.getInstance();
    }

    public void initialize() {
        sortByPriceComboBox.setItems(FXCollections.observableArrayList("Default", "Price: Low to High", "Price: High to Low"));
        sortByPriceComboBox.setValue("Default");
        
        // Load product types dynamically from productService
        try {
            if (productService != null) {
                List<String> productTypes = productService.getAllProductTypes();
                productTypes.add(0, "All Categories");
                categoryComboBox.setItems(FXCollections.observableArrayList(productTypes));
                categoryComboBox.setValue("All Categories");
            } else {
                categoryComboBox.setItems(FXCollections.observableArrayList("All Categories", "Books", "CDs", "DVDs", "LP Records"));
                categoryComboBox.setValue("All Categories");
            }
        } catch (SQLException e) {
            categoryComboBox.setItems(FXCollections.observableArrayList("All Categories", "Books", "CDs", "DVDs", "LP Records"));
            categoryComboBox.setValue("All Categories");
            System.err.println("Error loading product types: " + e.getMessage());
        }
        
        // Add listeners for filter changes
        categoryComboBox.setOnAction(event -> handleFilterChange());
        sortByPriceComboBox.setOnAction(event -> handleFilterChange());
    }

    /**
     * Sets the initial search criteria when navigating to this screen.
     */
    public void setSearchCriteria(String searchTerm, String category) {
        this.initialSearchTerm = searchTerm;
        this.initialCategory = category;

        if (searchField != null) searchField.setText(searchTerm != null ? searchTerm : "");
        if (categoryComboBox != null && category != null) categoryComboBox.setValue(category);

        if (searchTerm != null && !searchTerm.isEmpty()) {
            searchResultsTitleLabel.setText("Search Results for: \"" + searchTerm + "\"");
        } else if (category != null && !category.isEmpty()) {
            searchResultsTitleLabel.setText("Products in Category: " + category);
        } else {
            searchResultsTitleLabel.setText("Search Results");
        }
        
        this.currentPage = 1;
        loadSearchedProducts();
    }

    private void handleFilterChange() {
        currentPage = 1; // Reset to first page when filters change
        loadSearchedProducts();
    }

    private void loadSearchedProducts() {
        if (productService == null) {
            productFlowPane.getChildren().clear();
            productFlowPane.getChildren().add(new Label("Error: Product service not available."));
            updatePaginationControls(0, 0);
            return;
        }

        String searchTerm = searchField.getText() != null ? searchField.getText().trim() : initialSearchTerm;
        String category = categoryComboBox.getValue() != null ? categoryComboBox.getValue() : initialCategory;
        if ("All Categories".equals(category)) category = null;

        // Convert sort selection to service parameters
        String sortBy = "title";
        String sortOrder = "ASC";
        if (sortByPriceComboBox.getValue() != null) {
            switch (sortByPriceComboBox.getValue()) {
                case "Price: Low to High":
                    sortBy = "price";
                    sortOrder = "ASC";
                    break;
                case "Price: High to Low":
                    sortBy = "price";
                    sortOrder = "DESC";
                    break;
                default:
                    sortBy = "title";
                    sortOrder = "ASC";
                    break;
            }
        }

        productFlowPane.getChildren().clear();

        try {
            // Use the advanced search method
            SearchResult<Product> searchResult = productService.advancedSearchProducts(
                searchTerm, category, sortBy, sortOrder, currentPage, PAGE_SIZE);

            List<Product> products = searchResult.results();
            totalPages = searchResult.totalPages();

            if (products.isEmpty()) {
                productFlowPane.getChildren().add(new Label("No products found matching your criteria."));
                updatePaginationControls(0, 0);
                return;
            }

            // Create product cards with dynamic sizing
            for (Product product : products) {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/aims/presentation/views/partials/product_card.fxml"));
                    Parent productCardNode = loader.load();
                    ProductCardController cardController = loader.getController();

                    // Set data for the product card
                    cardController.setData(product);
                    if (cartService != null) {
                        cardController.setCartService(cartService);
                    }
                    // Pass this controller to the card controller for navigation purposes
                    cardController.setProductSearchResultsController(this);
                    cardController.setMainLayoutController(mainLayoutController); // Ensure card has main layout for fallback

                    // Apply dynamic sizing based on screen dimensions
                    applyDynamicCardSizing(productCardNode);

                    productFlowPane.getChildren().add(productCardNode);

                } catch (IOException e) {
                    e.printStackTrace();
                    System.err.println("Error loading product card: " + e.getMessage());
                }
            }
            
            // Apply responsive layout after all cards are loaded
            javafx.application.Platform.runLater(() -> updateResponsiveLayout());

            updatePaginationControls(searchResult.totalPages(), (int) searchResult.totalResults());

        } catch (SQLException e) {
            e.printStackTrace();
            productFlowPane.getChildren().add(new Label("Error loading search results: " + e.getMessage()));
            updatePaginationControls(0, 0);
        }
    }

    private void updatePaginationControls(int total, int totalResults) {
        this.totalPages = total;
        if (totalResults == 0) {
            currentPageLabel.setText("No results");
            paginationControls.setVisible(false);
        } else {
            currentPageLabel.setText("Page " + currentPage + "/" + totalPages);
            paginationControls.setVisible(true);
        }
        prevPageButton.setDisable(currentPage <= 1);
        nextPageButton.setDisable(currentPage >= totalPages);
    }

    @FXML
    void handleSearchAction(ActionEvent event) {
        currentPage = 1;
        this.initialSearchTerm = searchField.getText();
        this.initialCategory = categoryComboBox.getValue();
        if ("All Categories".equals(this.initialCategory)) this.initialCategory = null;
        
        if (this.initialSearchTerm != null && !this.initialSearchTerm.isEmpty()) {
            searchResultsTitleLabel.setText("Search Results for: \"" + this.initialSearchTerm + "\"");
        } else if (this.initialCategory != null && !this.initialCategory.isEmpty()) {
            searchResultsTitleLabel.setText("Products in Category: " + this.initialCategory);
        } else {
            searchResultsTitleLabel.setText("All Products");
        }
        
        loadSearchedProducts();
    }

    @FXML
    void handlePrevPageAction(ActionEvent event) {
        if (currentPage > 1) {
            currentPage--;
            loadSearchedProducts();
        }
    }

    @FXML
    void handleNextPageAction(ActionEvent event) {
        if (currentPage < totalPages) {
            currentPage++;
            loadSearchedProducts();
        }
    }

    @FXML
    void handleNavigateHome(ActionEvent event) {
        if (mainLayoutController != null) {
            // Use loadContentWithHistory to ensure proper context handling and history tracking
            mainLayoutController.loadContentWithHistory(FXMLPaths.HOME_SCREEN, "Home");
        } else {
            System.err.println("ProductSearchResultsController: MainLayoutController is null. Cannot navigate to home.");
            // Optionally, show an error dialog to the user
        }
    }

    /**
     * Navigates to the product detail screen, preserving the current search context.
     * @param productId The ID of the product to display.
     */
    public void navigateToProductDetail(String productId) {
        if (mainLayoutController != null && sceneManager != null) {
            // Preserve current search state
            String searchTerm = searchField.getText() != null ? searchField.getText().trim() : initialSearchTerm;
            String category = categoryComboBox.getValue() != null ? categoryComboBox.getValue() : initialCategory;
            if ("All Categories".equals(category)) category = null;

            String sortByValue = sortByPriceComboBox.getValue();
            String sortByApiValue = "title"; // default
            if (sortByValue != null) {
                switch (sortByValue) {
                    case "Price: Low to High":
                        sortByApiValue = "price_asc"; // Or however your API/NavigationContext expects it
                        break;
                    case "Price: High to Low":
                        sortByApiValue = "price_desc";
                        break;
                }
            }
            
            sceneManager.preserveSearchContext(searchTerm, category, sortByApiValue, currentPage);

            // Navigate to product detail screen
            // The FXMLSceneManager's loadContentWithHistory will push the current (search results)
            // context onto the history stack before loading the new screen.
            Object controller = mainLayoutController.loadContentWithHistory(FXMLPaths.PRODUCT_DETAIL_SCREEN, "Product Details");
            if (controller instanceof ProductDetailScreenController detailController) {
                detailController.setProductId(productId); // Product ID is set after loading
            }
        } else {
            System.err.println("ProductSearchResultsController: MainLayoutController or SceneManager is null. Cannot navigate to product detail.");
        }
    }

    /**
     * Restores the search context when navigating back to this screen.
     * Called by FXMLSceneManager.
     * @param searchTerm The search term to restore.
     * @param categoryFilter The category filter to restore.
     * @param sortBy The sort criteria to restore.
     * @param page The page number to restore.
     */
    public void restoreSearchContext(String searchTerm, String categoryFilter, String sortBy, int page) {
        System.out.println("ProductSearchResultsController: Restoring search context...");
        System.out.println("  Search Term: " + searchTerm);
        System.out.println("  Category: " + categoryFilter);
        System.out.println("  SortBy: " + sortBy);
        System.out.println("  Page: " + page);

        this.initialSearchTerm = searchTerm;
        this.initialCategory = categoryFilter;
        this.currentPage = page > 0 ? page : 1;

        if (searchField != null) {
            searchField.setText(searchTerm != null ? searchTerm : "");
        }
        if (categoryComboBox != null) {
            categoryComboBox.setValue(categoryFilter != null && !categoryFilter.isEmpty() ? categoryFilter : "All Categories");
        }
        if (sortByPriceComboBox != null) {
            if ("price_asc".equalsIgnoreCase(sortBy)) {
                sortByPriceComboBox.setValue("Price: Low to High");
            } else if ("price_desc".equalsIgnoreCase(sortBy)) {
                sortByPriceComboBox.setValue("Price: High to Low");
            } else {
                sortByPriceComboBox.setValue("Default");
            }
        }
        
        // Update title label based on restored criteria
        if (this.initialSearchTerm != null && !this.initialSearchTerm.isEmpty()) {
            searchResultsTitleLabel.setText("Search Results for: \"" + this.initialSearchTerm + "\"");
        } else if (this.initialCategory != null && !this.initialCategory.isEmpty()) {
            searchResultsTitleLabel.setText("Products in Category: " + this.initialCategory);
        } else {
            searchResultsTitleLabel.setText("Search Results");
        }

        loadSearchedProducts();
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
            
            // Calculate optimal card dimensions
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
                
                System.out.println("ProductSearchResultsController: Applied card size " + cardWidth + "x" + cardHeight + " for " + columns + " columns");
            }
            
        } catch (Exception e) {
            System.err.println("ProductSearchResultsController.applyDynamicCardSizing: Error applying dynamic sizing: " + e.getMessage());
        }
    }
    
    /**
     * Update responsive layout for the entire product grid
     */
    private void updateResponsiveLayout() {
        if (productFlowPane == null || productFlowPane.getScene() == null) {
            return;
        }
        
        try {
            double containerWidth = productFlowPane.getScene().getWidth();
            double containerHeight = productFlowPane.getScene().getHeight();
            
            System.out.println("ProductSearchResultsController.updateResponsiveLayout: Container size " + containerWidth + "x" + containerHeight);
            
            // Calculate optimal gap spacing based on screen size
            double gap = containerWidth >= 2560 ? 8 :
                        containerWidth >= 1920 ? 6 :
                        containerWidth >= 1440 ? 5 :
                        containerWidth >= 1024 ? 4 : 3;
            
            productFlowPane.setHgap(gap);
            productFlowPane.setVgap(gap);
            
            // Update responsive style classes
            updateResponsiveStyleClasses(containerWidth);
            
            // Re-apply sizing to all existing cards
            for (javafx.scene.Node child : productFlowPane.getChildren()) {
                if (child instanceof Parent) {
                    applyDynamicCardSizing((Parent) child);
                }
            }
            
            System.out.println("ProductSearchResultsController.updateResponsiveLayout: Applied gap " + gap + "px for width " + containerWidth);
            
        } catch (Exception e) {
            System.err.println("ProductSearchResultsController.updateResponsiveLayout: Error updating responsive layout: " + e.getMessage());
        }
    }
    
    /**
     * Calculate optimal column count based on container width
     */
    private int calculateOptimalColumns(double width) {
        if (width >= 2560) {
            return 5; // Ultra-wide (5 columns)
        } else if (width >= 1920) {
            return 4; // Large Desktop (4 columns)
        } else if (width >= 1440) {
            return 4; // Desktop (4 columns)
        } else if (width >= 1024) {
            return 3; // Laptop (3 columns)
        } else if (width >= 768) {
            return 2; // Tablet (2 columns)
        } else {
            return 1; // Mobile (1 column)
        }
    }
    
    /**
     * Update responsive style classes based on container width
     */
    private void updateResponsiveStyleClasses(double width) {
        if (productFlowPane == null) {
            return;
        }
        
        // Remove existing responsive classes
        productFlowPane.getStyleClass().removeIf(styleClass ->
            styleClass.startsWith("responsive-") && (
                styleClass.contains("mobile") ||
                styleClass.contains("tablet") ||
                styleClass.contains("desktop") ||
                styleClass.contains("ultrawide")
            )
        );
        
        // Add appropriate responsive class
        String responsiveClass = getResponsiveClassForWidth(width);
        if (!productFlowPane.getStyleClass().contains(responsiveClass)) {
            productFlowPane.getStyleClass().add(responsiveClass);
        }
    }
    
    /**
     * Get the responsive CSS class for the given width
     */
    private String getResponsiveClassForWidth(double width) {
        if (width >= 2560) {
            return "responsive-ultrawide";
        } else if (width >= 1920) {
            return "responsive-large-desktop";
        } else if (width >= 1440) {
            return "responsive-desktop";
        } else if (width >= 1024) {
            return "responsive-laptop";
        } else if (width >= 768) {
            return "responsive-tablet";
        } else {
            return "responsive-mobile";
        }
    }
}
