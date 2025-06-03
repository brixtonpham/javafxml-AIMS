package com.aims.core.presentation.controllers;

import com.aims.core.application.dtos.ProductDTO; // Dùng DTO để thu thập dữ liệu
import com.aims.core.application.services.IProductService;
import com.aims.core.entities.Product; // Có thể cần để load sản phẩm đang edit
import com.aims.core.entities.Book;
import com.aims.core.entities.CD;
import com.aims.core.entities.DVD;
import com.aims.core.enums.ProductType;
// import com.aims.presentation.utils.AlertHelper;
// import com.aims.presentation.utils.FXMLSceneManager;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;

import java.util.UUID;

public class AdminAddEditProductController {

    @FXML private Label screenTitleLabel;
    @FXML private TextField productIdField;
    @FXML private TextField titleField;
    @FXML private TextField categoryField;
    @FXML private TextField valueAmountField;
    @FXML private TextField priceField;
    @FXML private TextField quantityInStockField;
    @FXML private TextArea descriptionArea;
    @FXML private TextField imageUrlField;
    @FXML private ImageView previewImageView;
    @FXML private TextField barcodeField;
    @FXML private TextField dimensionsField;
    @FXML private TextField weightField;
    @FXML private ComboBox<ProductType> productTypeComboBox;
    @FXML private Separator specificFieldsSeparator;
    @FXML private Label specificFieldsTitleLabel;
    @FXML private GridPane specificFieldsGridPane;
    @FXML private Label errorMessageLabel;
    @FXML private Button saveProductButton;

    // --- Book Specific Fields (dynamically created) ---
    private TextField authorsField;
    private TextField coverTypeField;
    private TextField publisherField;
    private DatePicker publicationDatePicker;
    private TextField numPagesField;
    private TextField languageFieldBook; // Renamed to avoid conflict
    private TextField bookGenreField;

    // --- CD Specific Fields (dynamically created) ---
    private TextField artistsField;
    private TextField recordLabelField;
    private TextArea tracklistArea;
    private TextField cdGenreField;
    private DatePicker releaseDateCDPicker;

    // --- DVD Specific Fields (dynamically created) ---
    private TextField discTypeField;
    private TextField directorField;
    private TextField runtimeMinutesField;
    private TextField studioField;
    private TextField dvdLanguageField;
    private TextField subtitlesField;
    private DatePicker releaseDateDVDPicker;
    private TextField dvdGenreField;


    // @Inject
    private IProductService productService;
    // private MainLayoutController mainLayoutController;
    // private FXMLSceneManager sceneManager;

    private Product productToEdit; // Nếu là edit mode
    private String currentManagerId; // TODO: Set this from login session


    public AdminAddEditProductController() {
        // productService = new ProductServiceImpl(...); // DI
    }

    // public void setMainLayoutController(MainLayoutController mainLayoutController) { this.mainLayoutController = mainLayoutController; }
    // public void setSceneManager(FXMLSceneManager sceneManager) { this.sceneManager = sceneManager; }
    // public void setProductService(IProductService productService) { this.productService = productService; }
    // public void setCurrentManagerId(String managerId) { this.currentManagerId = managerId; }


    public void initialize() {
        productTypeComboBox.setItems(FXCollections.observableArrayList(ProductType.values()));
        productTypeComboBox.getItems().remove(ProductType.OTHER); // Giả sử OTHER không cho phép tạo trực tiếp từ form này
        setErrorMessage("", false);

        // Listener cho Image URL để preview
        imageUrlField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.isEmpty()) {
                try {
                    previewImageView.setImage(new Image(newVal, true)); // true for background loading
                } catch (Exception e) {
                    previewImageView.setImage(null); // Clear if URL is invalid
                }
            } else {
                previewImageView.setImage(null);
            }
        });
        clearSpecificFields();
    }

    /**
     * Được gọi từ controller trước (AdminProductListController)
     * @param product Sản phẩm để sửa, hoặc null nếu là thêm mới.
     */
    public void setProductToEdit(Product product) {
        this.productToEdit = product;
        if (productToEdit != null) {
            screenTitleLabel.setText("Edit Product - " + productToEdit.getTitle());
            saveProductButton.setText("Update Product");
            productIdField.setText(productToEdit.getProductId());
            productIdField.setDisable(true); // Không cho sửa ID
            productTypeComboBox.setDisable(true); // Không cho đổi type khi edit (thường là vậy)

            titleField.setText(productToEdit.getTitle());
            categoryField.setText(productToEdit.getCategory());
            valueAmountField.setText(String.valueOf(productToEdit.getValueAmount()));
            priceField.setText(String.valueOf(productToEdit.getPrice()));
            quantityInStockField.setText(String.valueOf(productToEdit.getQuantityInStock()));
            descriptionArea.setText(productToEdit.getDescription());
            imageUrlField.setText(productToEdit.getImageUrl());
            barcodeField.setText(productToEdit.getBarcode());
            dimensionsField.setText(productToEdit.getDimensionsCm());
            weightField.setText(String.valueOf(productToEdit.getWeightKg()));
            productTypeComboBox.setValue(productToEdit.getProductType());

            handleProductTypeChange(null); // Load specific fields for the existing product type
            populateSpecificFields(productToEdit);

        } else {
            screenTitleLabel.setText("Add New Product");
            saveProductButton.setText("Add Product");
            productIdField.setDisable(false);
            productIdField.setPromptText("Leave blank to auto-generate, or enter custom ID");
            productTypeComboBox.setDisable(false);
            productTypeComboBox.getSelectionModel().selectFirst(); // Chọn type mặc định
            handleProductTypeChange(null);
        }
    }

    private void populateSpecificFields(Product product) {
        if (product instanceof Book book) {
            authorsField.setText(book.getAuthors());
            coverTypeField.setText(book.getCoverType());
            publisherField.setText(book.getPublisher());
            publicationDatePicker.setValue(book.getPublicationDate());
            if (book.getNumPages() > 0) numPagesField.setText(String.valueOf(book.getNumPages()));
            languageFieldBook.setText(book.getLanguage());
            bookGenreField.setText(book.getBookGenre());
        } else if (product instanceof CD cd) {
            artistsField.setText(cd.getArtists());
            recordLabelField.setText(cd.getRecordLabel());
            tracklistArea.setText(cd.getTracklist());
            cdGenreField.setText(cd.getCdGenre());
            releaseDateCDPicker.setValue(cd.getReleaseDate());
        } else if (product instanceof DVD dvd) {
            discTypeField.setText(dvd.getDiscType());
            directorField.setText(dvd.getDirector());
            if (dvd.getRuntimeMinutes() > 0) runtimeMinutesField.setText(String.valueOf(dvd.getRuntimeMinutes()));
            studioField.setText(dvd.getStudio());
            dvdLanguageField.setText(dvd.getDvdLanguage());
            subtitlesField.setText(dvd.getSubtitles());
            releaseDateDVDPicker.setValue(dvd.getDvdReleaseDate());
            dvdGenreField.setText(dvd.getDvdGenre());
        }
    }


    @FXML
    void handleProductTypeChange(ActionEvent event) {
        clearSpecificFields();
        ProductType selectedType = productTypeComboBox.getValue();
        if (selectedType == null) return;

        specificFieldsSeparator.setVisible(true);
        specificFieldsTitleLabel.setText(selectedType.toString() + " Specific Details");
        specificFieldsTitleLabel.setVisible(true);
        specificFieldsGridPane.setVisible(true);
        specificFieldsGridPane.setManaged(true);
        int rowIndex = 0;

        switch (selectedType) {
            case BOOK:
                authorsField = addTextFieldToGrid("Authors*:", "e.g., Author1, Author2", rowIndex++);
                coverTypeField = addTextFieldToGrid("Cover Type*:", "Paperback or Hardcover", rowIndex++);
                publisherField = addTextFieldToGrid("Publisher:", "", rowIndex++);
                publicationDatePicker = addDatePickerToGrid("Publication Date:", rowIndex++);
                numPagesField = addTextFieldToGrid("Number of Pages:", "", rowIndex++);
                languageFieldBook = addTextFieldToGrid("Language:", "", rowIndex++);
                bookGenreField = addTextFieldToGrid("Genre (Book):", "", rowIndex++);
                break;
            case CD:
                artistsField = addTextFieldToGrid("Artists*:", "e.g., Artist1, BandName", rowIndex++);
                recordLabelField = addTextFieldToGrid("Record Label:", "", rowIndex++);
                tracklistArea = addTextAreaToGrid("Tracklist:", "Enter tracks, one per line", rowIndex++);
                cdGenreField = addTextFieldToGrid("Genre (CD):", "", rowIndex++);
                releaseDateCDPicker = addDatePickerToGrid("Release Date:", rowIndex++);
                break;
            case DVD:
                discTypeField = addTextFieldToGrid("Disc Type*:", "DVD, Blu-ray, HD-DVD", rowIndex++);
                directorField = addTextFieldToGrid("Director*:", "", rowIndex++);
                runtimeMinutesField = addTextFieldToGrid("Runtime (minutes):", "", rowIndex++);
                studioField = addTextFieldToGrid("Studio:", "", rowIndex++);
                dvdLanguageField = addTextFieldToGrid("Language (DVD):", "", rowIndex++);
                subtitlesField = addTextFieldToGrid("Subtitles:", "e.g., English, Vietnamese", rowIndex++);
                releaseDateDVDPicker = addDatePickerToGrid("Release Date (DVD):", rowIndex++);
                dvdGenreField = addTextFieldToGrid("Genre (DVD):", "", rowIndex++);
                break;
            default:
                specificFieldsSeparator.setVisible(false);
                specificFieldsTitleLabel.setVisible(false);
                specificFieldsGridPane.setVisible(false);
                specificFieldsGridPane.setManaged(false);
                break;
        }
        // If editing, repopulate based on the new structure
        if(productToEdit != null && productToEdit.getProductType() == selectedType){
             populateSpecificFields(productToEdit);
        }
    }

    private void clearSpecificFields() {
        specificFieldsGridPane.getChildren().clear();
        // Nullify field references to avoid issues if type changes again
        authorsField = null; coverTypeField = null; publisherField = null; publicationDatePicker = null;
        numPagesField = null; languageFieldBook = null; bookGenreField = null;
        artistsField = null; recordLabelField = null; tracklistArea = null; cdGenreField = null; releaseDateCDPicker = null;
        discTypeField = null; directorField = null; runtimeMinutesField = null; studioField = null;
        dvdLanguageField = null; subtitlesField = null; releaseDateDVDPicker = null; dvdGenreField = null;
    }

    private TextField addTextFieldToGrid(String labelText, String promptText, int rowIndex) {
        Label label = new Label(labelText);
        TextField textField = new TextField();
        textField.setPromptText(promptText);
        textField.setMaxWidth(Double.MAX_VALUE);
        specificFieldsGridPane.add(label, 0, rowIndex);
        specificFieldsGridPane.add(textField, 1, rowIndex);
        return textField;
    }
    private TextArea addTextAreaToGrid(String labelText, String promptText, int rowIndex) {
        Label label = new Label(labelText);
        TextArea textArea = new TextArea();
        textArea.setPromptText(promptText);
        textArea.setWrapText(true);
        textArea.setPrefHeight(80);
        specificFieldsGridPane.add(label, 0, rowIndex);
        specificFieldsGridPane.add(textArea, 1, rowIndex);
        return textArea;
    }
    private DatePicker addDatePickerToGrid(String labelText, int rowIndex) {
        Label label = new Label(labelText);
        DatePicker datePicker = new DatePicker();
        specificFieldsGridPane.add(label, 0, rowIndex);
        specificFieldsGridPane.add(datePicker, 1, rowIndex);
        return datePicker;
    }


    @FXML
    void handleSaveProductAction(ActionEvent event) {
        setErrorMessage("", false);
        // if (productService == null) {
        //     AlertHelper.showErrorAlert("Service Error", "Product service is not available.");
        //     return;
        // }

        // --- Validate Common Fields ---
        String title = titleField.getText();
        String category = categoryField.getText();
        ProductType type = productTypeComboBox.getValue();

        if (title == null || title.trim().isEmpty() || type == null) {
            setErrorMessage("Title and Product Type are required.", true);
            return;
        }
        // Basic validation for numeric fields
        float valAmount, priceAmount;
        int stock;
        float weightVal;
        try {
            valAmount = Float.parseFloat(valueAmountField.getText());
            priceAmount = Float.parseFloat(priceField.getText());
            stock = Integer.parseInt(quantityInStockField.getText());
            weightVal = (weightField.getText() == null || weightField.getText().trim().isEmpty()) ? 0f : Float.parseFloat(weightField.getText());
             if (valAmount <= 0 || priceAmount <= 0 || stock < 0 || weightVal < 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            setErrorMessage("Value, Price, Stock, and Weight must be valid positive numbers (Stock & Weight can be 0).", true);
            return;
        }
        // More specific validation for each type will be done in service, e.g. price constraints.

        ProductDTO dto = new ProductDTO(); // Using DTO to gather data
        String pId = productIdField.getText();
        if (productToEdit == null && (pId == null || pId.trim().isEmpty())) {
            dto.setProductId("PROD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        } else {
            dto.setProductId(pId); // Use provided or existing ID
        }

        dto.setTitle(title);
        dto.setCategory(category);
        dto.setValueAmount(valAmount);
        dto.setPrice(priceAmount);
        dto.setQuantityInStock(stock);
        dto.setDescription(descriptionArea.getText());
        dto.setImageUrl(imageUrlField.getText());
        dto.setBarcode(barcodeField.getText());
        dto.setDimensionsCm(dimensionsField.getText());
        dto.setWeightKg(weightVal);
        dto.setProductType(type);

        try {
            switch (type) {
                case BOOK:
                    if (authorsField == null || authorsField.getText().trim().isEmpty() || coverTypeField.getText().trim().isEmpty()) {
                         setErrorMessage("For Books: Authors and Cover Type are required.", true); return;
                    }
                    dto.setAuthors(authorsField.getText());
                    dto.setCoverType(coverTypeField.getText());
                    dto.setPublisher(publisherField.getText());
                    dto.setPublicationDate(publicationDatePicker.getValue());
                    dto.setNumPages(numPagesField.getText().isEmpty() ? null : Integer.parseInt(numPagesField.getText()));
                    dto.setLanguage(languageFieldBook.getText());
                    dto.setBookGenre(bookGenreField.getText());
                    break;
                case CD:
                     if (artistsField == null || artistsField.getText().trim().isEmpty()) {
                         setErrorMessage("For CDs: Artists is required.", true); return;
                    }
                    dto.setArtists(artistsField.getText());
                    dto.setRecordLabel(recordLabelField.getText());
                    dto.setTracklist(tracklistArea.getText());
                    dto.setCdGenre(cdGenreField.getText());
                    dto.setReleaseDateCD(releaseDateCDPicker.getValue());
                    break;
                case DVD:
                     if (discTypeField == null || discTypeField.getText().trim().isEmpty() || directorField.getText().trim().isEmpty()) {
                         setErrorMessage("For DVDs: Disc Type and Director are required.", true); return;
                    }
                    dto.setDiscType(discTypeField.getText());
                    dto.setDirector(directorField.getText());
                    dto.setRuntimeMinutes(runtimeMinutesField.getText().isEmpty() ? null : Integer.parseInt(runtimeMinutesField.getText()));
                    dto.setStudio(studioField.getText());
                    dto.setDvdLanguage(dvdLanguageField.getText());
                    dto.setSubtitles(subtitlesField.getText());
                    dto.setReleaseDateDVD(releaseDateDVDPicker.getValue());
                    dto.setDvdGenre(dvdGenreField.getText());
                    break;
                case OTHER:
                    // No specific fields for OTHER type products
                    break;
            }

            if (productToEdit == null) { // ADDING NEW
                System.out.println("Attempting to add new product: " + dto.getTitle());
                // productService.addProduct(dto, currentManagerId); // ProductService would take DTO and create appropriate entity
                // AlertHelper.showInfoAlert("Success", "Product '" + dto.getTitle() + "' added successfully!");
            } else { // UPDATING EXISTING
                System.out.println("Attempting to update product: " + dto.getTitle());
                dto.setProductId(productToEdit.getProductId()); // Ensure ID is set for update
                // productService.updateProduct(dto, currentManagerId);
                // AlertHelper.showInfoAlert("Success", "Product '" + dto.getTitle() + "' updated successfully!");
            }
            // handleBackToListAction(null); // Navigate back to list

             // --- SIMULATED SAVE ---
            System.out.println("Simulated Save/Update for: " + dto.getTitle());
            setErrorMessage("Product '" + dto.getTitle() + "' " + (productToEdit == null ? "added" : "updated") + " (Simulated).", false);
            errorMessageLabel.setStyle("-fx-text-fill: green;");
            errorMessageLabel.setVisible(true);


        } catch (NumberFormatException e) {
            setErrorMessage("Invalid number format in specific fields (e.g., Pages, Runtime).", true);
        }
        // catch (SQLException | ValidationException | ResourceNotFoundException e) {
        //     setErrorMessage("Error saving product: " + e.getMessage(), true);
        //     e.printStackTrace();
        // }
        catch (Exception e) { // Catch-all for other unexpected issues during DTO population
            setErrorMessage("An unexpected error occurred: " + e.getMessage(), true);
            e.printStackTrace();
        }
    }


    @FXML
    void handleBackToListAction(ActionEvent event) {
        System.out.println("Back to Product List action triggered");
        // if (sceneManager != null && mainLayoutController != null) {
        //     mainLayoutController.loadContent(FXMLSceneManager.ADMIN_PRODUCT_LIST_SCREEN);
        //     mainLayoutController.setHeaderTitle("Product Management");
        // }
    }

    private void setErrorMessage(String message, boolean isError) {
        errorMessageLabel.setText(message);
        errorMessageLabel.setVisible(message != null && !message.isEmpty());
        errorMessageLabel.setManaged(message != null && !message.isEmpty());
        if (isError) {
            errorMessageLabel.setStyle("-fx-text-fill: red;");
        } else {
            errorMessageLabel.setStyle("-fx-text-fill: green;"); // For success messages
        }
    }
}