package com.aims.core.presentation.controllers;

import com.aims.core.application.services.IProductService;
import com.aims.core.application.services.ICartService;
import com.aims.core.entities.Product;
import com.aims.core.entities.Book;
import com.aims.core.entities.CD;
import com.aims.core.entities.DVD;
// import com.aims.presentation.utils.AlertHelper;
// import com.aims.presentation.utils.FXMLSceneManager;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox; // If needed for dynamic content
import javafx.scene.layout.VBox;  // If needed for dynamic content
import javafx.scene.text.Text;   // For potentially long wrapped text

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
    // private MainLayoutController mainLayoutController; // For navigation and setting header
    // private FXMLSceneManager sceneManager;

    private Product currentProduct;
    private String productIdToLoad;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMMM yyyy");

    public ProductDetailScreenController() {
        // productService = new ProductServiceImpl(...); // DI
        // cartService = new CartServiceImpl(...);    // DI
    }

    // public void setMainLayoutController(MainLayoutController mainLayoutController) { this.mainLayoutController = mainLayoutController; }
    // public void setSceneManager(FXMLSceneManager sceneManager) { this.sceneManager = sceneManager; }
    // public void setProductService(IProductService productService) { this.productService = productService; }
    // public void setCartService(ICartService cartService) { this.cartService = cartService; }

    public void initialize() {
        productDescriptionArea.setWrapText(true);
        productDescriptionArea.setEditable(false);
        SpinnerValueFactory<Integer> valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100, 1); // Min 1, Max 100, Initial 1
        quantitySpinner.setValueFactory(valueFactory);
        setErrorMessage("", false);
    }

    /**
     * Sets the ID of the product to be displayed and loads its details.
     * This method should be called when navigating to this screen.
     * @param productId The ID of the product to display.
     */
    public void setProductId(String productId) {
        this.productIdToLoad = productId;
        if (productIdToLoad != null) {
            loadProductDetails();
        } else {
            displayError("Product ID not provided.");
        }
    }

    private void loadProductDetails() {
        // if (productService == null) {
        //     displayError("Product service is not available.");
        //     return;
        // }
        // try {
        //     // ProductService.getProductDetailsForCustomer should return product with VAT-inclusive price
        //     currentProduct = productService.getProductDetailsForCustomer(productIdToLoad);
        //     if (currentProduct != null) {
        //         populateProductData();
        //         if (mainLayoutController != null) {
        //             mainLayoutController.setHeaderTitle(currentProduct.getTitle());
        //         }
        //     } else {
        //         displayError("Product not found.");
        //     }
        // } catch (SQLException | ResourceNotFoundException e) {
        //     e.printStackTrace();
        //     displayError("Error loading product details: " + e.getMessage());
        // }
        System.out.println("loadProductDetails called for ID: " + productIdToLoad + " - Implement with actual service call.");
        // Dummy Data for UI testing
        if ("P001".equals(productIdToLoad)) { // Example
            Book book = new Book();
            book.setProductId("P001");
            book.setTitle("The Hitchhiker's Guide to the Galaxy (Example)");
            book.setPrice(150000f * 1.1f); // Assume service already added VAT
            book.setCategory("Science Fiction Comedy");
            book.setQuantityInStock(5);
            book.setDescription("A hilarious and absurd journey through space with Arthur Dent, the last surviving man from Earth, after it is demolished to make way for a hyperspace bypass.");
            book.setImageUrl("https://upload.wikimedia.org/wikipedia/en/b/bd/H2G2_UK_front_cover.jpg"); // Example URL
            book.setAuthors("Douglas Adams");
            book.setPublisher("Pan Books");
            book.setPublicationDate(java.time.LocalDate.of(1979, 10, 12));
            book.setNumPages(224);
            book.setCoverType("Paperback");
            book.setLanguage("English");
            book.setBookGenre("Sci-Fi, Comedy");
            this.currentProduct = book;
            populateProductData();
        } else {
             displayError("Product with ID " + productIdToLoad + " not found (dummy).");
        }
    }

    private void populateProductData() {
        productTitleLabel.setText(currentProduct.getTitle());
        productPriceLabel.setText(String.format("%,.0f VND", currentProduct.getPrice())); // Price already includes VAT from service
        productCategoryLabel.setText(currentProduct.getCategory() != null ? currentProduct.getCategory() : "N/A");
        productDescriptionArea.setText(currentProduct.getDescription() != null ? currentProduct.getDescription() : "No description available.");

        if (currentProduct.getQuantityInStock() > 0) {
            productAvailabilityLabel.setText("Available: " + currentProduct.getQuantityInStock());
            productAvailabilityLabel.setStyle("-fx-text-fill: green;");
            quantitySpinner.setDisable(false);
            addToCartButton.setDisable(false);
            ((SpinnerValueFactory.IntegerSpinnerValueFactory) quantitySpinner.getValueFactory())
                    .setMax(Math.min(100, currentProduct.getQuantityInStock())); // Limit spinner max to stock or 100
        } else {
            productAvailabilityLabel.setText("Out of Stock");
            productAvailabilityLabel.setStyle("-fx-text-fill: red;");
            quantitySpinner.setDisable(true);
            addToCartButton.setDisable(true);
        }

        if (currentProduct.getImageUrl() != null && !currentProduct.getImageUrl().isEmpty()) {
            try {
                Image image = new Image(currentProduct.getImageUrl(), true); // true for background loading
                productImageView.setImage(image);
            } catch (Exception e) {
                System.err.println("Error loading product image for detail view: " + currentProduct.getImageUrl() + " - " + e.getMessage());
                // Load placeholder image
            }
        } else {
            // Load placeholder image
        }
        populateSpecificDetails();
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
        Label label = new Label(labelText);
        label.setStyle("-fx-font-weight: bold;");
        productSpecificsGrid.add(label, 0, rowIndex);

        if (useTextNodeForValue) {
            Text valueNode = new Text(valueText != null ? valueText : "N/A");
            valueNode.setWrappingWidth(productSpecificsGrid.getColumnConstraints().get(1).getPrefWidth() > 0 ?
                                      productSpecificsGrid.getColumnConstraints().get(1).getPrefWidth() - 10 : 300); // Adjust wrapping
            productSpecificsGrid.add(valueNode, 1, rowIndex);
        } else {
            Label valueNode = new Label(valueText != null ? valueText : "N/A");
            valueNode.setWrapText(true);
            productSpecificsGrid.add(valueNode, 1, rowIndex);
        }
    }

    @FXML
    void handleAddToCartAction(ActionEvent event) {
        if (currentProduct == null) return;
        int quantity = quantitySpinner.getValue();
        if (quantity <= 0) {
            // AlertHelper.showWarningAlert("Invalid Quantity", "Please select a quantity greater than 0.");
            setErrorMessage("Quantity must be greater than 0.", true);
            return;
        }
        setErrorMessage("", false);

        System.out.println("Add to cart clicked for: " + currentProduct.getTitle() + ", Quantity: " + quantity);
        // if (cartService == null) {
        //     AlertHelper.showErrorAlert("Service Error", "Cart service is not available.");
        //     return;
        // }
        // try {
        //     String currentCartSessionId = "guest_cart_session_id_placeholder"; // TODO: Get actual session ID
        //     cartService.addItemToCart(currentCartSessionId, currentProduct.getProductId(), quantity);
        //     AlertHelper.showInfoAlert("Added to Cart", quantity + " of '" + currentProduct.getTitle() + "' added to your cart.");
        //     // Optionally update availability display, though a full reload via setProductId might be better
        //     loadProductDetails(); // Refresh details, especially stock
        // } catch (SQLException | ResourceNotFoundException | ValidationException | InventoryException e) {
        //     e.printStackTrace();
        //     AlertHelper.showErrorAlert("Add to Cart Failed", e.getMessage());
        // }
    }

    @FXML
    void handleBackToListingAction(ActionEvent event) {
        System.out.println("Back to Product Listing action triggered");
        // if (mainLayoutController != null && sceneManager != null) {
        //     mainLayoutController.loadContent(FXMLSceneManager.HOME_SCREEN); // Or previous screen
        //     mainLayoutController.setHeaderTitle("AIMS Home");
        // }
    }

    private void displayError(String message) {
        productTitleLabel.setText(message);
        productImageView.setImage(null); // Clear image
        productPriceLabel.setText("");
        productCategoryLabel.setText("");
        productAvailabilityLabel.setText("");
        productDescriptionArea.setText("");
        productSpecificsGrid.getChildren().clear();
        quantitySpinner.setDisable(true);
        addToCartButton.setDisable(true);
        // AlertHelper.showErrorAlert("Error", message);
        setErrorMessage(message, true);
    }

    private void setErrorMessage(String message, boolean visible) {
        errorMessageLabel.setText(message);
        errorMessageLabel.setVisible(visible);
        errorMessageLabel.setManaged(visible);
    }
}