package com.aims.presentation.controllers;

import com.aims.core.application.dtos.ProductDTO; // Hoặc Entity Product trực tiếp nếu không có DTO phức tạp
import com.aims.core.application.services.IProductService;
import com.aims.core.application.services.ICartService;
// import com.aims.presentation.utils.FXMLSceneManager;
// import com.aims.presentation.models.ProductViewModel; // Nếu dùng ViewModel
import com.aims.common.utils.SearchResult;
import com.aims.core.entities.Product;


import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
import javafx.scene.layout.BorderPane; // Để truy cập contentPane của MainLayout

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class HomeScreenController {

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<String> categoryComboBox; // Nên load categories từ service

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

    // --- Service Dependencies (cần được inject) ---
    // @Inject
    private IProductService productService;
    // @Inject
    private ICartService cartService;
    // private FXMLSceneManager sceneManager;
    // private MainLayoutController mainLayoutController; // Để điều hướng

    private int currentPage = 1;
    private final int PAGE_SIZE = 20; // Theo yêu cầu
    private int totalPages = 1;
    private String currentSearchTerm = "";
    private String currentCategory = null;
    private String currentSortBy = null;

    public HomeScreenController() {
        // Ví dụ khởi tạo service (Trong thực tế nên dùng DI)
        // productService = new ProductServiceImpl(...);
        // cartService = new CartServiceImpl(...);
    }

    // Phương thức này có thể được gọi bởi MainLayoutController sau khi load FXML này
    // public void setMainLayoutController(MainLayoutController mainLayoutController) {
    //     this.mainLayoutController = mainLayoutController;
    // }
    // public void setProductService(IProductService productService) { this.productService = productService; }
    // public void setCartService(ICartService cartService) { this.cartService = cartService; }


    public void initialize() {
        // sceneManager = FXMLSceneManager.getInstance();
        sortByPriceComboBox.setItems(FXCollections.observableArrayList("Default", "Price: Low to High", "Price: High to Low"));
        sortByPriceComboBox.setValue("Default");
        // TODO: Load categories into categoryComboBox from productService
        loadProducts();
    }

    private void loadProducts() {
        // productService cần được khởi tạo trước khi gọi
        if (productService == null) {
            System.err.println("ProductService is not initialized in HomeScreenController.");
             // Hiển thị lỗi cho người dùng
            productFlowPane.getChildren().clear();
            productFlowPane.getChildren().add(new Label("Error: Could not load products. Service not available."));
            updatePaginationControls(0,0);
            return;
        }

        try {
            SearchResult<Product> searchResult;
            if (currentSearchTerm.isEmpty() && currentCategory == null) {
                searchResult = productService.getProductsForDisplay(currentPage, PAGE_SIZE);
            } else {
                searchResult = productService.searchProducts(currentSearchTerm, currentCategory, currentPage, PAGE_SIZE, currentSortBy);
            }

            totalPages = searchResult.totalPages();
            List<Product> products = searchResult.results();

            productFlowPane.getChildren().clear();
            if (products.isEmpty() && currentPage == 1) {
                 productFlowPane.getChildren().add(new Label("No products found."));
            } else {
                for (Product product : products) {
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/aims/presentation/views/partials/product_card.fxml"));
                        Parent productCardNode = loader.load();
                        ProductCardController cardController = loader.getController();
                        // cardController.setProductService(productService); // Pass services if needed
                        // cardController.setCartService(cartService);
                        // cardController.setMainLayoutController(mainLayoutController);
                        cardController.setData(product); // Product đã có giá VAT từ service
                        productFlowPane.getChildren().add(productCardNode);
                    } catch (IOException e) {
                        e.printStackTrace();
                        System.err.println("Error loading product card: " + e.getMessage());
                    }
                }
            }
            updatePaginationControls(searchResult.currentPage(), searchResult.totalPages());

        } catch (SQLException e) {
            e.printStackTrace();
            // Hiển thị lỗi cho người dùng
            productFlowPane.getChildren().clear();
            productFlowPane.getChildren().add(new Label("Error loading products from database."));
        }
    }

    private void updatePaginationControls(int current, int total) {
        currentPage = current;
        totalPages = total;
        currentPageLabel.setText("Page " + currentPage + "/" + totalPages);
        prevPageButton.setDisable(currentPage <= 1);
        nextPageButton.setDisable(currentPage >= totalPages);
        paginationControls.setVisible(totalPages > 0);
    }

    @FXML
    void handleSearchAction(ActionEvent event) {
        currentSearchTerm = searchField.getText().trim();
        currentCategory = categoryComboBox.getValue() == null || categoryComboBox.getValue().equals("All Categories") ? null : categoryComboBox.getValue();
        String selectedSort = sortByPriceComboBox.getValue();
        if ("Price: Low to High".equals(selectedSort)) {
            currentSortBy = "ASC";
        } else if ("Price: High to Low".equals(selectedSort)) {
            currentSortBy = "DESC";
        } else {
            currentSortBy = null;
        }
        currentPage = 1; // Reset to first page for new search
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
}