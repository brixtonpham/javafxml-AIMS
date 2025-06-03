package com.aims.core.presentation.controllers;

import com.aims.core.application.dtos.ProductDTO; // Hoặc Entity Product
import com.aims.core.application.services.IProductService;
import com.aims.core.entities.Product;
import com.aims.core.enums.ProductType;
// import com.aims.presentation.utils.AlertHelper;
// import com.aims.presentation.utils.FXMLSceneManager;
import com.aims.core.shared.utils.SearchResult;


import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.util.Callback;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class AdminProductListController {

    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> categoryFilterComboBox;
    @FXML
    private ComboBox<ProductType> typeFilterComboBox; // Dùng Enum ProductType
    @FXML
    private TableView<Product> productsTableView; // Hiển thị Entity Product trực tiếp hoặc ProductDTO
    @FXML
    private TableColumn<Product, String> idColumn;
    @FXML
    private TableColumn<Product, ImageView> imageColumn;
    @FXML
    private TableColumn<Product, String> titleColumn;
    @FXML
    private TableColumn<Product, ProductType> typeColumn;
    @FXML
    private TableColumn<Product, String> categoryColumn;
    @FXML
    private TableColumn<Product, Float> priceColumn;
    @FXML
    private TableColumn<Product, Integer> stockColumn;
    @FXML
    private TableColumn<Product, Void> actionsColumn;

    @FXML
    private HBox paginationControls;
    @FXML
    private Button prevPageButton;
    @FXML
    private Label currentPageLabel;
    @FXML
    private Button nextPageButton;

    // @Inject
    private IProductService productService;
    // private MainLayoutController mainLayoutController;
    // private FXMLSceneManager sceneManager;

    private ObservableList<Product> productObservableList = FXCollections.observableArrayList();
    private int currentPage = 1;
    private final int PAGE_SIZE = 15; // Số lượng sản phẩm trên một trang quản lý
    private int totalPages = 1;

    // User ID của Admin/PM đang thực hiện, cần cho các action có kiểm tra giới hạn
    private String currentManagerId; // TODO: Set this from login session

    public AdminProductListController() {
        // productService = new ProductServiceImpl(...); // DI
    }

    // public void setMainLayoutController(MainLayoutController mainLayoutController) { this.mainLayoutController = mainLayoutController; }
    // public void setSceneManager(FXMLSceneManager sceneManager) { this.sceneManager = sceneManager; }
    // public void setProductService(IProductService productService) { this.productService = productService; }
    // public void setCurrentManagerId(String managerId) { this.currentManagerId = managerId; }


    public void initialize() {
        // sceneManager = FXMLSceneManager.getInstance();
        setupTableColumns();
        typeFilterComboBox.setItems(FXCollections.observableArrayList(ProductType.values()));
        // TODO: Load categories for filter from productService or a distinct list from products
        // categoryFilterComboBox.setItems(...);

        loadProducts();
    }

    private void setupTableColumns() {
        // idColumn, titleColumn, typeColumn, categoryColumn, priceColumn, stockColumn đã được cấu hình cellValueFactory trong FXML
        // Format price column
        priceColumn.setCellFactory(tc -> new TableCell<Product, Float>() {
            @Override
            protected void updateItem(Float price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) {
                    setText(null);
                } else {
                    setText(String.format("%,.0f VND", price));
                }
            }
        });

        // Custom cell for image
        imageColumn.setCellValueFactory(param -> {
            ImageView imageView = new ImageView();
            imageView.setFitHeight(40);
            imageView.setFitWidth(40);
            imageView.setPreserveRatio(true);
            Product product = param.getValue();
            if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
                try {
                    imageView.setImage(new Image(product.getImageUrl(), true));
                } catch (Exception e) { /* default/placeholder */ }
            }
            return new SimpleObjectProperty<>(imageView);
        });


        // Custom cell for action buttons (Edit, Delete)
        Callback<TableColumn<Product, Void>, TableCell<Product, Void>> cellFactory = new Callback<>() {
            @Override
            public TableCell<Product, Void> call(final TableColumn<Product, Void> param) {
                final TableCell<Product, Void> cell = new TableCell<>() {
                    private final Button editButton = new Button("Edit");
                    private final Button deleteButton = new Button("Delete");
                    private final HBox pane = new HBox(5, editButton, deleteButton);

                    {
                        editButton.getStyleClass().add("button-warning-small");
                        deleteButton.getStyleClass().add("button-danger-small");

                        editButton.setOnAction((ActionEvent event) -> {
                            Product product = getTableView().getItems().get(getIndex());
                            handleEditProductAction(product);
                        });
                        deleteButton.setOnAction((ActionEvent event) -> {
                            Product product = getTableView().getItems().get(getIndex());
                            handleDeleteProductAction(product);
                        });
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            setGraphic(pane);
                        }
                    }
                };
                return cell;
            }
        };
        actionsColumn.setCellFactory(cellFactory);
        productsTableView.setItems(productObservableList);
    }

    private void loadProducts() {
        // if (productService == null) {
        //     AlertHelper.showErrorAlert("Service Error", "Product service is not available.");
        //     return;
        // }
        // String searchTerm = searchField.getText();
        // String category = categoryFilterComboBox.getValue();
        // ProductType type = typeFilterComboBox.getValue();
        //
        // try {
        //     // TODO: ProductService cần một phương thức tìm kiếm/lọc nâng cao hơn cho Admin/PM
        //     // Ví dụ: searchProductsForManager(searchTerm, category, type, page, size, sortBy)
        //     // Tạm thời dùng searchProducts của khách hàng và lọc thêm nếu cần
        //     SearchResult<Product> result = productService.searchProducts(searchTerm, category, currentPage, PAGE_SIZE, null);
        //
        //     List<Product> filteredList = result.results();
        //     if (type != null) {
        //         filteredList = filteredList.stream()
        //                                  .filter(p -> p.getProductType() == type)
        //                                  .collect(Collectors.toList());
        //     }
        //
        //     productObservableList.setAll(filteredList);
        //     updatePaginationControls(result.currentPage(), result.totalPages(), result.totalResults());
        //
        // } catch (SQLException e) {
        //     e.printStackTrace();
        //     AlertHelper.showErrorAlert("Database Error", "Failed to load products: " + e.getMessage());
        // }
        System.out.println("loadProducts called - implement with actual service call and filtering.");
        // Dummy data for now
        // productObservableList.setAll(
        //     new Product("P001", "Sample Book 1", "Books", 100000f, 120000f, 10, "Desc1", "", "BC001", "20x15x2", 0.5f, ProductType.BOOK),
        //     new Product("P002", "Sample CD 1", "Music", 80000f, 95000f, 5, "Desc2", "", "BC002", "12x12x0.5", 0.1f, ProductType.CD)
        // );
        // updatePaginationControls(1,1, productObservableList.size());
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
        currentPage = 1; // Reset to first page on new search/filter
        loadProducts();
    }

    @FXML
    void handleClearFiltersAction(ActionEvent event) {
        searchField.clear();
        categoryFilterComboBox.setValue(null);
        typeFilterComboBox.setValue(null);
        currentPage = 1;
        loadProducts();
    }

    @FXML
    void handleAddNewProductAction(ActionEvent event) {
        System.out.println("Add New Product action triggered");
        // if (sceneManager != null && mainLayoutController != null) {
        //     AdminAddEditProductController addEditCtrl = (AdminAddEditProductController) sceneManager.loadFXMLIntoPane(
        //         mainLayoutController.getContentPane(), FXMLSceneManager.ADMIN_ADD_EDIT_PRODUCT_SCREEN
        //     );
        //     addEditCtrl.setProductToEdit(null); // null for adding new product
        //     addEditCtrl.setMainLayoutController(mainLayoutController);
        //     addEditCtrl.setCurrentManagerId(this.currentManagerId);
        //     mainLayoutController.setHeaderTitle("Add New Product");
        // }
    }

    private void handleEditProductAction(Product product) {
        System.out.println("Edit action for product: " + product.getTitle());
        // if (sceneManager != null && mainLayoutController != null) {
        //     AdminAddEditProductController addEditCtrl = (AdminAddEditProductController) sceneManager.loadFXMLIntoPane(
        //         mainLayoutController.getContentPane(), FXMLSceneManager.ADMIN_ADD_EDIT_PRODUCT_SCREEN
        //     );
        //     addEditCtrl.setProductToEdit(product); // Pass the product to edit
        //     addEditCtrl.setMainLayoutController(mainLayoutController);
        //     addEditCtrl.setCurrentManagerId(this.currentManagerId);
        //     mainLayoutController.setHeaderTitle("Edit Product - " + product.getTitle());
        // }
    }

    private void handleDeleteProductAction(Product product) {
        System.out.println("Delete action for product: " + product.getTitle());
        // if (productService == null || currentManagerId == null) {
        //     AlertHelper.showErrorAlert("Error", "Service or manager context not available.");
        //     return;
        // }
        // boolean confirmed = AlertHelper.showConfirmationDialog("Delete Product",
        //         "Are you sure you want to delete product: " + product.getTitle() + " (ID: " + product.getProductId() + ")?");
        // if (confirmed) {
        //     try {
        //         productService.deleteProduct(product.getProductId(), currentManagerId);
        //         AlertHelper.showInfoAlert("Success", "Product '" + product.getTitle() + "' deleted successfully.");
        //         loadProducts(); // Refresh the list
        //     } catch (SQLException | ValidationException | ResourceNotFoundException e) {
        //         AlertHelper.showErrorAlert("Deletion Failed", e.getMessage());
        //     }
        // }
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