package com.aims.core.presentation.controllers;

import com.aims.core.application.services.IProductService;
import com.aims.core.application.services.ICartService;
import com.aims.core.entities.Product;
import com.aims.core.shared.utils.SearchResult; // Your SearchResult utility class
// import com.aims.presentation.utils.AlertHelper;
// import com.aims.presentation.utils.FXMLSceneManager;


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

public class ProductSearchResultsController {

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

    // --- Service Dependencies (to be injected) ---
    // @Inject
    private IProductService productService;
    // @Inject
    private ICartService cartService; // For product cards
    // private MainLayoutController mainLayoutController;
    // private FXMLSceneManager sceneManager;

    private int currentPage = 1;
    private final int PAGE_SIZE = 20; // As per requirement
    private int totalPages = 1;

    private String initialSearchTerm;
    private String initialCategory;


    public ProductSearchResultsController() {
        // productService = new ProductServiceImpl(...); // DI
        // cartService = new CartServiceImpl(...);    // DI
    }

    // public void setMainLayoutController(MainLayoutController mainLayoutController) { this.mainLayoutController = mainLayoutController; }
    // public void setSceneManager(FXMLSceneManager sceneManager) { this.sceneManager = sceneManager; }
    // public void setProductService(IProductService productService) { this.productService = productService; }
    // public void setCartService(ICartService cartService) { this.cartService = cartService; }

    public void initialize() {
        // sceneManager = FXMLSceneManager.getInstance();
        sortByPriceComboBox.setItems(FXCollections.observableArrayList("Default", "Price: Low to High", "Price: High to Low"));
        sortByPriceComboBox.setValue("Default"); // Default sort
        // TODO: Load categories into categoryComboBox from productService or distinct values
        // loadProducts(); // Will be called by setSearchCriteria
    }

    /**
     * Sets the initial search criteria when navigating to this screen.
     */
    public void setSearchCriteria(String searchTerm, String category) {
        this.initialSearchTerm = searchTerm;
        this.initialCategory = category;

        if (searchField != null) searchField.setText(searchTerm != null ? searchTerm : "");
        if (categoryComboBox != null) categoryComboBox.setValue(category); // Assuming category name is string

        if (searchTerm != null && !searchTerm.isEmpty()){
            searchResultsTitleLabel.setText("Search Results for: \"" + searchTerm + "\"");
        } else if (category != null && !category.isEmpty()){
            searchResultsTitleLabel.setText("Products in Category: " + category);
        } else {
            searchResultsTitleLabel.setText("Search Results");
        }
        
        this.currentPage = 1; // Reset to first page for new criteria
        loadSearchedProducts();
    }


    private void loadSearchedProducts() {
        // if (productService == null) {
        //     AlertHelper.showErrorAlert("Service Error", "Product service is unavailable.");
        //     productFlowPane.getChildren().clear();
        //     productFlowPane.getChildren().add(new Label("Error: Could not load search results. Service not available."));
        //     updatePaginationControls(0,0);
        //     return;
        // }

        String searchTerm = searchField.getText() != null ? searchField.getText().trim() : initialSearchTerm;
        String category = categoryComboBox.getValue() != null ? categoryComboBox.getValue() : initialCategory;
        if ("All Categories".equals(category)) category = null;

        String sortBy = null;
        String selectedSort = sortByPriceComboBox.getValue();
        if ("Price: Low to High".equals(selectedSort)) {
            sortBy = "ASC";
        } else if ("Price: High to Low".equals(selectedSort)) {
            sortBy = "DESC";
        }

        // try {
        //     SearchResult<Product> searchResult = productService.searchProducts(searchTerm, category, currentPage, PAGE_SIZE, sortBy);
        //     totalPages = searchResult.totalPages();
        //     List<Product> products = searchResult.results(); // Products should have VAT-inclusive prices from service
        //
        //     productFlowPane.getChildren().clear();
        //     if (products.isEmpty() && currentPage == 1) {
        //          productFlowPane.getChildren().add(new Label("No products found matching your search criteria."));
        //     } else {
        //         for (Product product : products) {
        //             try {
        //                 FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/aims/presentation/views/partials/product_card.fxml"));
        //                 Parent productCardNode = loader.load();
        //                 ProductCardController cardController = loader.getController();
        //                 // cardController.setCartService(this.cartService);
        //                 // cardController.setMainLayoutController(this.mainLayoutController);
        //                 // cardController.setHomeScreenController(null); // Not the home screen controller
        //                 cardController.setData(product);
        //                 productFlowPane.getChildren().add(productCardNode);
        //             } catch (IOException e) {
        //                 e.printStackTrace();
        //                 System.err.println("Error loading product card: " + e.getMessage());
        //             }
        //         }
        //     }
        //     updatePaginationControls(searchResult.currentPage(), searchResult.totalPages());
        //
        // } catch (SQLException e) {
        //     e.printStackTrace();
        //     // AlertHelper.showErrorAlert("Search Error", "Could not perform search: " + e.getMessage());
        //     productFlowPane.getChildren().clear();
        //     productFlowPane.getChildren().add(new Label("Error performing search."));
        // }
        System.out.println("loadSearchedProducts called - Implement with actual service call. Term: " + searchTerm + ", Cat: " + category + ", Sort: " + sortBy + ", Page: " + currentPage);
        // Dummy update for UI
        // updatePaginationControls(1,1);
    }

    private void updatePaginationControls(int current, int total) {
        currentPage = current;
        totalPages = total;
        if (total == 0 && current == 0) { // Special case for no results at all
            currentPageLabel.setText("No results");
            paginationControls.setVisible(false);
        } else if (total == 0) { // No items but page might be 1
            currentPageLabel.setText("Page 1/0");
            paginationControls.setVisible(false);
        }
        else {
            currentPageLabel.setText("Page " + currentPage + "/" + totalPages);
            paginationControls.setVisible(true);
        }
        prevPageButton.setDisable(currentPage <= 1);
        nextPageButton.setDisable(currentPage >= totalPages);
    }

    @FXML
    void handleSearchAction(ActionEvent event) {
        currentPage = 1; // Reset to first page for new search from UI elements
        this.initialSearchTerm = searchField.getText(); // Update initial term for subsequent pagination
        this.initialCategory = categoryComboBox.getValue(); // Update initial category
        if ("All Categories".equals(this.initialCategory)) this.initialCategory = null;
        
        if (this.initialSearchTerm != null && !this.initialSearchTerm.isEmpty()){
            searchResultsTitleLabel.setText("Search Results for: \"" + this.initialSearchTerm + "\"");
        } else if (this.initialCategory != null && !this.initialCategory.isEmpty()){
            searchResultsTitleLabel.setText("Products in Category: " + this.initialCategory);
        } else {
             searchResultsTitleLabel.setText("All Products"); // Or some default if both are empty
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
}