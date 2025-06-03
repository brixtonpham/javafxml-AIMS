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

import java.io.IOException;
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

        // TODO: Load categories into categoryComboBox dynamically from productService
        // Example:
        // List<String> categories = productService.getAllCategories(); // Needs method in IProductService
        // categoryComboBox.setItems(FXCollections.observableArrayList(categories));
        // categoryComboBox.getItems().add(0, "All Categories"); // Add an option for no filter
        // categoryComboBox.setValue("All Categories");
        // Dummy categories for now
        categoryComboBox.setItems(FXCollections.observableArrayList("All Categories", "Book", "CD", "DVD"));
        categoryComboBox.setValue("All Categories");


        // Add listeners to automatically search/filter when combo box values change
        categoryComboBox.setOnAction(event -> handleFilterOrSortChange());
        sortByPriceComboBox.setOnAction(event -> handleFilterOrSortChange());

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
        // if (productService == null) {
        //     AlertHelper.showErrorAlert("Service Error", "Product service is unavailable.");
        //     productFlowPane.getChildren().clear();
        //     productFlowPane.getChildren().add(new Label("Error: Could not load products. Service not available."));
        //     updatePaginationControls(0, 0, 0);
        //     return;
        // }

        // try {
        //     SearchResult<Product> searchResult;
        //     // If searchTerm is empty and category is null, call getProductsForDisplay
        //     // Otherwise, call searchProducts
        //     if ((currentSearchTerm == null || currentSearchTerm.isEmpty()) && currentCategoryFilter == null) {
        //         searchResult = productService.getProductsForDisplay(currentPage, PAGE_SIZE);
        //     } else {
        //         searchResult = productService.searchProducts(currentSearchTerm, currentCategoryFilter, currentPage, PAGE_SIZE, currentSortBy);
        //     }
        //
        //     totalPages = searchResult.totalPages();
        //     List<Product> products = searchResult.results(); // Products should have VAT-inclusive prices
        //
        //     productFlowPane.getChildren().clear();
        //     if (products.isEmpty() && currentPage == 1) {
        //          productFlowPane.getChildren().add(new Label("No products found matching your criteria."));
        //     } else {
        //         for (Product product : products) {
        //             try {
        //                 FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/aims/presentation/views/partials/product_card.fxml"));
        //                 Parent productCardNode = loader.load();
        //                 ProductCardController cardController = loader.getController();
        //
        //                 // Inject dependencies into ProductCardController
        //                 cardController.setCartService(this.cartService);
        //                 cardController.setMainLayoutController(this.mainLayoutController);
        //                 cardController.setHomeScreenController(this); // Pass this controller
        //
        //                 cardController.setData(product);
        //                 productFlowPane.getChildren().add(productCardNode);
        //             } catch (IOException e) {
        //                 e.printStackTrace();
        //                 System.err.println("Error loading product card: " + e.getMessage());
        //             }
        //         }
        //     }
        //     updatePaginationControls(searchResult.currentPage(), searchResult.totalPages(), searchResult.totalResults());
        //
        // } catch (SQLException e) {
        //     e.printStackTrace();
        //     // AlertHelper.showErrorAlert("Load Error", "Could not load products: " + e.getMessage());
        //     productFlowPane.getChildren().clear();
        //     productFlowPane.getChildren().add(new Label("Error loading products. Please try again."));
        // }
        System.out.println("loadProducts called - Implement with actual service call. Term: " + currentSearchTerm + ", Cat: " + currentCategoryFilter + ", Sort: " + currentSortBy + ", Page: " + currentPage);
        // Dummy update
        // updatePaginationControls(1,1, 0);
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
        if (mainLayoutController != null /*&& sceneManager != null*/) {
            System.out.println("HomeScreenController: Navigating to product detail for ID: " + productId);
            // FXMLLoader loader = sceneManager.getLoader(FXMLPaths.PRODUCT_DETAIL_SCREEN);
            // try {
            //     Parent detailNode = loader.load();
            //     ProductDetailScreenController detailController = loader.getController();
            //     detailController.setMainLayoutController(mainLayoutController);
            //     detailController.setProductService(this.productService); // Pass services
            //     detailController.setCartService(this.cartService);
            //     detailController.setProductId(productId);
            //
            //     mainLayoutController.loadContent(FXMLPaths.PRODUCT_DETAIL_SCREEN); // Or use sceneManager to load into contentPane
            //     // The above line might be redundant if sceneManager.loadFXMLIntoPane in ProductCardController already does this.
            //     // This depends on how navigation is structured.
            //     // For simplicity, ProductCardController might directly call mainLayoutController.loadContent()
            //
            // } catch (IOException e) {
            //     e.printStackTrace();
            //     AlertHelper.showErrorAlert("Navigation Error", "Could not load product details.");
            // }
        }
    }
}