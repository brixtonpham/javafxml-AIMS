# Controller Improvements Required

## 1. Complete ProductSearchResultsController Implementation

```java
@FXML
public void initialize() {
    // Setup combo boxes
    sortByPriceComboBox.setItems(FXCollections.observableArrayList(
        "Default", "Price: Low to High", "Price: High to Low"
    ));
    sortByPriceComboBox.setValue("Default");
    
    // Load categories from service
    loadCategories();
    
    // Setup listeners for automatic filtering
    setupListeners();
}

private void loadCategories() {
    try {
        List<String> categories = productService.getAllCategories();
        categories.add(0, "All Categories");
        categoryComboBox.setItems(FXCollections.observableArrayList(categories));
        categoryComboBox.setValue("All Categories");
    } catch (SQLException e) {
        // Handle error - show error message or use default categories
        categoryComboBox.setItems(FXCollections.observableArrayList("All Categories"));
        categoryComboBox.setValue("All Categories");
    }
}

private void setupListeners() {
    // Auto-search when sort option changes
    sortByPriceComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
        if (newVal != null && !newVal.equals(oldVal)) {
            currentPage = 1;
            loadSearchedProducts();
        }
    });
    
    // Auto-search when category changes
    categoryComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
        if (newVal != null && !newVal.equals(oldVal)) {
            currentPage = 1;
            loadSearchedProducts();
        }
    });
}

private void loadSearchedProducts() {
    if (productService == null) {
        showErrorMessage("Product service is unavailable.");
        return;
    }

    String searchTerm = searchField.getText() != null ? searchField.getText().trim() : initialSearchTerm;
    String category = categoryComboBox.getValue() != null ? categoryComboBox.getValue() : initialCategory;
    if ("All Categories".equals(category)) category = null;

    String sortBy = getSortDirection();

    try {
        SearchResult<Product> searchResult = productService.searchProducts(
            searchTerm, category, currentPage, PAGE_SIZE, sortBy
        );
        
        totalPages = searchResult.totalPages();
        List<Product> products = searchResult.results();

        displayProducts(products);
        updatePaginationControls(currentPage, totalPages);
        
    } catch (SQLException e) {
        showErrorMessage("Database error: " + e.getMessage());
    }
}

private String getSortDirection() {
    String selectedSort = sortByPriceComboBox.getValue();
    if ("Price: Low to High".equals(selectedSort)) {
        return "ASC";
    } else if ("Price: High to Low".equals(selectedSort)) {
        return "DESC";
    }
    return null;
}

private void displayProducts(List<Product> products) {
    productFlowPane.getChildren().clear();
    
    if (products.isEmpty() && currentPage == 1) {
        Label noResultsLabel = new Label("No products found matching your search criteria.");
        noResultsLabel.getStyleClass().add("no-results-label");
        productFlowPane.getChildren().add(noResultsLabel);
        return;
    }

    for (Product product : products) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/aims/presentation/views/partials/product_card.fxml")
            );
            Parent productCardNode = loader.load();
            ProductCardController cardController = loader.getController();
            
            // Initialize product card
            cardController.setCartService(this.cartService);
            cardController.initializeWithProduct(product);
            
            productFlowPane.getChildren().add(productCardNode);
            
        } catch (IOException e) {
            System.err.println("Error loading product card: " + e.getMessage());
        }
    }
}

private void updatePaginationControls(int current, int total) {
    this.currentPage = current;
    this.totalPages = total;
    
    if (total == 0) {
        currentPageLabel.setText("No results");
        paginationControls.setVisible(false);
    } else {
        currentPageLabel.setText("Page " + current + " / " + total);
        paginationControls.setVisible(true);
    }
    
    prevPageButton.setDisable(current <= 1);
    nextPageButton.setDisable(current >= total);
}

private void showErrorMessage(String message) {
    productFlowPane.getChildren().clear();
    Label errorLabel = new Label("Error: " + message);
    errorLabel.getStyleClass().add("error-label");
    productFlowPane.getChildren().add(errorLabel);
    updatePaginationControls(0, 0);
}
```

## 2. HomeScreenController Improvements

```java
@FXML
public void initialize() {
    // Setup sort combo box
    sortByPriceComboBox.setItems(FXCollections.observableArrayList(
        "Default", "Price: Low to High", "Price: High to Low"
    ));
    sortByPriceComboBox.setValue("Default");
    
    // Load categories
    loadCategories();
    
    // Setup listeners
    setupListeners();
    
    // Load initial products
    loadProducts();
}

private void setupListeners() {
    // Real-time search as user types (with debounce)
    Timeline searchTimeline = new Timeline(new KeyFrame(Duration.millis(500), e -> {
        currentPage = 1;
        loadProducts();
    }));
    searchTimeline.setCycleCount(1);
    
    searchField.textProperty().addListener((obs, oldVal, newVal) -> {
        searchTimeline.stop();
        searchTimeline.play();
    });
    
    // Auto-filter when category/sort changes
    categoryComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
        if (newVal != null && !newVal.equals(oldVal)) {
            currentPage = 1;
            loadProducts();
        }
    });
    
    sortByPriceComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
        if (newVal != null && !newVal.equals(oldVal)) {
            currentPage = 1;
            loadProducts();
        }
    });
}
```

## 3. Error Handling & User Experience

```java
public class ProductSearchResultsController {
    
    private void handleSearchError(Exception e) {
        // Log error
        System.err.println("Search error: " + e.getMessage());
        
        // Show user-friendly message
        Platform.runLater(() -> {
            productFlowPane.getChildren().clear();
            VBox errorBox = new VBox(10);
            errorBox.setAlignment(Pos.CENTER);
            errorBox.getStyleClass().add("error-box");
            
            Label errorIcon = new Label("⚠");
            errorIcon.getStyleClass().add("error-icon");
            
            Label errorMessage = new Label("Unable to load products. Please try again later.");
            errorMessage.getStyleClass().add("error-message");
            
            Button retryButton = new Button("Retry");
            retryButton.setOnAction(e2 -> loadSearchedProducts());
            
            errorBox.getChildren().addAll(errorIcon, errorMessage, retryButton);
            productFlowPane.getChildren().add(errorBox);
        });
    }
    
    private void showLoadingState() {
        productFlowPane.getChildren().clear();
        
        ProgressIndicator loadingIndicator = new ProgressIndicator();
        loadingIndicator.setMaxSize(50, 50);
        
        Label loadingLabel = new Label("Loading products...");
        loadingLabel.getStyleClass().add("loading-label");
        
        VBox loadingBox = new VBox(10, loadingIndicator, loadingLabel);
        loadingBox.setAlignment(Pos.CENTER);
        
        productFlowPane.getChildren().add(loadingBox);
    }
}
```
