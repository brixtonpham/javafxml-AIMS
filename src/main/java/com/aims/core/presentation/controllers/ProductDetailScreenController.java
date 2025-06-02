package com.aims.presentation.controllers;

import com.aims.core.entities.Product;
import com.aims.core.entities.Book;
import com.aims.core.entities.CD;
import com.aims.core.entities.DVD;
import com.aims.core.application.services.IProductService;
import com.aims.core.application.services.ICartService;
// import com.aims.presentation.utils.AlertHelper;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;

public class ProductDetailScreenController {

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
    private GridPane productSpecificsGrid; // Để thêm các thông tin đặc thù
    @FXML
    private Spinner<Integer> quantitySpinner;
    @FXML
    private Button addToCartButton;
    @FXML
    private VBox detailsContainer; // Parent container for dynamic fields

    // @Inject
    private IProductService productService;
    // @Inject
    private ICartService cartService;
    // private MainLayoutController mainLayoutController;


    private Product currentProduct;
    private String productId;

    public ProductDetailScreenController() {
        //  productService = new ProductServiceImpl(...); // DI
        //  cartService = new CartServiceImpl(...);    // DI
    }

    // public void setMainLayoutController(MainLayoutController mainLayoutController) {
    //     this.mainLayoutController = mainLayoutController;
    // }
    // public void setProductService(IProductService productService) { this.productService = productService; }
    // public void setCartService(ICartService cartService) { this.cartService = cartService; }


    public void setProductId(String productId) {
        this.productId = productId;
        loadProductDetails();
    }

    public void initialize() {
        productDescriptionArea.setWrapText(true);
        productDescriptionArea.setEditable(false);
        SpinnerValueFactory<Integer> valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100, 1);
        quantitySpinner.setValueFactory(valueFactory);
    }

    private void loadProductDetails() {
        if (productId == null || productService == null) {
             System.err.println("Product ID or ProductService is null in ProductDetailScreenController.");
             // Hiển thị lỗi
            productTitleLabel.setText("Error loading product.");
            return;
        }
        try {
            // getProductDetailsForCustomer sẽ trả về giá đã có VAT
            currentProduct = productService.getProductDetailsForCustomer(productId);
            if (currentProduct != null) {
                productTitleLabel.setText(currentProduct.getTitle());
                productPriceLabel.setText(String.format("Price: %,.0f VND", currentProduct.getPrice())); // Price now includes VAT
                productCategoryLabel.setText("Category: " + currentProduct.getCategory());
                productAvailabilityLabel.setText("Available: " + currentProduct.getQuantityInStock());
                productDescriptionArea.setText(currentProduct.getDescription() != null ? currentProduct.getDescription() : "No description available.");

                if (currentProduct.getQuantityInStock() > 0) {
                    quantitySpinner.setDisable(false);
                    addToCartButton.setDisable(false);
                    ((SpinnerValueFactory.IntegerSpinnerValueFactory) quantitySpinner.getValueFactory())
                            .setMax(currentProduct.getQuantityInStock());
                } else {
                    quantitySpinner.setDisable(true);
                    addToCartButton.setDisable(true);
                    productAvailabilityLabel.setText("Out of Stock");
                }


                if (currentProduct.getImageUrl() != null && !currentProduct.getImageUrl().isEmpty()) {
                    try {
                        Image image = new Image(currentProduct.getImageUrl(), true);
                        productImageView.setImage(image);
                    } catch (Exception e) {
                         System.err.println("Error loading image for product " + productId + ": " + e.getMessage());
                        // productImageView.setImage(placeholderImage);
                    }
                } else {
                    // productImageView.setImage(placeholderImage);
                }
                displaySpecificDetails();
            } else {
                productTitleLabel.setText("Product Not Found");
                 // Disable controls
                addToCartButton.setDisable(true);
                quantitySpinner.setDisable(true);
            }
        } catch (SQLException | ResourceNotFoundException e) {
            e.printStackTrace();
            // AlertHelper.showErrorAlert("Error Loading Product", e.getMessage());
            productTitleLabel.setText("Error loading product details.");
        }
    }

    private void displaySpecificDetails() {
        productSpecificsGrid.getChildren().clear(); // Clear previous specific details
        productSpecificsGrid.setVgap(5);
        productSpecificsGrid.setHgap(10);
        int rowIndex = 0;

        // Common details
        addDetailToGrid("Barcode:", currentProduct.getBarcode(), rowIndex++);
        addDetailToGrid("Dimensions:", currentProduct.getDimensionsCm(), rowIndex++);
        addDetailToGrid("Weight:", String.format("%.2f kg", currentProduct.getWeightKg()), rowIndex++);
        addDetailToGrid("Entry Date:", currentProduct.getEntryDate() != null ? currentProduct.getEntryDate().format(DateTimeFormatter.ISO_DATE) : "N/A", rowIndex++);


        if (currentProduct instanceof Book book) {
            addDetailToGrid("Authors:", book.getAuthors(), rowIndex++);
            addDetailToGrid("Cover Type:", book.getCoverType(), rowIndex++);
            addDetailToGrid("Publisher:", book.getPublisher(), rowIndex++);
            addDetailToGrid("Publication Date:", book.getPublicationDate() != null ? book.getPublicationDate().format(DateTimeFormatter.ISO_DATE) : "N/A", rowIndex++);
            addDetailToGrid("Pages:", book.getNumPages() > 0 ? String.valueOf(book.getNumPages()) : "N/A", rowIndex++);
            addDetailToGrid("Language:", book.getLanguage(), rowIndex++);
            addDetailToGrid("Genre:", book.getBookGenre(), rowIndex++);
        } else if (currentProduct instanceof CD cd) {
            addDetailToGrid("Artists:", cd.getArtists(), rowIndex++);
            addDetailToGrid("Record Label:", cd.getRecordLabel(), rowIndex++);
            addDetailToGrid("Genre:", cd.getCdGenre(), rowIndex++);
            addDetailToGrid("Release Date:", cd.getReleaseDate() != null ? cd.getReleaseDate().format(DateTimeFormatter.ISO_DATE) : "N/A", rowIndex++);
            // Tracklist might be too long for grid, consider a separate view or popup
            Label tracklistLabel = new Label("Tracklist:");
            TextArea tracklistArea = new TextArea(cd.getTracklist() != null ? cd.getTracklist() : "N/A");
            tracklistArea.setEditable(false);
            tracklistArea.setWrapText(true);
            tracklistArea.setPrefHeight(100);
            productSpecificsGrid.add(tracklistLabel, 0, rowIndex);
            productSpecificsGrid.add(tracklistArea, 1, rowIndex++);

        } else if (currentProduct instanceof DVD dvd) {
            addDetailToGrid("Disc Type:", dvd.getDiscType(), rowIndex++);
            addDetailToGrid("Director:", dvd.getDirector(), rowIndex++);
            addDetailToGrid("Runtime:", dvd.getRuntimeMinutes() > 0 ? dvd.getRuntimeMinutes() + " mins" : "N/A", rowIndex++);
            addDetailToGrid("Studio:", dvd.getStudio(), rowIndex++);
            addDetailToGrid("Language:", dvd.getDvdLanguage(), rowIndex++);
            addDetailToGrid("Subtitles:", dvd.getSubtitles(), rowIndex++);
            addDetailToGrid("Release Date:", dvd.getDvdReleaseDate() != null ? dvd.getDvdReleaseDate().format(DateTimeFormatter.ISO_DATE) : "N/A", rowIndex++);
            addDetailToGrid("Genre:", dvd.getDvdGenre(), rowIndex++);
        }
    }

    private void addDetailToGrid(String labelText, String valueText, int rowIndex) {
        Label label = new Label(labelText);
        label.setStyle("-fx-font-weight: bold;");
        Label value = new Label(valueText != null ? valueText : "N/A");
        value.setWrapText(true);
        productSpecificsGrid.add(label, 0, rowIndex);
        productSpecificsGrid.add(value, 1, rowIndex);
    }


    @FXML
    void handleAddToCartAction(ActionEvent event) {
        if (currentProduct == null || cartService == null) {
            // AlertHelper.showErrorAlert("Error", "Cannot add to cart. Product or service not available.");
            return;
        }
        int quantity = quantitySpinner.getValue();
        if (quantity <= 0) {
            // AlertHelper.showWarningAlert("Invalid Quantity", "Please select a quantity greater than 0.");
            return;
        }

        try {
            String currentCartSessionId = "guest_cart_session"; // TODO: Lấy session ID thực tế
            cartService.addItemToCart(currentCartSessionId, currentProduct.getProductId(), quantity);
            // AlertHelper.showInfoAlert("Success", quantity + " of " + currentProduct.getTitle() + " added to your cart!");
            // Cập nhật lại thông tin tồn kho trên UI
            loadProductDetails(); // Tải lại để cập nhật số lượng còn lại
        } catch (SQLException | ResourceNotFoundException | ValidationException | InventoryException e) {
            e.printStackTrace();
            // AlertHelper.showErrorAlert("Add to Cart Failed", e.getMessage());
        }
    }

    @FXML
    void handleBackToListingAction(ActionEvent event) { // Nút này có thể không có trong FXML đã cung cấp
        // if (mainLayoutController != null && sceneManager != null) {
        //     mainLayoutController.loadContent(FXMLSceneManager.HOME_SCREEN); // Hoặc màn hình trước đó
        //     mainLayoutController.setHeaderTitle("AIMS Home");
        // }
        System.out.println("Back to listing action called.");
    }
}